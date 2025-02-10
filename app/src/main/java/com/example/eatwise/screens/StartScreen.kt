package com.example.eatwise.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.eatwise.R
import com.example.eatwise.navigation.Screen
/*
  Wyświetla ekran startowy (StartScreen) aplikacji.
  Zawiera logo aplikacji, tekst powitalny i przycisk "Get Started" przenoszący do ekranu Login
 */
@Composable
fun StartScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image( // Funkcja wyswietlająca logo
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo aplikacji",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text( // Funkcja wyświetlająca tekst.
            "Your smart companion for tracking meals,\ncalories, and achieving your fitness goals!",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp)) // Dodanie pustego przestrzeni (spacera) o wysokości 32dp.

        Button( // Funkcja wyświetlająca przycisk.
            onClick = {
                navController.navigate(Screen.Login.route)
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Get Started")
        }
    }
}