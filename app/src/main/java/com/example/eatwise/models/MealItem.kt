package com.example.eatwise.models

// Przechowuje dane o posiłku
data class MealItem(
    val productName: String,
    val kcal: Float,
    val proteins: Float,
    val fat: Float,
    val carbs: Float
)