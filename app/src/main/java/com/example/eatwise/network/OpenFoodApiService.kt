package com.example.eatwise.network

/*
  Definiuje metodę wyszukiwania produktów spożywczych z API openfoodfacts.
  Używa adnotacji Retrofit do określenia endpointu oraz parametrów zapytania.
 */
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFoodApiService {

    // Endpoint do wyszukiwania produktów spożywczych.
    @GET("cgi/search.pl?json=1")
    suspend fun searchProduct(
        @Query("search_terms") query: String  // Parametr "search_terms"
    ): Response<OpenFoodSearchResponse>
}
