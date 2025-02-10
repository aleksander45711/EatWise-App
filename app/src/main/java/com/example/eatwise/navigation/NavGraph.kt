package com.example.eatwise.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.eatwise.screens.*

/*
  Definiuje nawigację w aplikacji
  Miejsce do zarządzania ścieżkami ekranów.
*/
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController, // Kontroler nawigacji
        startDestination = Screen.Start.route // Pierwszy ekran, który się ładuje (ekran startowy)
    ) {
        // Ekran startowy
        composable(Screen.Start.route) { StartScreen(navController) }
        // Ekran logowania
        composable(Screen.Login.route) { LoginScreen(navController) }
        // Ekrany rejestracji (etapy 1 i 2)
        composable(Screen.SignUpStep1.route) { SignUpScreenStep1(navController) }
        composable(Screen.SignUpStep2.route) { SignUpScreenStep2(navController) }
        // Główny ekran
        composable(Screen.Main.route) { MainScreen(navController) }
        // Ekran dodawania posiłku
        composable(Screen.AddMeal.route) { AddMealScreen(navController) }
        // Ekran kalendarza
        composable(Screen.Calendar.route) { CalendarScreen(navController) }
        // Wykres wagi
        composable(Screen.WeightChart.route) { WeightChartScreen(navController) }
        // Profil użytkownika
        composable(Screen.Profile.route) { ProfileScreen(navController) }
    }
}
