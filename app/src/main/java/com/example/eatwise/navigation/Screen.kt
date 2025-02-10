package com.example.eatwise.navigation

// Każda podklasa reprezentuje jeden ekran aplikacji.
sealed class Screen(val route: String) {

    // Ekran startowy
    object Start : Screen("start")
    // Ekran logowania
    object Login : Screen("login")
    // Ekrany rejestracji (etapy 1 i 2)
    object SignUpStep1 : Screen("signUpStep1")
    object SignUpStep2 : Screen("signUpStep2")
    // Główny ekran
    object Main : Screen("main")
    // Ekran dodawania posiłku
    object AddMeal : Screen("addMeal")
    // Kalendarz
    object Calendar : Screen("calendar")
    // Wykres wagi
    object WeightChart : Screen("weightChart")
    // Profil użytkownika
    object Profile : Screen("profile")
}
