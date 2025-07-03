package com.dinohunters.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dinohunters.app.ui.inventory.InventoryScreen
import com.dinohunters.app.ui.map.MapScreen
import com.dinohunters.app.ui.profile.ProfileScreen
import com.dinohunters.app.ui.theme.DinoHunterTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DinoHunterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DinoHunterApp()
                }
            }
        }
    }
}

@Composable
fun DinoHunterApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "map") {
        composable("map") {
            MapScreen(
                onNavigateToInventory = { navController.navigate("inventory") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }
        composable("inventory") {
            InventoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("profile") {
            ProfileScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}