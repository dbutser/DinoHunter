package com.dinohunters.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dinohunters.app.data.repository.DinoRepository
import com.dinohunters.app.ui.inventory.InventoryScreen
import com.dinohunters.app.ui.map.MapScreen
import com.dinohunters.app.ui.profile.ProfileScreen
import com.dinohunters.app.ui.theme.DinoHunterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dinoRepository: DinoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (dinoRepository.getUserProfile().firstOrNull() == null) {
                Log.d("MainActivity", "Профиль не найден. Создание нового профиля...")
                dinoRepository.createInitialProfile()
            } else {
                Log.d("MainActivity", "Профиль уже существует.")
            }
        }

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
        composable("map") { MapScreen(onNavigateToInventory = { navController.navigate("inventory") }, onNavigateToProfile = { navController.navigate("profile") }) }
        composable("inventory") { InventoryScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("profile") { ProfileScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}