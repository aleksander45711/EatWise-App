package com.example.eatwise.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eatwise.R
import com.example.eatwise.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/*
  Implementację ekranu profilu użytkownika (Profile).
  Główne funkcjonalnosci:
  - Wyświetlanie postępów różnicy w wadze od pierwszego dnia rejestracji oraz wskźnik BMI.
  - Funkcja zmiany wagi docelowej.
  - Aktualizacja poziomu aktywności oraz rodzaju diety.
  - Możliwosc wylogowania użytkownika za pomoca przycisku "Log out".
  - Wszystkie dane są pobierane i aktualizowane w Firebase Firestore.
 */

// Główny ekran profilu użytkownika
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    // Uzyskanie instancji FirebaseAuth i Firestore
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val db = FirebaseFirestore.getInstance()
    // Scope do operacji asynchronicznych
    val coroutineScope = rememberCoroutineScope()

    // Zmienne przechowujące dane użytkownika: aktualna waga, wzrost, waga początkowa, waga docelowa
    var currentWeight by remember { mutableStateOf(0f) }
    var height by remember { mutableStateOf(0f) }
    var initialWeight by remember { mutableStateOf(0f) }
    var goalWeight by remember { mutableStateOf(0f) }
    // Zmienna przechowująca tekst wpisany przez użytkownika do zmiany wagi docelowej
    var newGoalWeightInput by remember { mutableStateOf("") }
    // Zmienna przechowująca tekst informujący o postępach (np. utrata lub przyrost wagi)
    var progressText by remember { mutableStateOf("") }
    // Historia dziennych wag (para: data i waga)
    var weightHistory by remember { mutableStateOf<List<Pair<Date, Float>>>(emptyList()) }
    // Aktualnie wybrany poziom aktywności jako tekst
    var selectedActivityText by remember { mutableStateOf("Low") }
    // Dostępne opcje aktywności
    val activityOptions = listOf("Low", "Medium", "High")
    // Zmienna przechowująca wybrany typ diety
    var selectedDietType by remember { mutableStateOf("Balanced Diet") }
    // Opcje do wyboru dla typu diety wraz z opisami procentowymi makroskładników
    val dietOptions = listOf(
        "Balanced Diet: Carbs 50%, Protein 20%, Fat 30%",
        "High-protein diet: Carbs 40%, Protein 35%, Fat 25%",
        "Ketogenic diet: Carbs 10%, Protein 15%, Fat 75%",
        "Low-fat diet: Carbs 65%, Protein 20%, Fat 15%"
    )

    // Funkcja obliczająca postęp wagi na podstawie wagi początkowej i bieżącej
    fun calculateProgress(dailyWeight: Float, current: Float) {
        if (dailyWeight == 0f) {
            progressText = "Weight data is not available yet."
            return
        }
        when {
            dailyWeight == current -> progressText = "Your weight hasn't changed."
            dailyWeight > current -> {
                val diff = String.format(Locale.getDefault(), "%.1f", dailyWeight - current)
                progressText = "you lost $diff kg!"
            }
            else -> {
                val diff = String.format(Locale.getDefault(), "%.1f", current - dailyWeight)
                progressText = "You gain $diff kg!"
            }
        }
    }

    // Funkcja aktualizująca typ diety w Firestore
    fun updateDietType(newDiet: String) {
        if (user == null) return
        coroutineScope.launch {
            try {
                db.collection("users").document(user.uid)
                    .update("dietType", newDiet)
                    .await()
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error updating dietType: ${e.message}")
            }
        }
    }

    // Funkcja aktualizująca wagę docelową w Firestore na podstawie danych wpisanych przez użytkownika
    fun updateGoalWeight() {
        val newGoalWeight = newGoalWeightInput.toFloatOrNull()
        if (newGoalWeight != null && user != null) {
            coroutineScope.launch {
                try {
                    db.collection("users").document(user.uid)
                        .update("goalWeight", newGoalWeight)
                        .await()
                    goalWeight = newGoalWeight
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Error updating goal weight: ${e.message}")
                }
            }
        }
    }

    // Funkcja aktualizująca wartość aktywności w Firestore na podstawie wybranej opcji
    fun updateActivityValue(newActivityText: String) {
        if (user == null) return
        val newValue = when (newActivityText) {
            "Low" -> 1.45f
            "Medium" -> 1.65f
            "High" -> 1.85f
            else -> 1.45f
        }
        coroutineScope.launch {
            try {
                db.collection("users").document(user.uid)
                    .update("activityValue", newValue)
                    .await()
                selectedActivityText = newActivityText
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error updating activityValue: ${e.message}")
            }
        }
    }

    // Efekt uruchamiany przy pierwszym załadowaniu ekranu lub zmianie UID użytkownika
    LaunchedEffect(user?.uid) {
        if (user != null) {
            try {
                // Pobranie dokumentu użytkownika z Firestore
                val userDoc = db.collection("users").document(user.uid).get().await()
                currentWeight = userDoc.getDouble("currentWeight")?.toFloat() ?: 0f
                goalWeight = userDoc.getDouble("goalWeight")?.toFloat() ?: 0f
                // Pobranie wzrostu (height) w centymetrach
                height = userDoc.getDouble("height")?.toFloat() ?: 0f

                // Ustalenie poziomu aktywności na podstawie wartości zapisanego w Firestore
                val savedActivityValue = userDoc.getDouble("activityValue") ?: 1.45
                selectedActivityText = when (savedActivityValue.toFloat()) {
                    1.45f -> "Low"
                    1.65f -> "Medium"
                    1.85f -> "High"
                    else -> "Low"
                }
                // Uaktualnienie typu diety na podstawie danych z Firestore
                val savedDietType = userDoc.getString("dietType") ?: "Balanced Diet"
                selectedDietType = savedDietType

                // Ustawienie pola tekstowego dla wagi docelowej na podstawie danych z Firestore
                newGoalWeightInput = goalWeight.toInt().toString()

                // Pobranie historii dziennych wag z kolekcji "dailyWeight"
                val dailyWeightQuery = db.collection("users")
                    .document(user.uid)
                    .collection("dailyWeight")
                    .get()
                    .await()

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                // Przekształcenie dokumentów na pary (data, waga) i posortowanie ich chronologicznie
                val allDailyWeights = dailyWeightQuery.documents.mapNotNull { doc ->
                    val dateStr = doc.id
                    val w = doc.getDouble("weight")?.toFloat() ?: 0f
                    try {
                        sdf.parse(dateStr)?.let { parsedDate ->
                            parsedDate to w
                        }
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.first }

                weightHistory = allDailyWeights
                initialWeight = allDailyWeights.firstOrNull()?.second ?: 0f
                // Obliczenie postępu wagi na podstawie wagi początkowej i bieżącej
                calculateProgress(initialWeight, currentWeight)

            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error fetching data: ${e.message}")
            }
        }
    }

    // Główna struktura interfejsu użytkownika
    Scaffold(
        // Pasek nawigacyjny u dołu ekranu
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                // Pozycja "Home" w nawigacji
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Main.route) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent
                    )
                )
                // Pozycja "Calendar" w nawigacji
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Calendar.route) },
                    icon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar") },
                    label = { Text("Calendar") }
                )
                // Pozycja "Weight Chart" w nawigacji
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.WeightChart.route) },
                    icon = { Icon(Icons.Filled.PieChart, contentDescription = "Weight Chart") },
                    label = { Text("Weight Chart") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background
                    )
                )
                // Pozycja "Profile" (aktualny ekran)
                NavigationBarItem(
                    selected = true,
                    onClick = { /* ... */ },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        },
        // Pasek tytułowy u góry ekranu
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Główna zawartość ekranu profilu, umieszczona w przewijalnej kolumnie
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Jeśli użytkownik nie jest zalogowany, wyświetl komunikat
            if (user == null) {
                Text(
                    text = "User not logged in",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                // Wyświetlenie nagłówka "Your Progress" oraz obliczonego tekstu postępu wagi
                Text(
                    text = "Your Progress",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = progressText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Sekcja obliczania i wyświetlania BMI
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "BMI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (height > 0f) {
                    // Obliczenie BMI: (waga w kg * 10000) / (wzrost w cm)^2
                    val bmi = currentWeight * 10000 / (height * height)
                    val bmiDescription: String
                    val bmiColor: Color
                    // Określenie opisu BMI oraz koloru w zależności od wartości
                    when {
                        bmi > 30 -> {
                            bmiDescription = "Obesity"
                            bmiColor = Color.Red
                        }
                        bmi > 25 -> {
                            bmiDescription = "Overweight"
                            bmiColor = Color.Yellow
                        }
                        bmi > 18.5 -> {
                            bmiDescription = "Normal weight"
                            bmiColor = Color.Green
                        }
                        else -> {
                            bmiDescription = "Underweight"
                            bmiColor = Color.Blue
                        }
                    }
                    // Wyświetlenie wartości BMI oraz opisu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format("%.1f", bmi)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bmiDescription,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = bmiColor
                        )
                    }
                } else {
                    // Jeśli dane dotyczące wzrostu nie są dostępne, wyświetl odpowiedni komunikat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Height data not available",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sekcja zmiany wagi docelowej
                Text(
                    text = "Change Goal Weight",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pole tekstowe do wpisania nowej wagi docelowej
                    OutlinedTextField(
                        value = newGoalWeightInput,
                        onValueChange = { newGoalWeightInput = it },
                        label = { Text("Goal Weight (kg)") },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Przycisk aktualizujący wagę docelową po kliknięciu
                    Button(
                        onClick = { updateGoalWeight() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Update")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sekcja zmiany wartości aktywności
                Text(
                    text = "Change Activity Value",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Rozwijane menu wyboru poziomu aktywności; wywołanie updateActivityValue przy zmianie
                ActivityValueDropdown(
                    currentSelection = selectedActivityText,
                    options = activityOptions,
                    onSelectionChange = { updateActivityValue(it) }
                )

                // Nowa sekcja zmiany rodzaju diety
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Change the type of diet",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Rozwijane menu wyboru typu diety; przy zmianie wywoływana jest funkcja updateDietType
                DietTypeDropdown(
                    currentSelection = selectedDietType,
                    options = dietOptions,
                    onSelectionChange = { newDiet ->
                        updateDietType(newDiet)
                        selectedDietType = newDiet
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Wyświetlenie logo aplikacji
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo aplikacji",
                    modifier = Modifier.size(150.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Przycisk umożliwiający wylogowanie się; po wylogowaniu następuje przekierowanie do LoginScreen
                Button(
                    onClick = {
                        auth.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text("Log Out")
                }
            }
        }
    }
}
