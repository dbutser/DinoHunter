package com.dinohunters.app.ui.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToInventory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Охотник за динозаврами") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Профиль")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToInventory) {
                Icon(imageVector = Icons.Default.Backpack, contentDescription = "Инвентарь")
            }
        }
    ) { innerPadding ->
        // Применяем отступы от Scaffold к контейнеру, в котором лежит карта
        Box(modifier = Modifier.padding(innerPadding)) {
            if (locationPermissionsState.allPermissionsGranted) {
                MapContent(uiState = uiState)
            } else {
                PermissionRequestScreen(
                    onGrantPermission = { locationPermissionsState.launchMultiplePermissionRequest() }
                )
            }
        }
    }
}

@Composable
private fun MapContent(uiState: MapUiState, modifier: Modifier = Modifier) {
    val defaultLocation = LatLng(37.38, -122.08) // Штаб-квартира Google для эмулятора
    val currentLocation = uiState.currentLocation?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 16f)
    }

    // Плавное перемещение камеры к текущему местоположению
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLng(
                    LatLng(it.latitude, it.longitude)
                )
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = true, mapType = MapType.NORMAL),
        // Вот ключевое исправление: добавляем внутренний отступ для UI элементов Google Карт,
        // чтобы они сдвинулись вверх и не мешали нашей кнопке.
        contentPadding = PaddingValues(bottom = 72.dp, end = 8.dp)
    ) {
        uiState.boneZones.forEach { zone ->
            Circle(
                center = LatLng(zone.centerLat, zone.centerLng),
                radius = zone.radius,
                fillColor = if (zone.isCollected) Color.Gray.copy(alpha = 0.2f) else Color.Green.copy(alpha = 0.2f),
                strokeColor = if (zone.isCollected) Color.Gray else Color.Green,
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun PermissionRequestScreen(modifier: Modifier = Modifier, onGrantPermission: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = {
            Text("Для охоты нужно разрешение на геолокацию.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGrantPermission) {
                Text("Дать разрешение")
            }
        }
    )
}