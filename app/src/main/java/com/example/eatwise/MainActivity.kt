package com.example.eatwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.eatwise.navigation.AppNavGraph
import com.example.eatwise.ui.theme.EatWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EatWiseTheme {
                // Tworzymy instancję NavController:
                val navController = rememberNavController()

                // Wywołujemy AppNavGraph:
                AppNavGraph(navController = navController)
            }
        }
    }
}