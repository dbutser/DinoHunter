package com.dinohunters.app.ui.map

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background // <-- Добавлен импорт для .background
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
import androidx.compose.ui.layout.ContentScale // <-- Добавлен импорт
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isHintMenuExpanded by remember { mutableStateOf(false) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            viewModel.initializeMapData()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // --- НАЧАЛО ИЗМЕНЕНИЙ В TOP BAR ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp) // Стандартная высота TopAppBar, можете изменить
                    .background(MaterialTheme.colorScheme.primary) // Цвет фона, если изображение не покрывает всю область
            ) {
                // ВАШ РИСУНОК
                Image(
                    painter = painterResource(id = R.drawable.top_bar_background), // <-- ЗАМЕНИТЕ 'top_bar_background' НА ИМЯ ВАШЕГО ФАЙЛА РИСУНКА
                    contentDescription = "Фон верхней плашки", // Описание для доступности
                    modifier = Modifier.fillMaxSize(), // Рисунок заполняет всю Box
                    contentScale = ContentScale.Crop // Масштабирование: Crop (обрезает, но заполняет), Fit (помещает полностью), FillBounds (растягивает)
                )

                // Иконка профиля справа
                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.align(Alignment.CenterEnd) // Выравнивание иконки справа по центру
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonalInjury,
                        contentDescription = "Профиль",
                        tint = Color.Black // Цвет иконки. Настройте для контраста с вашим рисунком.
                        // Можно использовать MaterialTheme.colorScheme.onPrimary, если подходит
                    )
                }

                // Дополнительно: если вы хотите добавить текст, выровненный по центру, поверх рисунка:
                // Text(
                //     text = "Охотник за динозаврами",
                //     style = MaterialTheme.typography.titleLarge, // Выберите подходящий стиль типографики
                //     color = Color.White, // Цвет текста
                //     modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp) // Выравнивание текста
                // )
            }
            // --- КОНЕЦ ИЗМЕНЕНИЙ В TOP BAR ---
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToInventory) {
                Icon(imageVector = Icons.Default.Collections, contentDescription = "Инвентарь")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (locationPermissionsState.allPermissionsGranted) {
                MapContent(
                    uiState = uiState,
                    isHintMenuExpanded = isHintMenuExpanded,
                    onHintMenuToggle = { isHintMenuExpanded = !isHintMenuExpanded }
                )
            } else {
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
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), 2f)
    }

    val blurRadius by animateDpAsState(
        targetValue = if (isHintMenuExpanded) 8.dp else 0.dp,
        animationSpec = tween(300),
        label = "blurAnimation"
    )

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
            properties = MapProperties(isMyLocationEnabled = true, mapType = MapType.NORMAL),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false),
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

@Composable
private fun PermissionRequestContent(
    modifier: Modifier = Modifier,
    shouldShowRationale: Boolean,
    onGrantPermission: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
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

@Composable
fun ExpandingHintMenu(isExpanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(visible = isExpanded) {
            Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuItem(icon = painterResource(id = R.drawable.ic_hint_highlight), text = "Подсветить кость в зоне", visible = isExpanded)
                MenuItem(icon = painterResource(id = R.drawable.ic_hint_collect), text = "Забрать кость из зоны", visible = isExpanded)
                MenuItem(icon = painterResource(id = R.drawable.ic_hint_scan), text = "Спутниковое сканирование", visible = isExpanded)
            }
        }
        VectorIconFab(imageVector = Icons.Default.Add, isExpanded = isExpanded, onClick = onToggle)
    }
}

@Composable
private fun MenuItem(icon: Painter, text: String, visible: Boolean) {
    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInHorizontally(), exit = fadeOut() + slideOutHorizontally()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CustomIconFab(icon = icon, onClick = { /* TODO: Действие */ })
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), tonalElevation = 4.dp) {
                Text(text = text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CustomIconFab(icon: Painter, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, modifier = Modifier.size(56.dp), containerColor = MaterialTheme.colorScheme.secondary) {
        Image(painter = icon, contentDescription = null, modifier = Modifier.size(56.dp))
    }
}

@Composable
private fun VectorIconFab(imageVector: ImageVector, isExpanded: Boolean, onClick: () -> Unit) {
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f, animationSpec = tween(durationMillis = 300), label = "fabRotation")
    FloatingActionButton(onClick = onClick, modifier = Modifier.size(56.dp), containerColor = MaterialTheme.colorScheme.primary) {
        Icon(imageVector = imageVector, contentDescription = "Меню", modifier = Modifier.rotate(rotationAngle), tint = MaterialTheme.colorScheme.onPrimary)
    }
}