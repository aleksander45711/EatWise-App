package com.example.eatwise.screens

/*
  Definiuje drugi krok rejestracji użytkownika (SignUpScreenStep2) w aplikacji EatWise.
  Umożliwia on użytkownikowi wprowadzenie dodatkowych danych, takich jak:
  - Aktualna waga, docelowa waga oraz wzrost.
  - Data urodzenia (rok, miesiąc, dzień), z której obliczany jest wiek.
  - Wybór płci oraz poziomu aktywności.
  - Wprowadzonoa walidacje wartosci.
  Wprowadzone dane są przetwarzane (np. obliczany jest wiek, określana wartość dietetyczna)
  i zapisywane w bazie Firebase Firestore. Ekran zawiera również elementy nawigacyjne,
  pozwalające na powrót do poprzedniego kroku rejestracji.
 */

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eatwise.R
import com.example.eatwise.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Pomocnicza funkcja obliczająca wiek na podstawie daty urodzenia.
fun calculateAge(birthDate: Date): Int {
    val today = Calendar.getInstance()
    val birth = Calendar.getInstance().apply { time = birthDate }
    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
        age--
    }
    return age
}

@Composable
fun SignUpScreenStep2(navController: NavController) {
    // Pola tekstowe dla parametrów użytkownika
    var currentWeight by remember { mutableStateOf("") }
    var goalWeight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    var birthMonth by remember { mutableStateOf("") }
    var birthDay by remember { mutableStateOf("") }

    // Wybór płci
    val genderOptions = listOf("Men", "Woman")
    var selectedGender by remember { mutableStateOf(genderOptions[0]) }
    val genderValue = if (selectedGender == "Men") 5 else -161

    // Wybór poziomu aktywności
    val activityOptions = listOf("Low", "Medium", "High")
    var selectedActivity by remember { mutableStateOf(activityOptions[0]) }
    val activityValue = when (selectedActivity) {
        "Low" -> 1.45
        "Medium" -> 1.65
        "High" -> 1.85
        else -> 1.45
    }

    // Kontekst i instancje Firebase
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Image(
                painter = painterResource(id = R.drawable.header_image),
                contentDescription = "Header",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { navController.navigate(Screen.SignUpStep1.route) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onBackground
                    )
                }
                Text(
                    text = "Sign Up",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pola tekstowe dla wagi i wzrostu
            listOf(
                "Current Weight (Kg)" to currentWeight,
                "Goal Weight (Kg)" to goalWeight,
                "Height (Cm)" to height
            ).forEach { (label, value) ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue ->
                        when (label) {
                            "Current Weight (Kg)" -> currentWeight = newValue
                            "Goal Weight (Kg)" -> goalWeight = newValue
                            "Height (Cm)" -> height = newValue
                        }
                    },
                    label = { Text(label, color = colors.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Grupa pól dla daty urodzenia
            Text(
                text = "Birth Date",
                fontSize = 18.sp,
                color = colors.onBackground
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = birthYear,
                    onValueChange = { birthYear = it },
                    label = { Text("Year", color = colors.onSurfaceVariant) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = birthMonth,
                    onValueChange = { birthMonth = it },
                    label = { Text("Month", color = colors.onSurfaceVariant) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = birthDay,
                    onValueChange = { birthDay = it },
                    label = { Text("Day", color = colors.onSurfaceVariant) },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wybór płci
            Text(text = "Select Gender", fontSize = 18.sp, color = colors.onBackground)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                genderOptions.forEach { gender ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (gender == selectedGender),
                            onClick = { selectedGender = gender },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.onSurfaceVariant
                            )
                        )
                        Text(text = gender, color = colors.onBackground)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wybór aktywności
            Text(text = "Select Activity Level", fontSize = 18.sp, color = colors.onBackground)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                activityOptions.forEach { activity ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (activity == selectedActivity),
                            onClick = { selectedActivity = activity },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.onSurfaceVariant
                            )
                        )
                        Text(text = activity, color = colors.onBackground)
                    }
                }
            }
        }

        Button(
            onClick = {
                val user = auth.currentUser
                if (user != null) {
                    val userId = user.uid

                    // Parsowanie wartości liczbowych
                    val currentWeightValue = currentWeight.toDoubleOrNull() ?: 0.0
                    val goalWeightValue = goalWeight.toDoubleOrNull() ?: 0.0
                    val heightValue = height.toDoubleOrNull() ?: 0.0

                    // Parsowanie pól dla daty urodzenia
                    val birthYearInt = birthYear.toIntOrNull()
                    val birthMonthInt = birthMonth.toIntOrNull()
                    val birthDayInt = birthDay.toIntOrNull()

                    // Sprawdzenie czy data urodzenia została poprawnie wprowadzona
                    if (birthYearInt == null || birthMonthInt == null || birthDayInt == null) {
                        Toast.makeText(context, "Please enter a valid birth date", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Walidacja currentWeight: wartość od 1 do 1000
                    if (currentWeightValue < 1 || currentWeightValue > 1000) {
                        Toast.makeText(context, "Invalid Current Weight!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Walidacja goalWeight: wartość od 1 do 1000
                    if (goalWeightValue < 1 || goalWeightValue > 1000) {
                        Toast.makeText(context, "Invalid Goal Weight!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Walidacja height: wartość od 30 do 300
                    if (heightValue < 30 || heightValue > 300) {
                        Toast.makeText(context, "Invalid Height!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Walidacja daty urodzenia:
                    if (birthYearInt < 1890 || birthYearInt > 3000 ||
                        birthMonthInt < 1 || birthMonthInt > 12 ||
                        birthDayInt < 1 || birthDayInt > 31
                    ) {
                        Toast.makeText(context, "Invalid Birth Date", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Tworzymy datę urodzenia
                    val cal = Calendar.getInstance().apply {
                        set(birthYearInt, birthMonthInt - 1, birthDayInt)
                    }
                    val birthDateObj = cal.time
                    val computedAge = calculateAge(birthDateObj)

                    val dietValue = when {
                        currentWeightValue > goalWeightValue -> -300
                        currentWeightValue < goalWeightValue -> 300
                        else -> 0
                    }

                    // Tworzymy string z datą urodzenia w formacie "yyyy-MM-dd"
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val birthDateString = sdf.format(birthDateObj)

                    val userData = hashMapOf(
                        "currentWeight" to currentWeightValue,
                        "goalWeight" to goalWeightValue,
                        "height" to heightValue,
                        "birthDate" to birthDateString,
                        "age" to computedAge,
                        "genderValue" to genderValue,
                        "activityValue" to activityValue,
                        "dietValue" to dietValue
                    )

                    db.collection("users").document(userId).set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Data saved!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.Start.route) { inclusive = true }
                            }
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            )
        ) {
            Text("Sign Up", fontSize = 18.sp)
        }
    }
}
