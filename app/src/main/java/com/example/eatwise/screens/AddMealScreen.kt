package com.example.eatwise.screens

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eatwise.R
import com.example.eatwise.models.MealItem
import com.example.eatwise.network.OpenFoodApi
import com.example.eatwise.network.Product
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth

/*
  Implementacja ekarnu dodawania poszczególnych posiłków (AddMeal)
  Główne funkcjonalnosci:
  - Funkcja AddMealSearchResultItem wyświetla pojedynczy wynik wyszukiwania produktu,
  umożliwiając użytkownikowi wprowadzenie wagi produktu oraz dodanie go do posiłku.
  - Funkcja AddMealScreen stanowi główny ekran dodawania posiłku. Umożliwia wyszukiwanie produktów
  poprzez API, w przypadku brak produktu w bazie ręczne wprowadzanie danych,
  wybór typu posiłku (śniadanie, przekąska, obiad, kolacja) oraz zarządzanie listą dodanych
  produktów.
  - Funkcja zapisywanie sumarycznych makroskładników (kcal, białko, tłuszcze, węglowodany)
  do bazy Firestore i informuje użytkownika o statusie zapisu.
  - Funkcja MealTypeDropdown pozwala na wybór typu posiłku z rozwijanego menu.
 */


// Funkcja wyświetlająca pojedynczy wynik wyszukiwania produktu
@Composable
fun AddMealSearchResultItem(product: Product, onAddClick: (Float) -> Unit) {
    // Przechowywanie wartości wagi wpisanej przez użytkownika
    var weight by remember { mutableStateOf("") }

    // Karta prezentująca dane produktu wraz z polem do wprowadzenia wagi i przyciskiem "Add product"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Wyświetlenie nazwy produktu lub komunikatu "Brak nazwy" gdy nazwa jest pusta
            Text(
                text = product.productName ?: "Brak nazwy",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Jeśli dane o wartościach odżywczych są dostępne, wyświetl je
            product.nutriments?.let { nutriments ->
                Text(
                    "Kcal: ${nutriments.energyKcal ?: 0f} na 100g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Protein: ${nutriments.proteins ?: 0f} g na 100g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Fat: ${nutriments.fat ?: 0f} g na 100g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Carbs: ${nutriments.carbohydrates ?: 0f} g na 100g",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Pole tekstowe do wprowadzenia wagi produktu
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Enter weight (g)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Przycisk "Add product" - po kliknięciu, jeśli waga jest poprawna, wywołuje funkcję onAddClick
            Button(
                onClick = {
                    val weightValue = weight.toFloatOrNull()
                    if (weightValue != null && weightValue > 0) {
                        onAddClick(weightValue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onTertiary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Add product")
            }
        }
    }
}

// Główny ekran dodawania posiłku
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(navController: NavController) {
    // Scope do uruchamiania operacji asynchronicznych
    val coroutineScope = rememberCoroutineScope()

    // Stany dla zapytania wyszukiwania, ładowania, błędów, wyników wyszukiwania oraz listy dodanych produktów
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Product>>(emptyList()) }
    val addedMealItems = remember { mutableStateListOf<MealItem>() }

    // Stany kontrolujące wyświetlanie różnych dialogów
    var showManualConfirmDialog by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }
    var selectedMealType by remember { mutableStateOf("breakfast") }
    // Lista dostępnych typów posiłków
    val mealTypes = listOf("breakfast", "snack", "lunch", "dinner")
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<MealItem?>(null) }

    // Obliczanie sumarycznych wartości makroskładników dla dodanych produktów
    val totalKcal by remember { derivedStateOf { addedMealItems.sumOf { it.kcal.toDouble() }.toInt() } }
    val totalProteins by remember { derivedStateOf { addedMealItems.sumOf { it.proteins.toDouble() }.toInt() } }
    val totalFat by remember { derivedStateOf { addedMealItems.sumOf { it.fat.toDouble() }.toInt() } }
    val totalCarbs by remember { derivedStateOf { addedMealItems.sumOf { it.carbs.toDouble() }.toInt() } }

    // Ustalanie aktualnej daty w formacie odpowiednim dla Firestore
    val currentDateFirestore = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Inicjalizacja Firestore i FirebaseAuth
    val db = Firebase.firestore
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // Funkcja zapisująca sumaryczne makroskładniki posiłku do Firestore
    fun saveMacrosToFirestore() {
        if (user != null) {
            // Uzyskanie dostępu do kolekcji posiłków dla aktualnej daty i wybranego typu posiłku
            val mealCollection = db.collection("users").document(user.uid)
                .collection("dailyMacros").document(currentDateFirestore)
                .collection(selectedMealType)

            val summaryDocRef = mealCollection.document("${selectedMealType}Summary")

            // Pobranie dokumentu podsumowania i aktualizacja lub utworzenie nowego wpisu
            summaryDocRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Aktualizacja wartości poprzez inkrementację
                    summaryDocRef.update(
                        "kcal", FieldValue.increment(totalKcal.toLong()),
                        "carbs", FieldValue.increment(totalCarbs.toLong()),
                        "protein", FieldValue.increment(totalProteins.toLong()),
                        "fat", FieldValue.increment(totalFat.toLong())
                    ).addOnSuccessListener {
                        saveStatusMessage = "Successfully updated $selectedMealType!"
                        addedMealItems.clear()
                        navController.popBackStack()
                        // Ustawienie flagi do odświeżenia ekranu głównego
                        navController.currentBackStackEntry?.savedStateHandle?.set("refreshMainScreen", true)
                    }.addOnFailureListener { e ->
                        saveStatusMessage = "${selectedMealType} recording error: ${e.message}"
                    }
                } else {
                    // Utworzenie nowego dokumentu podsumowania, jeśli nie istnieje
                    summaryDocRef.set(
                        hashMapOf(
                            "kcal" to totalKcal,
                            "carbs" to totalCarbs,
                            "protein" to totalProteins,
                            "fat" to totalFat
                        )
                    ).addOnSuccessListener {
                        saveStatusMessage = "Successfully saved $selectedMealType!"
                        addedMealItems.clear()
                        navController.popBackStack()
                        navController.currentBackStackEntry?.savedStateHandle?.set("refreshMainScreen", true)
                    }.addOnFailureListener { e ->
                        saveStatusMessage = "${selectedMealType} recording error: ${e.message}"
                    }
                }
            }
        } else {
            // Komunikat o braku zalogowanego użytkownika
            saveStatusMessage = "The user is not logged in."
        }
    }

    // Dialog potwierdzający ręczne dodanie produktu, gdy wyszukiwanie nie zwróciło wyników
    if (showManualConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showManualConfirmDialog = false },
            title = { Text("Product not found") },
            text = { Text("Would you like to add a product from outside the list?") },
            confirmButton = {
                TextButton(onClick = {
                    showManualConfirmDialog = false
                    showManualInputDialog = true
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualConfirmDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    // Dialog umożliwiający ręczne wprowadzenie danych produktu
    if (showManualInputDialog) {
        var manualProductName by remember { mutableStateOf("") }
        var manualKcal by remember { mutableStateOf("") }
        var manualProteins by remember { mutableStateOf("") }
        var manualFat by remember { mutableStateOf("") }
        var manualCarbs by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("Add product manually") },
            text = {
                Column {
                    // Pola tekstowe do wprowadzenia danych produktu
                    OutlinedTextField(
                        value = manualProductName,
                        onValueChange = { manualProductName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualKcal,
                        onValueChange = { manualKcal = it },
                        label = { Text("Kcal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualProteins,
                        onValueChange = { manualProteins = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualFat,
                        onValueChange = { manualFat = it },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualCarbs,
                        onValueChange = { manualCarbs = it },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Konwersja wpisanych wartości na liczby i utworzenie obiektu MealItem
                    val kcalValue = manualKcal.toFloatOrNull() ?: 0f
                    val proteinsValue = manualProteins.toFloatOrNull() ?: 0f
                    val fatValue = manualFat.toFloatOrNull() ?: 0f
                    val carbsValue = manualCarbs.toFloatOrNull() ?: 0f
                    val mealItem = MealItem(
                        productName = manualProductName.takeIf { it.isNotBlank() } ?: "$searchQuery (manual)",
                        kcal = kcalValue,
                        proteins = proteinsValue,
                        fat = fatValue,
                        carbs = carbsValue
                    )
                    addedMealItems.add(mealItem)
                    showManualInputDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog potwierdzający usunięcie elementu z listy dodanych produktów
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                itemToDelete = null
            },
            title = { Text("Delete item?") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                TextButton(onClick = {
                    itemToDelete?.let { mealItemToDelete ->
                        addedMealItems.remove(mealItemToDelete)
                    }
                    showDeleteConfirmationDialog = false
                    itemToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmationDialog = false
                    itemToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Główny layout ekranu z paskiem nawigacyjnym (TopAppBar) i zawartością ekranu
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Pasek tytułowy z nazwą ekranu i logo aplikacji
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Meal",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(100.dp)
                                .padding(end = 8.dp)
                        )
                    }
                },
                navigationIcon = {
                    // Ikona umożliwiająca powrót do poprzedniego ekranu
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Sekcja wyboru typu posiłku
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Meal Type:",
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Rozwijane menu wyboru typu posiłku
                MealTypeDropdown(
                    selectedMealType = selectedMealType,
                    onMealTypeChange = { selectedMealType = it },
                    mealTypes = mealTypes
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Pole wyszukiwania produktu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Enter product name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Przycisk wyszukiwania - uruchamia zapytanie do API
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        isLoading = true
                        errorMessage = ""
                        coroutineScope.launch {
                            try {
                                // Wykonanie zapytania do API o produkty
                                val response = OpenFoodApi.service.searchProduct(searchQuery)
                                if (response.isSuccessful) {
                                    // Filtrowanie wyników wyszukiwania, aby wykluczyć puste dane
                                    val products = response.body()?.products?.filter {
                                        it.productName != null && it.nutriments != null
                                    } ?: emptyList()
                                    searchResults = products
                                    // Jeśli lista wyników jest pusta, zaproponuj dodanie produktu ręcznie
                                    if (products.isEmpty()) {
                                        showManualConfirmDialog = true
                                    }
                                } else {
                                    errorMessage = "Error: ${response.message()}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Exception: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Search")
            }
            // Wyświetlanie wskaźnika ładowania, jeśli trwa zapytanie
            if (isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            // Wyświetlanie komunikatu błędu, jeśli wystąpił
            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Sekcja wyświetlania wyników wyszukiwania
            Text("Search Results:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(searchResults) { product ->
                    // Dla każdego znalezionego produktu, wyświetl komponent AddMealSearchResultItem
                    AddMealSearchResultItem(product = product, onAddClick = { weight ->
                        val nutriments = product.nutriments
                        val scale = weight / 100f
                        // Utworzenie obiektu MealItem na podstawie danych produktu i wagi
                        val mealItem = MealItem(
                            productName = product.productName ?: "No Name",
                            kcal = (nutriments?.energyKcal ?: 0f) * scale,
                            proteins = (nutriments?.proteins ?: 0f) * scale,
                            fat = (nutriments?.fat ?: 0f) * scale,
                            carbs = (nutriments?.carbohydrates ?: 0f) * scale
                        )
                        addedMealItems.add(mealItem)
                        // Czyszczenie wyników wyszukiwania i zapytania
                        searchResults = emptyList()
                        searchQuery = ""
                    })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Sekcja wyświetlająca listę dodanych produktów
            Text("Added products:", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                items(items = addedMealItems) { mealItem: MealItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                // Wyświetlenie nazwy produktu dodanego do posiłku
                                Text(
                                    text = mealItem.productName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Wyświetlenie szczegółowych wartości makroskładników
                                Text(
                                    text = "${mealItem.kcal.toInt()} kcal, ${mealItem.proteins.toInt()} P, ${mealItem.fat.toInt()} F, ${mealItem.carbs.toInt()} C",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Ikona usuwania produktu z listy
                            IconButton(onClick = {
                                itemToDelete = mealItem
                                showDeleteConfirmationDialog = true
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    "Delete item",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Sekcja wyświetlająca podsumowanie makroskładników dodanego posiłku
            Text("Summary:", style = MaterialTheme.typography.titleMedium)
            Text("Kcal: $totalKcal")
            Text("Protein: $totalProteins g")
            Text("Fat: $totalFat g")
            Text("Carbs: $totalCarbs g")

            Spacer(modifier = Modifier.height(16.dp))
            // Przycisk zapisujący dane do Firestore
            Button(
                onClick = {
                    if (addedMealItems.isNotEmpty()) {
                        saveMacrosToFirestore()
                    } else {
                        saveStatusMessage = "Add product before saving."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save")
            }

            // Wyświetlanie komunikatu statusu zapisu, który znika po 3 sekundach
            saveStatusMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message)
                LaunchedEffect(key1 = message) {
                    delay(3000)
                    saveStatusMessage = null
                }
            }
        }
    }
}

// Funkcja wyświetlająca rozwijane menu wyboru typu posiłku
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTypeDropdown(
    selectedMealType: String,
    onMealTypeChange: (String) -> Unit,
    mealTypes: List<String>
) {
    // Stan kontrolujący rozwinięcie menu
    var expanded by remember { mutableStateOf(false) }

    // Kontener rozwijanego menu
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        // Pole tekstowe wyświetlające aktualnie wybrany typ posiłku
        OutlinedTextField(
            readOnly = true,
            value = selectedMealType.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            onValueChange = {},
            label = { Text("Meal Type", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        // Lista rozwijanego menu z dostępnymi typami posiłków
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            mealTypes.forEach { mealType ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mealType.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        onMealTypeChange(mealType)
                        expanded = false
                    }
                )
            }
        }
    }
}
