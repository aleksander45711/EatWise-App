plugins {
    // Wtyczki do budowy aplikacji na Androida z użyciem Kotlin oraz Jetpack Compose
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Wtyczka Google Services (Firebase)
    id("com.google.gms.google-services")
}

android {
    // Ustawienia projektu Android
    namespace = "com.example.eatwise"  // przestrzeń nazw aplikacji
    compileSdk = 35                   // Wersja SDK, na której aplikacja jest kompilowana

    defaultConfig {
        applicationId = "com.example.eatwise"  // Identyfikator aplikacji
        minSdk = 24                            // Minimalna obsługiwana wersja SDK
        targetSdk = 35                         // Wersja SDK, dla której aplikacja jest optymalizowana
        versionCode = 1                        // Wewnętrzna wersja aplikacji
        versionName = "1.0"                    // Wersja aplikacji

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"  // Runner testów instrumentacyjnych
    }

    buildTypes {
        release {
            // Konfiguracja wersji release
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),  // Domyślny plik ProGuard
                "proguard-rules.pro"                                        // Niestandardowe reguły ProGuard
            )
        }
    }
    compileOptions {
        // Ustawienia kompatybilności z Javą
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        // Ustawienie docelowej wersji JVM dla Kotlin
        jvmTarget = "11"
    }
    buildFeatures {
        // Włączenie wsparcia dla Jetpack Compose
        compose = true
    }
    composeOptions {
        // Wersja rozszerzenia kompilatora Kotlin dla Compose
        kotlinCompilerExtensionVersion = "1.1.1"
    }
}

dependencies {
    // Material Icons rozszerzone dla Compose
    implementation(libs.androidx.compose.material.icons.extended)
    // Retrofit - biblioteka do wykonywania zapytań HTTP
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // Konwerter Gson do mapowania JSON na obiekty Kotlin
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Korutyny dla Retrofit (asynchroniczne operacje)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson - biblioteka do serializacji/deserializacji JSON
    implementation("com.google.code.gson:gson:2.10.1")
    // DataStore do przechowywania preferencji
    implementation(libs.androidx.datastore.preferences)
    // Core KTX, rozszerzenia Kotlin dla Androida
    implementation(libs.androidx.core.ktx)
    // Obsługa Lifecycle dla Androida
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Wsparcie dla Compose w Activity
    implementation(libs.androidx.activity.compose)
    // BOM dla Compose
    implementation(platform(libs.androidx.compose.bom))
    // Material3
    implementation(libs.androidx.material3)
    // Obsługa grafiki Compose
    implementation(libs.androidx.ui.graphics)
    // Narzędzia podglądu interfejsu w Compose
    implementation(libs.androidx.ui.tooling.preview)
    // Obsługa okien rozmiarowych w Material3
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    // Adaptacyjna nawigacja w Material3
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha07")

    // Jetpack Navigation dla Compose
    implementation(libs.androidx.navigation.compose)

    // Podstawowe biblioteki Jetpack Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.text)

    // Import Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))

    // Firebase Analytics do analizy zachowań użytkowników
    implementation("com.google.firebase:firebase-analytics")
    // Firebase Authentication do uwierzytelniania użytkowników
    implementation("com.google.firebase:firebase-auth")
    // Firebase Firestore baza danych NoSQL z rozszerzeniem KTX
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Compose Foundation
    implementation("androidx.compose.foundation:foundation:1.2.0")

    // Testowanie jednostkowe i instrumentacyjne
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}