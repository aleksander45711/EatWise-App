package com.example.eatwise.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eatwise.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

/*
  Opis:
  Implementacja ekranu kalendarza (Calendar), z funkcją przeglądania danych dotyczące spożytych
  kalorii oraz innych makroskładników dla poszczególnych dni.
  Główne funkcjonalnosci:
  - Mechanizm zmiany miesiąca/roku oraz dynamiczne generowanie listy dat dla danego miesiąca.
  - Pobieranie danych z Firestore dla wybranych dni.
  - Wyświetlanie szczegółowych danych (kcal, węglowodany, białko oraz tłuszcze) w oknie dialogowym
  po kliknięciu na dzień.
 */

// Główny ekran kalendarza aplikacji
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController) {
    // Uzyskanie instancji FirebaseAuth i sprawdzenie czy użytkownik jest zalogowany
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // Jeśli użytkownik nie jest zalogowany, wyświetl ekran informujący o braku zalogowania
    if (user == null) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* ... */ },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Screen.Calendar.route) },
                        icon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar") },
                        label = { Text("Calendar") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Screen.WeightChart.route) },
                        icon = { Icon(Icons.Filled.PieChart, contentDescription = "Weight Chart") },
                        label = { Text("Weight Chart") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Screen.Profile.route) },
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            // Wyświetlenie komunikatu, że użytkownik nie jest zalogowany
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Użytkownik nie jest zalogowany", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        return
    }

    // Inicjalizacja Firestore
    val db = FirebaseFirestore.getInstance()
    // Ustalenie bieżącej daty (rok i miesiąc) w strefie czasowej UTC
    val baseCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val currentYear = baseCalendar.get(Calendar.YEAR)
    val currentMonth = baseCalendar.get(Calendar.MONTH)

    // Stany przechowujące wyświetlany rok i miesiąc w kalendarzu
    var displayedYear by remember { mutableStateOf(currentYear) }
    var displayedMonth by remember { mutableStateOf(currentMonth) }

    // Ustalenie kalendarza dla wyświetlanego miesiąca, wyzerowanie czasu
    val monthCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clearTime()
        set(Calendar.YEAR, displayedYear)
        set(Calendar.MONTH, displayedMonth)
    }
    // Pobranie liczby dni w wybranym miesiącu
    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Generowanie listy dat dla wszystkich dni wybranego miesiąca
    val dateList = (1..daysInMonth).map { day ->
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clearTime()
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, day)
        }.time
    }

    // Format daty używany do komunikacji z Firestore
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Główna struktura ekranu: pasek u góry, pasek nawigacyjny na dole, zawartość ekranu w środku
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Calendar",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Main.route) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* nic */ },
                    icon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.WeightChart.route) },
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = "Weight Chart") },
                    label = { Text("Weight Chart") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Zawartość ekranu kalendarza: kolumna z paskiem zmiany miesiąca/roku i siatką dni
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pasek do zmiany miesiąca i roku
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Przycisk zmieniający rok na poprzedni
                IconButton(onClick = { displayedYear-- }) {
                    Icon(Icons.Filled.KeyboardDoubleArrowLeft, contentDescription = "Previous Year", tint = MaterialTheme.colorScheme.primary)
                }
                // Przycisk zmieniający miesiąc na poprzedni
                IconButton(onClick = {
                    if (displayedMonth == 0) {
                        displayedMonth = 11
                        displayedYear--
                    } else {
                        displayedMonth--
                    }
                }) {
                    Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = MaterialTheme.colorScheme.primary)
                }

                // Wyświetlenie aktualnie wybranego miesiąca i roku
                Text(
                    text = "${getMonthName(displayedMonth)} $displayedYear",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Przycisk zmieniający miesiąc na następny
                IconButton(onClick = {
                    if (displayedMonth == 11) {
                        displayedMonth = 0
                        displayedYear++
                    } else {
                        displayedMonth++
                    }
                }) {
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.primary)
                }
                // Przycisk zmieniający rok na następny
                IconButton(onClick = { displayedYear++ }) {
                    Icon(Icons.Filled.KeyboardDoubleArrowRight, contentDescription = "Next Year", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Siatka wyświetlająca poszczególne dni miesiąca
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(0.9f),
                contentPadding = PaddingValues(4.dp)
            ) {
                // Dla każdej daty z listy, wyświetl kafelek kalendarza
                items(dateList) { date ->
                    CalendarTile(
                        date = date,
                        dateFormat = dateFormat,
                        db = db,
                        userId = user.uid
                    )
                }
            }
        }
    }
}

