package com.example.eatwise.network

/*
  Obiekt inicjalizuje Retrofit, konfigurując go z bazowym adresem API openfoodfacts
  oraz z konwerterem Gson, który przekształca odpowiedzi JSON na obiekty Kotlin.
  Udostępnia interfejs OpenFoodApiService, który pozwala wykonywać zapytania do API.
*/

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenFoodApi {
    // Inicjalizacja Retrofit z bazowym URL API
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/") // Główny adres API
        .addConverterFactory(GsonConverterFactory.create()) // Konwersja odpowiedzi JSON na obiekty Kotlin
        .build()

    // Utworzenie instancji interfejsu OpenFoodApiService do wykonywania zapytań
    val service: OpenFoodApiService = retrofit.create(OpenFoodApiService::class.java)
}