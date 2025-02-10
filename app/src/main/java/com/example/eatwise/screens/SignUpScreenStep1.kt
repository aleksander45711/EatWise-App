package com.example.eatwise.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.eatwise.R
import com.example.eatwise.navigation.Screen
import com.google.firebase.auth.FirebaseAuth

/*
  Definiuje ekran pierwszego kroku rejestracji (SignUpScreenStep1) użytkownika w aplikacji.
  Umożliwia użytkownikowi wpisanie imienia, adresu e-mail oraz hasła.
  Po kliknięciu przycisku "Next", następuje próba utworzenia konta w Firebase Authentication.
  Jeśli rejestracja się powiedzie, użytkownik zostaje przeniesiony do następnego ekranu rejestracji.
  W przypadku błędu, wyświetlany jest odpowiedni komunikat.
 */

@Composable
fun SignUpScreenStep1(navController: NavController) {
    // Stan przechowujący dane wprowadzane przez użytkownika
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val colors = MaterialTheme.colorScheme

    // Główna powierzchnia aplikacji
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Górna sekcja ekranu: nagłówek, przycisk powrotu i pola formularza
            Column {
                Image(
                    painter = painterResource(id = R.drawable.header_image),
                    contentDescription = "Header",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Wiersz z przyciskiem powrotu i nagłówkiem "Sign Up"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { navController.navigate(Screen.Login.route) }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pola formularza dla imienia, e-maila i hasła
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = colors.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = colors.onSurfaceVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = colors.onSurfaceVariant) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = colors.onSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Przycisk "Next" do przejścia do kolejnego etapu rejestracji
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Konto zostało utworzone
                                    Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                    navController.navigate(Screen.SignUpStep2.route)
                                } else {
                                    // Błąd w rejestracji
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        // Użytkownik nie podał wymaganych danych
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    text = "Next",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