// Funkcja rozszerzająca Calendar - ustawia czas na 0 (początek dnia)
private fun Calendar.clearTime() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

// Funkcja zwracająca nazwę miesiąca na podstawie numeru miesiąca
fun getMonthName(month: Int): String {
    return DateFormatSymbols().months[month]
}

// Funkcja wyświetlająca pojedynczy kafelek kalendarza
@Composable
fun CalendarTile(
    date: Date,
    dateFormat: SimpleDateFormat,
    db: FirebaseFirestore,
    userId: String
) {
    // Sformatowana data jako string (np. "2025-02-09")
    val dateString = dateFormat.format(date)
    var consumedKcal by remember { mutableStateOf(0) }
    var goalKcal by remember { mutableStateOf(0) }

    // Pobranie danych z Firestore dotyczących spożytych kalorii i celu kalorycznego dla danego dnia
    LaunchedEffect(dateString, userId) {
        if (userId.isNotEmpty()) {
            try {
                val docSnapshot = withContext(Dispatchers.IO) {
                    db.collection("users").document(userId)
                        .collection("dailyMacros").document(dateString)
                        .get().await()
                }
                if (docSnapshot.exists()) {
                    consumedKcal = docSnapshot.getLong("consumedKcal")?.toInt() ?: 0
                    goalKcal = docSnapshot.getLong("kcal")?.toInt() ?: 0
                } else {
                    consumedKcal = 0
                    goalKcal = 0
                }
            } catch (e: Exception) {
                Log.e("CalendarTile", "Error retrieving data for $dateString: ${e.message}")
            }
        }
    }

    // Ustalenie koloru tła kafelka: zielony, jeśli spożyto kalorie; inaczej domyślny kolor
    val backgroundColor = if (consumedKcal > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    var showDetails by remember { mutableStateOf(false) }

    // Jeśli kafelek zostanie kliknięty, wyświetl szczegóły dnia w oknie dialogowym
    if (showDetails) {
        var consumedCarbs by remember { mutableStateOf(0) }
        var goalCarbs by remember { mutableStateOf(0) }
        var consumedProtein by remember { mutableStateOf(0) }
        var goalProtein by remember { mutableStateOf(0) }
        var consumedFat by remember { mutableStateOf(0) }
        var goalFat by remember { mutableStateOf(0) }

        // Pobranie dodatkowych szczegółowych danych (węglowodany, białko, tłuszcze) z Firestore
        LaunchedEffect(dateString, userId) {
            try {
                val docSnapshot = withContext(Dispatchers.IO) {
                    db.collection("users").document(userId)
                        .collection("dailyMacros").document(dateString)
                        .get().await()
                }
                if (docSnapshot.exists()) {
                    consumedCarbs = docSnapshot.getLong("consumedCarbs")?.toInt() ?: 0
                    goalCarbs = docSnapshot.getLong("carbs")?.toInt() ?: 0
                    consumedProtein = docSnapshot.getLong("consumedProtein")?.toInt() ?: 0
                    goalProtein = docSnapshot.getLong("protein")?.toInt() ?: 0
                    consumedFat = docSnapshot.getLong("consumedFat")?.toInt() ?: 0
                    goalFat = docSnapshot.getLong("fat")?.toInt() ?: 0
                }
            } catch (e: Exception) {
                Log.e("CalendarTile", "Details error for $dateString: ${e.message}")
            }
        }

        // Okno dialogowe wyświetlające szczegółowe dane dla wybranej daty
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Details for $dateString", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Kcal: $consumedKcal / $goalKcal", color = MaterialTheme.colorScheme.onSurface)
                    Text("Carbs: $consumedCarbs / $goalCarbs", color = MaterialTheme.colorScheme.onSurface)
                    Text("Protein: $consumedProtein / $goalProtein", color = MaterialTheme.colorScheme.onSurface)
                    Text("Fat: $consumedFat / $goalFat", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Zamknij", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // Ustalenie numeru dnia w miesiącu z obiektu Date
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

    // Wyświetlenie kafelka kalendarza: kwadrat z tłem i numerem dnia; kliknięcie otwiera dialog ze szczegółami
    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .background(backgroundColor)
            .clickable { showDetails = true },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayOfMonth.toString(),
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
