package com.dinohunters.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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
import android.Manifest // Импортируем Manifest

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dinoRepository: DinoRepository

    // Объявляем ActivityResultLauncher для запроса разрешения
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Разрешение ACTIVITY_RECOGNITION получено.")
                // Здесь вы можете начать слушать шаги или активировать связанные функции
                // (например, инициализировать StepCounterManager)
            } else {
                Log.d("MainActivity", "Разрешение ACTIVITY_RECOGNITION отклонено.")
                // Объясните пользователю, почему разрешение важно, или отключите функцию подсчета шагов
                // Toast.makeText(this, "Для подсчета шагов требуется разрешение на активность.", Toast.LENGTH_LONG).show()
            }
        }

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

        // Вызываем проверку и запрос разрешения
        checkAndRequestActivityRecognitionPermission()

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

    // Функция для проверки и запроса разрешения
    private fun checkAndRequestActivityRecognitionPermission() {
        // Разрешение ACTIVITY_RECOGNITION требуется только начиная с Android Q (API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Разрешение уже предоставлено
                    Log.d("MainActivity", "Разрешение ACTIVITY_RECOGNITION уже предоставлено.")
                    // Здесь вы можете начать слушать шаги или активировать связанные функции
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION) -> {
                    // Пользователь ранее отклонил разрешение.
                    // Объясните, почему это разрешение необходимо, а затем запросите его снова.
                    Log.d("MainActivity", "Пользователь ранее отклонил разрешение. Объясняем...")
                    // В реальном приложении здесь можно показать AlertDialog с объяснением
                    requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
                else -> {
                    // Разрешение еще не было запрошено или было окончательно отклонено (don't ask again)
                    Log.d("MainActivity", "Запрашиваем разрешение ACTIVITY_RECOGNITION.")
                    requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            // Для версий Android ниже Q (API 29) это разрешение не требуется
            Log.d("MainActivity", "Версия Android ниже Q, разрешение ACTIVITY_RECOGNITION не требуется.")
            // Можете инициализировать функции подсчета шагов, если они используют старые методы или не требуют разрешения
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