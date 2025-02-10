package com.example.eatwise.network

/*
  Definiuje modele danych, które odpowiadają strukturze odpowiedzi zwracanej przez API openfoodfacts.
  Modele obejmują listę produktów, szczegóły produktu oraz informacje o makroskładnikach.
 */
import com.google.gson.annotations.SerializedName

// Model odpowiedzi z API, zawierający listę produktów
data class OpenFoodSearchResponse(
    val products: List<Product>?
)

// Model pojedynczego produktu spożywczego
data class Product(
    @SerializedName("product_name")
    val productName: String?,  // Nazwa produktu
    @SerializedName("nutriments")
    val nutriments: Nutriments? // Informacje o makroskładnikach produktu
)

// Model makroskładników produktu
data class Nutriments(
    @SerializedName("energy-kcal_100g")
    val energyKcal: Float?,    // Kalorie (dla 100g)
    @SerializedName("carbohydrates_100g")
    val carbohydrates: Float?, // Węglowodany (dla 100g)
    @SerializedName("proteins_100g")
    val proteins: Float?,      // Białko (dla 100g)
    @SerializedName("fat_100g")
    val fat: Float?            // Tłuszcze (dla 100g)
)
