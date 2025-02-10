package com.example.eatwise.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.example.eatwise.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

/*
  Implementacja ekranu wykresu wagi (Weight Chart).
  Główne funkcjonalnosci:
  - Wizualizacja historii wagi użytkownika na wykresie.
  - Wyświetlanie szczegółów (data oraz waga) po dotknięciu punktu na wykresie.
  - Funkcja aktualizacji bieżącej wagi.
 */

// Stała określająca strefę czasową UTC
private val utcTimeZone = TimeZone.getTimeZone("UTC")

// Główny ekran wykresu wagi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightChartScreen(navController: NavController) {
    // Uzyskanie instancji FirebaseAuth i pobranie aktualnie zalogowanego użytkownika
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    // Pobranie kontekstu (do wyświetlania Toastów)
    val context = LocalContext.current

    // Jeśli użytkownik nie jest zalogowany, wyświetl komunikat o braku zalogowania oraz pasek nawigacji
    if (user == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Weight Chart",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    // Pozycja "Home"
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Screen.Main.route) },
                        icon = {
                            Icon(
                                Icons.Filled.Home,
                                contentDescription = "Home",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        label = { Text("Home", color = MaterialTheme.colorScheme.onSurface) }
                    )
                    // Pozycja "Calendar"
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Screen.Calendar.route) },
                        icon = {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = "Calendar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        label = { Text("Calendar", color = MaterialTheme.colorScheme.onSurface) }
                    )
                    // Pozycja "Weight Chart" - aktualnie wybrana
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* już tu jesteśmy */ },
                        icon = {
                            Icon(
                                Icons.Filled.PieChart,
                                contentDescription = "Weight Chart",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        label = { Text("Weight Chart", color = MaterialTheme.colorScheme.primary) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.tertiary,
                            selectedTextColor = MaterialTheme.colorScheme.tertiary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    // Pozycja "Profile"
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(Screen.Profile.route) },
                        icon = {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        label = { Text("Profile", color = MaterialTheme.colorScheme.onSurface) }
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            // Wyświetlenie komunikatu, że użytkownik nie jest zalogowany
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Użytkownik nie jest zalogowany",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }

    // Inicjalizacja Firestore oraz scope do operacji asynchronicznych
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()

    // Zmienne stanu przechowujące bieżącą wagę, historię wag, dane z pola wejściowego oraz wybrany punkt wykresu
    var currentWeight by remember { mutableStateOf(0f) }
    var weightHistory by remember { mutableStateOf<List<Pair<Date, Float>>>(emptyList()) }
    var newWeightInput by remember { mutableStateOf("") }
    var selectedPoint by remember { mutableStateOf<Pair<Date, Float>?>(null) }

    // Pobranie danych dotyczących wagi użytkownika oraz historii wag z Firestore
    LaunchedEffect(user.uid) {
        try {
            // Pobranie dokumentu użytkownika z Firestore i aktualizacja bieżącej wagi
            val userDoc = db.collection("users").document(user.uid).get().await()
            val currentWeightVal = userDoc.getDouble("currentWeight")?.toFloat() ?: 0f
            currentWeight = currentWeightVal

            // Ustawienie formatu daty "yyyy-MM-dd" oraz strefy czasowej UTC
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = utcTimeZone
            }
            // Pobranie dzisiejszej daty
            val todayCal = Calendar.getInstance(utcTimeZone).apply { time = Date() }
            val todayStr = sdf.format(todayCal.time)
            // Odwołanie do dokumentu dziennej wagi dla dzisiaj
            val todayDocRef = db.collection("users").document(user.uid)
                .collection("dailyWeight")
                .document(todayStr)
            val todaySnapshot = todayDocRef.get().await()
            // Jeśli dokument dzisiejszej wagi nie istnieje, utwórz go z bieżącą wagą
            if (!todaySnapshot.exists()) {
                todayDocRef.set(mapOf("weight" to currentWeightVal)).await()
            }

            // Tworzenie listy dat od daty rejestracji do dzisiaj
            val creationTimestamp = user.metadata?.creationTimestamp ?: 0L
            val startDate = Date(creationTimestamp)
            val endDate = Date()
            val cal = Calendar.getInstance().apply {
                time = startDate
                timeZone = utcTimeZone
            }
            val dates = mutableListOf<Date>()
            while (!cal.time.after(endDate)) {
                dates.add(cal.time)
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Pobranie wszystkich dokumentów z kolekcji "dailyWeight"
            val querySnapshot = db.collection("users").document(user.uid)
                .collection("dailyWeight")
                .get()
                .await()
            // Mapowanie dokumentów na pary: (data w formacie "yyyy-MM-dd", waga)
            val weightMap: Map<String, Float> = querySnapshot.documents.associate { doc ->
                val dateStr = doc.id  // Identyfikatory dokumentów są datami w formacie "yyyy-MM-dd"
                val w = doc.getDouble("weight")?.toFloat() ?: currentWeight
                dateStr to w
            }
            // Upewnienie się, że data rejestracji jest obecna w mapie wag
            val creationDateStr = sdf.format(startDate)
            val updatedWeightMap = weightMap.toMutableMap()
            if (!updatedWeightMap.containsKey(creationDateStr)) {
                updatedWeightMap[creationDateStr] = currentWeightVal
            }
            // Przechodzimy przez wszystkie daty, uzupełniając brakujące wartości wag
            var lastWeight = updatedWeightMap[creationDateStr]!!
            val history = dates.map { date ->
                val dateStr = sdf.format(date)
                // Jeśli dla danej daty nie ma wpisu, przyjmujemy ostatnią znaną wagę
                val w = updatedWeightMap[dateStr] ?: lastWeight
                lastWeight = w
                date to w
            }
            weightHistory = history
        } catch (e: Exception) {
            Log.e("WeightChartScreen", "Błąd pobierania danych: ${e.message}")
        }
    }

    // Pobranie gęstości ekranu (do rysowania tekstu i kształtów)
    val density = LocalDensity.current.density

    // Pobranie kolorów z motywu, aby ułatwić ich użycie
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary

    // Główna struktura ekranu wykresu z paskiem u góry, nawigacją na dole i zawartością w środku
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Weight Chart",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                },
                colors = topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                // Pozycja "Home"
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Main.route) },
                    icon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Home",
                            tint = onSurface
                        )
                    },
                    label = { Text("Home", color = onSurface) }
                )
                // Pozycja "Calendar"
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Calendar.route) },
                    icon = {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = "Calendar",
                            tint = onSurface
                        )
                    },
                    label = { Text("Calendar", color = onSurface) }
                )
                // Pozycja "Weight Chart" - aktualnie wybrana
                NavigationBarItem(
                    selected = true,
                    onClick = {  },
                    icon = {
                        Icon(
                            Icons.Filled.PieChart,
                            contentDescription = "Weight Chart",
                            tint = primary
                        )
                    },
                    label = { Text("Weight Chart", color = primary) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        selectedTextColor = MaterialTheme.colorScheme.tertiary,
                        indicatorColor = Color.Transparent
                    )
                )
                // Pozycja "Profile"
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Profile.route) },
                    icon = {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = onSurface
                        )
                    },
                    label = { Text("Profile", color = onSurface) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Obszar wykresu – opakowany w BoxWithConstraints umożliwiający dynamiczne określenie rozmiarów
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Obliczenie szerokości widocznego obszaru wykresu w pikselach
                val viewportWidthPx = constraints.maxWidth.toFloat()
                // Ustalona liczba punktów, które mają być widoczne jednocześnie
                val fixedVisiblePoints = 5
                // Margines z prawej strony wykresu
                val rightMarginPx = 50f
                // Obliczenie odstępu między punktami wykresu
                val spacing = (viewportWidthPx - rightMarginPx) / (fixedVisiblePoints - 1)
                val n = weightHistory.size
                // Obliczenie całkowitej szerokości zawartości wykresu
                val contentWidth = if (n > fixedVisiblePoints) {
                    (n - 1) * spacing + rightMarginPx
                } else {
                    viewportWidthPx
                }
                // Jeśli punktów jest mniej niż fixedVisiblePoints, wycentrujemy je poziomo
                val xOffset = if (n in 1 until fixedVisiblePoints) {
                    (viewportWidthPx - ((n - 1) * spacing)) / 2f
                } else {
                    0f
                }
                // Użycie stanu scrollowania poziomego
                val scrollState = rememberScrollState()

                // Kontener umożliwiający przewijanie zawartości wykresu w poziomie
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                ) {
                    // Rysowanie wykresu przy użyciu Canvas
                    Canvas(
                        modifier = Modifier
                            .width(with(LocalDensity.current) { contentWidth.toDp() })
                            .fillMaxHeight()
                            // Obsługa gestów – wykrywanie dotknięć na wykresie
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    if (n > 0) {
                                        var minDistance = Float.MAX_VALUE
                                        var closestIndex = -1
                                        // Znalezienie najbliższego punktu do miejsca dotknięcia
                                        for (i in 0 until n) {
                                            val xPos = xOffset + i.toFloat() * spacing
                                            val d = kotlin.math.abs(xPos - tapOffset.x)
                                            if (d < minDistance) {
                                                minDistance = d
                                                closestIndex = i
                                            }
                                        }
                                        // Jeśli odległość jest mniejsza od progu, ustaw wybrany punkt
                                        selectedPoint = if (closestIndex != -1 && minDistance < 20f)
                                            weightHistory[closestIndex]
                                        else null
                                    }
                                }
                            }
                    ) {
                        val chartWidth = size.width
                        val chartHeight = size.height

                        // Rysowanie osi X – linia u dołu wykresu
                        drawLine(
                            color = onSurface,
                            start = Offset(0f, chartHeight),
                            end = Offset(chartWidth, chartHeight),
                            strokeWidth = 2f
                        )
                        // Rysowanie osi Y – linia po prawej stronie wykresu
                        drawLine(
                            color = onSurface,
                            start = Offset(chartWidth, 0f),
                            end = Offset(chartWidth, chartHeight),
                            strokeWidth = 2f
                        )

                        // Rysowanie etykiety osi Y ("Weight (kg)") po prawej stronie
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                "Weight (kg)",
                                chartWidth - 10f,
                                20f,
                                android.graphics.Paint().apply {
                                    color = onSurface.toArgb()
                                    textSize = 14f * density
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        }

                        // Ustalenie zakresu wag (minimalna i maksymalna wartość)
                        val minWeight = 30f
                        val maxWeight = 249f
                        val yTickStep = 10f
                        val startTick = minWeight + yTickStep
                        val lastTick = maxWeight - yTickStep

                        // Rysowanie linii siatki i etykiet dla osi Y
                        for (tick in generateSequence(startTick) { if (it <= lastTick) it + yTickStep else null }) {
                            val yPos = chartHeight - ((tick - minWeight) / (maxWeight - minWeight)) * chartHeight
                            drawLine(
                                color = onSurface.copy(alpha = 0.3f),
                                start = Offset(0f, yPos),
                                end = Offset(chartWidth, yPos),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = onSurface,
                                start = Offset(0f, yPos),
                                end = Offset(10f, yPos),
                                strokeWidth = 2f
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                tick.toInt().toString(),
                                chartWidth - 10f,
                                yPos + 5f,
                                android.graphics.Paint().apply {
                                    color = onSurface.toArgb()
                                    textSize = 12f * density
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        }

                        // Formatowanie dat dla osi X
                        val sdfLabel = SimpleDateFormat("MM/dd", Locale.getDefault())
                        if (n > 1) {
                            val desiredXLabels = 5
                            val labelStep = max(1, (n - 1) / (desiredXLabels - 1))
                            // Rysowanie linii siatki i etykiet dla osi X
                            for (i in 0 until n step labelStep) {
                                val xPos = xOffset + i.toFloat() * spacing
                                drawLine(
                                    color = onSurface.copy(alpha = 0.3f),
                                    start = Offset(xPos, 0f),
                                    end = Offset(xPos, chartHeight),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = onSurface,
                                    start = Offset(xPos, chartHeight),
                                    end = Offset(xPos, chartHeight - 10f),
                                    strokeWidth = 2f
                                )
                                val dateLabel = sdfLabel.format(weightHistory[i].first)
                                drawContext.canvas.nativeCanvas.drawText(
                                    dateLabel,
                                    xPos,
                                    chartHeight + 30f,
                                    android.graphics.Paint().apply {
                                        color = onSurface.toArgb()
                                        textSize = 12f * density
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                )
                            }
                        } else if (n == 1) {
                            // Jeśli tylko jeden punkt, wyświetl etykietę wycentrowaną
                            val xPos = xOffset
                            drawLine(
                                color = onSurface,
                                start = Offset(xPos, chartHeight),
                                end = Offset(xPos, chartHeight - 10f),
                                strokeWidth = 2f
                            )
                            val dateLabel = sdfLabel.format(weightHistory[0].first)
                            drawContext.canvas.nativeCanvas.drawText(
                                dateLabel,
                                xPos,
                                chartHeight + 30f,
                                android.graphics.Paint().apply {
                                    color = onSurface.toArgb()
                                    textSize = 12f * density
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }

                        // Rysowanie statycznego podpisu "Date" na osi X
                        drawContext.canvas.nativeCanvas.drawText(
                            "Date",
                            chartWidth - 10f,
                            chartHeight + 60f,
                            android.graphics.Paint().apply {
                                color = onSurface.toArgb()
                                textSize = 14f * density
                                textAlign = android.graphics.Paint.Align.RIGHT
                            }
                        )

                        // Rysowanie punktów na wykresie reprezentujących wagę
                        for ((index, pair) in weightHistory.withIndex()) {
                            val (_, w) = pair
                            // Ograniczenie wagi do zakresu minWeight i maxWeight
                            val weightClamped = w.coerceIn(minWeight, maxWeight)
                            val xPos = xOffset + index.toFloat() * spacing
                            val yPos = chartHeight - ((weightClamped - minWeight) / (maxWeight - minWeight)) * chartHeight
                            drawCircle(
                                color = primary,
                                radius = 6f,
                                center = Offset(xPos, yPos)
                            )
                        }
                    }
                }
            }

            // Jeśli użytkownik dotknął punktu wykresu, wyświetl okno dialogowe z jego szczegółami
            selectedPoint?.let { (date, weight) ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = utcTimeZone
                }
                AlertDialog(
                    onDismissRequest = { selectedPoint = null },
                    title = { Text("Details", color = MaterialTheme.colorScheme.onBackground) },
                    text = { Text("Date: ${sdf.format(date)}\nWeight: $weight kg", color = MaterialTheme.colorScheme.onBackground) },
                    confirmButton = {
                        TextButton(onClick = { selectedPoint = null }) {
                            Text("Close", color = primary)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sekcja umożliwiająca wpisanie nowej wagi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newWeightInput,
                    onValueChange = { newWeightInput = it },
                    label = { Text("Enter new weight (kg)", color = onSurface) },
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Przycisk "Update" aktualizujący wagę użytkownika
                TextButton(
                    onClick = {
                        val newWeight = newWeightInput.toFloatOrNull()
                        if (newWeight != null) {
                            coroutineScope.launch {
                                try {
                                    // Pobranie dokumentu użytkownika w celu odczytu wagi docelowej
                                    val userDoc = db.collection("users").document(user.uid).get().await()
                                    val goalWeightVal = userDoc.getDouble("goalWeight")?.toFloat() ?: 0f
                                    // Ustalanie wartości zmiany diety w zależności od tego, czy nowa waga jest wyższa lub niższa od docelowej
                                    val newDietValue = when {
                                        newWeight > goalWeightVal -> -300f
                                        newWeight < goalWeightVal -> 300f
                                        else -> 0f
                                    }
                                    // Aktualizacja bieżącej wagi użytkownika
                                    db.collection("users").document(user.uid)
                                        .update("currentWeight", newWeight)
                                        .await()
                                    // Aktualizacja wartości dietValue użytkownika
                                    db.collection("users").document(user.uid)
                                        .update("dietValue", newDietValue)
                                        .await()
                                    // Aktualizacja dziennego wpisu w kolekcji "dailyWeight" dla dzisiejszej daty
                                    val todayCal = Calendar.getInstance(utcTimeZone).apply { time = Date() }
                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                                        timeZone = utcTimeZone
                                    }
                                    val todayStr = sdf.format(todayCal.time)
                                    db.collection("users").document(user.uid)
                                        .collection("dailyWeight")
                                        .document(todayStr)
                                        .set(mapOf("weight" to newWeight))
                                        .await()
                                    currentWeight = newWeight
                                    newWeightInput = ""
                                    // Aktualizacja historii wag – jeśli wpis dla dzisiejszej daty istnieje, zastąp go nową wagą
                                    weightHistory = weightHistory.map { (date, w) ->
                                        if (sdf.format(date) == todayStr) date to newWeight else date to w
                                    }
                                    // Wyświetlenie Toast, jeśli nowa waga jest równa wadze docelowej
                                    if (newWeight == goalWeightVal) {
                                        Toast.makeText(
                                            context,
                                            "CONGRATULATIONS!!! You've achieved your goal weight!!!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("WeightChartScreen", "Błąd aktualizacji wagi/dietValue: ${e.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = primary)
                ) {
                    Text("Update", color = primary)
                }
            }
        }
    }
}
