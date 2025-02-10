package com.example.eatwise.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
  Definiuje ekran logowania (LoginScreen) dla aplikacji.
  Umożliwia użytkownikowi wpisanie e-maila oraz hasła, a następnie próbuje zalogować go przy użyciu FirebaseAuth.
  W zależności od wyniku logowania wyświetlany jest komunikat i następuje przejście do ekranu głównego lub komunikat o błędzie.
  Opcja zarejestrowania się jako nowy użytkownik. W tym celu przenosi nas do kolejnego ekranu SignUpScreenStep1
 */

@Composable
fun LoginScreen(navController: NavController) {
    // Używamy stanu do przechowywania wpisanego e-maila oraz hasła
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Pobieramy kontekst, który będzie używany przy wyświetlaniu Toast
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val colors = MaterialTheme.colorScheme

    // Główna kolumna zajmująca cały ekran z odstępami
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Górna sekcja – zawiera obrazek, tytuł oraz pola tekstowe do logowania
        Column {
            Image(
                painter = painterResource(id = R.drawable.header_image),
                contentDescription = "Header",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Login",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    // Próba logowania przy użyciu FirebaseAuth
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Logged in!", Toast.LENGTH_SHORT).show()
                                // Po udanym logowaniu przechodzimy do ekranu głównego
                                navController.navigate(Screen.Main.route)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Login failed: ${task.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Next",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Dolna sekcja – zawiera opcję rejestracji, z podziałem dekoracyjnym
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.width(60.dp),
                color = colors.onSurfaceVariant
            )
            // Tekst umożliwiający przejście do ekranu rejestracji po kliknięciu
            Text(
                text = " Or sign up with ",
                modifier = Modifier.clickable { navController.navigate(Screen.SignUpStep1.route) },
                color = colors.onSurfaceVariant
            )
            HorizontalDivider(
                modifier = Modifier.width(60.dp),
                color = colors.onSurfaceVariant
            )
        }
    }
}
