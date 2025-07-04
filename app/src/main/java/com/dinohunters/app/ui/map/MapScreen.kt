// Путь: app/src/main/java/com/dinohunters/app/ui/map/MapScreen.kt

package com.dinohunters.app.ui.map

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dinohunters.app.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateToInventory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isHintMenuExpanded by remember { mutableStateOf(false) }

    // Ваша реализация логики разрешений. Она идеальна.
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Этот LaunchedEffect следит за состоянием разрешений.
    // Если они есть - запускает ViewModel.
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        }
    }

    // [ИЗМЕНЕНО] Этот LaunchedEffect подписывается на сообщения от ViewModel для Snackbar.
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
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
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToInventory) {
                Icon(imageVector = Icons.Default.Backpack, contentDescription = "Инвентарь")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Если разрешения есть - показываем карту. Если нет - экран запроса.
            if (locationPermissionsState.allPermissionsGranted) {
                MapContent(
                    uiState = uiState,
                    isHintMenuExpanded = isHintMenuExpanded,
                    onHintMenuToggle = { isHintMenuExpanded = !isHintMenuExpanded }
                )
            } else {
                // Если разрешения еще не запрашивались или были отклонены
                PermissionRequestContent(
                    modifier = Modifier.fillMaxSize(),
                    shouldShowRationale = locationPermissionsState.shouldShowRationale,
                    onGrantPermission = { locationPermissionsState.launchMultiplePermissionRequest() }
                )
            }
        }
    }
}


@Composable
private fun MapContent(
    uiState: MapUiState,
    isHintMenuExpanded: Boolean,
    onHintMenuToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Начальная позиция (например, Москва), если геолокация еще не определена.
    val defaultLocation = LatLng(55.751244, 37.618423)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    val blurRadius by animateDpAsState(
        targetValue = if (isHintMenuExpanded) 8.dp else 0.dp,
        animationSpec = tween(300),
        label = "blurAnimation"
    )

    // [ИЗМЕНЕНО] Эта корутина плавно перемещает камеру к игроку при обновлении его локации.
    LaunchedEffect(uiState.currentLocation) {
        uiState.currentLocation?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16.5f),
                durationMs = 1500
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurRadius else 0.dp),
            cameraPositionState = cameraPositionState,
            // [ИЗМЕНЕНО] Включаем синюю точку игрока на карте
            properties = MapProperties(isMyLocationEnabled = true, mapType = MapType.NORMAL),
            // [ИЗМЕНЕНО] Включаем стандартную кнопку "Мое местоположение"
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false),
            contentPadding = PaddingValues(bottom = 72.dp, end = 8.dp)
        ) {
            // Рисуем зоны из uiState
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

        // [ИЗМЕНЕНО] Показываем индикатор загрузки, пока ViewModel генерирует/загружает зоны.
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        ExpandingHintMenu(
            isExpanded = isHintMenuExpanded,
            onToggle = onHintMenuToggle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

// Улучшенная версия экрана запроса разрешений.
@Composable
private fun PermissionRequestContent(
    modifier: Modifier = Modifier,
    shouldShowRationale: Boolean,
    onGrantPermission: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp), // Добавляем отступы для красоты
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val text = if (shouldShowRationale) {
            "Без разрешения на геолокацию охота невозможна. Пожалуйста, предоставьте доступ, чтобы находить кости динозавров в реальном мире!"
        } else {
            "Для начала охоты нужно ваше разрешение на доступ к геолокации."
        }
        Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGrantPermission) {
            Text("Дать разрешение")
        }
    }
}


// --- ВСЕ ВАШИ КОМПОНЕНТЫ МЕНЮ ОСТАЮТСЯ БЕЗ ИЗМЕНЕНИЙ ---

@Composable
fun ExpandingHintMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(visible = isExpanded) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MenuItem(icon = painterResource(id = R.drawable.ic_hint_highlight), text = "Подсветить кость в зоне", visible = isExpanded)
                MenuItem(icon = painterResource(id = R.drawable.ic_hint_collect), text = "Забрать кость из зоны", visible = isExpanded)
                MenuItem(icon = painterResource(id = R.drawable.ic_hint_scan), text = "Спутниковое сканирование", visible = isExpanded)
            }
        }

        VectorIconFab(
            imageVector = Icons.Default.Add,
            isExpanded = isExpanded,
            onClick = onToggle
        )
    }
}

@Composable
private fun MenuItem(
    icon: Painter,
    text: String,
    visible: Boolean,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(),
        exit = fadeOut() + slideOutHorizontally()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CustomIconFab(icon = icon, onClick = { /* TODO: Действие */ })
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 4.dp
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CustomIconFab(icon: Painter, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        containerColor = MaterialTheme.colorScheme.secondary
    ) {
        Image(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
private fun VectorIconFab(
    imageVector: ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "fabRotation"
    )
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = "Меню",
            modifier = Modifier.rotate(rotationAngle),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}