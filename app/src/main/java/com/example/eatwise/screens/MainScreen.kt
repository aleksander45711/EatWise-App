package com.example.eatwise.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/*
  Implementacja ekranu głównego (Main).
  Główne funkcjonalności:
  - Obliczanie docelowych makroskładników (kalorie, węglowodany, białka, tłuszcze)
  na podstawie danych użytkownika.
  - Pobieranie i aktualizacja danych użytkownika oraz posiłków z bazy Firestore.
  - Przeliczanie sum spożytych makroskładników i synchronizacja wyników z Firestore.
  - Wyświetlanie podsumowania makroskładników w postaci wykresów oraz listy posiłków.
  - Zarządzanie posiłkami: dodawanie nowego posiłku, usuwanie istniejącego posiłku z potwierdzeniem.
  - Nawigacja między ekranami (Home, Calendar, Weight Chart, Profile) za pomocą dolnego
  paska nawigacyjnego.
  Całość interfejsu użytkownika została zaimplementowana przy użyciu Jetpack Compose.
 */

@Composable
fun MainScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Docelowe makroskładniki
    var kcal by remember { mutableIntStateOf(0) }
    var carbs by remember { mutableIntStateOf(0) }
    var protein by remember { mutableIntStateOf(0) }
    var fat by remember { mutableIntStateOf(0) }

    // Makroskładniki posiłków
    var mealMacros = remember {
        mutableStateMapOf(
            "breakfast" to MealMacroState(),
            "snack" to MealMacroState(),
            "lunch" to MealMacroState(),
            "dinner" to MealMacroState()
        )
    }

    // Suma spożytych makroskładników
    var consumedKcal by remember { mutableIntStateOf(0) }
    var consumedCarbs by remember { mutableIntStateOf(0) }
    var consumedProtein by remember { mutableIntStateOf(0) }
    var consumedFat by remember { mutableIntStateOf(0) }

    // Stan dialogu potwierdzenia usunięcia posiłku
    var showDeleteMealDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableStateOf(0) } // Wyzwalacz odświeżenia

    // Funkcja do przeliczania sum spożytych makroskładników
    fun recalculateConsumedMacros(dailyMacrosDocRef: DocumentReference, mealMacros: Map<String, MealMacroState>) {
        Log.d("MainScreen", "recalculateConsumedMacros została wywołana")

        var totalConsumedKcal = 0
        var totalConsumedCarbs = 0
        var totalConsumedProtein = 0
        var totalConsumedFat = 0

        for (mealState in mealMacros.values) {
            totalConsumedKcal += mealState.kcal
            totalConsumedCarbs += mealState.carbs
            totalConsumedProtein += mealState.protein
            totalConsumedFat += mealState.fat
        }

        consumedKcal = totalConsumedKcal
        consumedCarbs = totalConsumedCarbs
        consumedProtein = totalConsumedProtein
        consumedFat = totalConsumedFat

        Log.d(
            "MainScreen",
            "Obliczone sumy: Kcal=$consumedKcal, Carbs=$consumedCarbs, Protein=$consumedProtein, Fat=$consumedFat"
        )

        val consumedMacrosSumData = hashMapOf(
            "consumedKcal" to consumedKcal,
            "consumedCarbs" to consumedCarbs,
            "consumedProtein" to consumedProtein,
            "consumedFat" to consumedFat
        )
        dailyMacrosDocRef.set(consumedMacrosSumData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(
                    "MainScreen",
                    "Pomyślnie zapisano sumy do Firestore (recalculateConsumedMacros): " +
                            "Kcal=$consumedKcal, Carbs=$consumedCarbs, Protein=$consumedProtein, Fat=$consumedFat"
                )
            }
            .addOnFailureListener { e ->
                Log.e("MainScreen", "Błąd zapisu sum do Firestore (recalculateConsumedMacros): ${e.message}")
            }
    }

    // Funkcja odświeżająca dane z Firestore
    val refreshData = {
        val user = auth.currentUser
        if (user != null) {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dailyMacrosDocRef = db.collection("users").document(user.uid)
                .collection("dailyMacros").document(currentDate)

            coroutineScope.launch {
                try {
                    // 1. Pobierz docelowe makroskładniki użytkownika
                    val userDoc = db.collection("users").document(user.uid).get().await()
                    if (userDoc != null && userDoc.exists()) {
                        val cw = userDoc.getDouble("currentWeight")?.toFloat() ?: 0f
                        val h = userDoc.getDouble("height")?.toFloat() ?: 0f
                        val a = userDoc.getDouble("age")?.toFloat() ?: 0f
                        val g = userDoc.getDouble("genderValue")?.toFloat() ?: 0f
                        val act = userDoc.getDouble("activityValue")?.toFloat() ?: 1f
                        // Pobierz typ diety – jeśli nie ustawiono, przyjmujemy "Balanced Diet"
                        val userDietType = userDoc.getString("dietType") ?: "Balanced Diet"
                        // Ustal procentowe wartości makroskładników w zależności od typu diety
                        val (carbsPercentage, proteinPercentage, fatPercentage) = when (userDietType) {
                            "High-protein diet" -> Triple(0.4, 0.35, 0.25)
                            "Ketogenic diet" -> Triple(0.1, 0.15, 0.75)
                            "Low-fat diet" -> Triple(0.65, 0.2, 0.15)
                            else -> Triple(0.5, 0.2, 0.3)
                        }
                        val diet = userDoc.getDouble("dietValue")?.toFloat() ?: 0f

                        val computedKcal = (((10 * cw) + (6.25 * h) - (5 * a) + g) * act) + diet
                        kcal = computedKcal.roundToInt()
                        carbs = ((computedKcal * carbsPercentage) / 4).roundToInt()
                        protein = ((computedKcal * proteinPercentage) / 4).roundToInt()
                        fat = ((computedKcal * fatPercentage) / 9).roundToInt()

                        val dailyMacroData = hashMapOf(
                            "kcal" to kcal,
                            "carbs" to carbs,
                            "protein" to protein,
                            "fat" to fat
                        )
                        dailyMacrosDocRef.set(dailyMacroData, SetOptions.merge()).await()
                        Log.d("MainScreen", "Goal macros saved to Firestore")
                    }

                    // 2. Pobierz podsumowania makroskładników dla poszczególnych posiłków
                    val mealTypes = listOf("breakfast", "snack", "lunch", "dinner")
                    for (mealType in mealTypes) {
                        val mealSummaryDocRef =
                            dailyMacrosDocRef.collection(mealType).document("${mealType}Summary")
                        val mealSummaryDoc = mealSummaryDocRef.get().await()
                        val mealKcal = mealSummaryDoc.getLong("kcal")?.toInt() ?: 0
                        val mealCarbs = mealSummaryDoc.getLong("carbs")?.toInt() ?: 0
                        val mealProtein = mealSummaryDoc.getLong("protein")?.toInt() ?: 0
                        val mealFat = mealSummaryDoc.getLong("fat")?.toInt() ?: 0
                        mealMacros[mealType] = MealMacroState(mealKcal, mealCarbs, mealProtein, mealFat)
                    }

                    // 3. Oblicz i zapisz sumy spożytych makroskładników
                    recalculateConsumedMacros(dailyMacrosDocRef, mealMacros)

                    // 4. Pobierz zaktualizowane consumedMacros z Firestore i uaktualnij stan
                    val dailyDoc = dailyMacrosDocRef.get().await()
                    if (dailyDoc.exists()) {
                        consumedKcal = dailyDoc.getLong("consumedKcal")?.toInt() ?: 0
                        consumedCarbs = dailyDoc.getLong("consumedCarbs")?.toInt() ?: 0
                        consumedProtein = dailyDoc.getLong("consumedProtein")?.toInt() ?: 0
                        consumedFat = dailyDoc.getLong("consumedFat")?.toInt() ?: 0
                    }
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error refreshing data: ${e.message}")
                }
            }
        }
    }

    // Odświeżamy dane, gdy ekran się pojawia
    LaunchedEffect(Unit) {
        refreshData()
    }

    // Odświeżamy dane przy zmianie refreshTrigger
    LaunchedEffect(key1 = refreshTrigger) {
        refreshData()
    }

    // Po powrocie z AddMealScreen może przyjść sygnał do odświeżenia
    LaunchedEffect(key1 = navController.currentBackStackEntry?.savedStateHandle) {
        val refreshMainScreen = navController.currentBackStackEntry
            ?.savedStateHandle
            ?.remove<Boolean>("refreshMainScreen")
        if (refreshMainScreen == true) {
            refreshTrigger++
        }
    }

    // Funkcja do usuwania posiłku
    fun deleteMeal(mealType: String) {
        auth.currentUser?.let {
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dailyMacrosDocRef = db.collection("users").document(it.uid)
                .collection("dailyMacros").document(currentDate)
            val mealSummaryDocRef =
                dailyMacrosDocRef.collection(mealType).document("${mealType}Summary")

            mealSummaryDocRef.delete()
                .addOnSuccessListener {
                    mealMacros[mealType] = MealMacroState() // Reset stanu lokalnego
                    recalculateConsumedMacros(dailyMacrosDocRef, mealMacros)
                    refreshData() // odśwież po usunięciu
                }
                .addOnFailureListener { e ->
                    Log.e("MainScreen", "Błąd usuwania $mealType: ${e.message}")
                }
        }
    }

    // Aktualna data do wyświetlania
    val displayDate = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background
            ) {
                // HOME
                NavigationBarItem(
                    selected = true,
                    onClick = {  },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = MaterialTheme.colorScheme.background
                    )
                )
                // CALENDAR
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Calendar.route) },
                    icon = { Icon(Icons.Filled.CalendarToday, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background
                    )
                )
                // WEIGHT CHART
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
                // PROFILE
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Górny wiersz z datą i logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayDate,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(end = 8.dp)
                )
            }

            Text(
                text = "Summary",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )

            // Okrągłe wykresy makroskładników
            MacroSummary(
                kcal = kcal,
                carbs = carbs,
                protein = protein,
                fat = fat,
                consumedKcal = consumedKcal,
                consumedCarbs = consumedCarbs,
                consumedProtein = consumedProtein,
                consumedFat = consumedFat
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Meals:",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )

            // Kafelek "Add Meal"
            MealCard(
                mealType = "Add Meal",
                kcal = 0,
                showAddButton = true
            ) {
                navController.navigate(Screen.AddMeal.route)
            }

            // Kafelki posiłków
            MealSummaryCard(
                mealType = "Breakfast",
                mealMacroState = mealMacros["breakfast"] ?: MealMacroState(),
                onDelete = {
                    mealToDelete = "breakfast"
                    showDeleteMealDialog = true
                }
            )
            MealSummaryCard(
                mealType = "Snack",
                mealMacroState = mealMacros["snack"] ?: MealMacroState(),
                onDelete = {
                    mealToDelete = "snack"
                    showDeleteMealDialog = true
                }
            )
            MealSummaryCard(
                mealType = "Lunch",
                mealMacroState = mealMacros["lunch"] ?: MealMacroState(),
                onDelete = {
                    mealToDelete = "lunch"
                    showDeleteMealDialog = true
                }
            )
            MealSummaryCard(
                mealType = "Dinner",
                mealMacroState = mealMacros["dinner"] ?: MealMacroState(),
                onDelete = {
                    mealToDelete = "dinner"
                    showDeleteMealDialog = true
                }
            )
        }
    }

    // Dialog potwierdzenia usunięcia
    if (showDeleteMealDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMealDialog = false },
            title = { Text("Delete Meal?") },
            text = {
                Text(
                    "Are you sure you want to delete " +
                            mealToDelete?.replaceFirstChar { it.titlecase(Locale.getDefault()) } +
                            "?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mealToDelete?.let { deleteMeal(it) }
                    showDeleteMealDialog = false
                    mealToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteMealDialog = false
                    mealToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Data class do przechowywania stanu makroskładników
data class MealMacroState(
    var kcal: Int = 0,
    var carbs: Int = 0,
    var protein: Int = 0,
    var fat: Int = 0
)

@Composable
fun MacroSummary(
    kcal: Int,
    carbs: Int,
    protein: Int,
    fat: Int,
    consumedKcal: Int,
    consumedCarbs: Int,
    consumedProtein: Int,
    consumedFat: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Kcal", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground)
        CircularProgressIndicator(
            modifier = Modifier.size(120.dp),
            progress = { if (kcal > 0) consumedKcal.toFloat() / kcal.toFloat() else 0f },
            color = if (consumedKcal <= kcal || kcal == 0) Color.Green else Color.Red,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Text(text = "$consumedKcal/$kcal", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroCircularIndicator(
                "Carbs", consumedCarbs, carbs, indicatorColor = {
                    if (consumedCarbs <= carbs || carbs == 0) Color.Green else Color.Red
                },
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            MacroCircularIndicator(
                "Protein", consumedProtein, protein, indicatorColor = {
                    if (consumedProtein <= protein || protein == 0) Color.Green else Color.Red
                },
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            MacroCircularIndicator(
                "Fats", consumedFat, fat, indicatorColor = {
                    if (consumedFat <= fat || fat == 0) Color.Green else Color.Red
                },
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun MacroCircularIndicator(title: String, consumed: Int, goal: Int, indicatorColor: () -> Color, trackColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            progress = { if (goal > 0) consumed.toFloat() / goal.toFloat() else 0f },
            color = indicatorColor(),
            trackColor = trackColor
        )
        Text(text = "$consumed/$goal g", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun MealCard(
    mealType: String,
    kcal: Int = 0,
    carbs: Int = 0,
    protein: Int = 0,
    fat: Int = 0,
    showAddButton: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = mealType,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (!showAddButton) {
                    Text(
                        text = "$kcal kcal, $carbs C, $protein P, $fat F",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
            if (showAddButton) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Meal",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                IconButton(onClick = { onClick() }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Meal",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun MealSummaryCard(
    mealType: String,
    mealMacroState: MealMacroState,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = mealType,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "${mealMacroState.kcal} kcal, " +
                            "${mealMacroState.carbs} C, " +
                            "${mealMacroState.protein} P, " +
                            "${mealMacroState.fat} F",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Meal",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityValueDropdown(
    currentSelection: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = currentSelection,
            onValueChange = {},
            label = { Text("Activity") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { activityText ->
                DropdownMenuItem(
                    text = {
                        Text(
                            activityText,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelectionChange(activityText)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietTypeDropdown(
    currentSelection: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = currentSelection,
            onValueChange = {},
            label = { Text("Diet Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { dietText ->
                DropdownMenuItem(
                    text = {
                        Text(
                            dietText,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onSelectionChange(dietText)
                        expanded = false
                    }
                )
            }
        }
    }
}
