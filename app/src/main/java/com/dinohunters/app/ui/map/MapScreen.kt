package com.dinohunters.app.ui.map

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dinohunters.app.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.top_bar_background),
                    contentDescription = "Фон верхней плашки",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Профиль",
                        tint = Color.Black
                    )
                }
            }
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
                    onHintMenuToggle = { isHintMenuExpanded = !isHintMenuExpanded },
                    onHighlightHintClick = { viewModel.onHighlightHintClicked() },
                    onRemoteCollectHintClick = { viewModel.onRemoteCollectHintClicked() },
                    onSatelliteScanHintClick = { viewModel.onSatelliteScanHintClicked() }
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
    onHighlightHintClick: () -> Unit,
    onRemoteCollectHintClick: () -> Unit,
    onSatelliteScanHintClick: () -> Unit,
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
            if (cameraPositionState.position.zoom < 15f) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16.5f),
                    durationMs = 1500
                )
            }
        }
    }

    // [НОВОЕ] Порог видимости для текста и текущий уровень зума
    val TEXT_VISIBILITY_ZOOM_LEVEL = 16.0f
    val currentZoom = cameraPositionState.position.zoom

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

                // [ИЗМЕНЕНО] Условное отображение в зависимости от зума
                uiState.satelliteScanResults[zone.id]?.let { bone ->
                    if (currentZoom >= TEXT_VISIBILITY_ZOOM_LEVEL) {
                        // Показываем текст при сильном приближении
                        MarkerComposable(
                            keys = arrayOf(zone.id),
                            state = rememberMarkerState(position = LatLng(zone.centerLat, zone.centerLng)),
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shadowElevation = 4.dp,
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Text(
                                    text = bone.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        // Показываем синий маркер при отдалении
                        Marker(
                            state = MarkerState(position = LatLng(zone.centerLat, zone.centerLng)),
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            title = "Неопознанная кость" // Название для стандартного InfoWindow по клику
                        )
                    }
                }
            }

            val highlightedZone = uiState.boneZones.find { it.id == uiState.highlightedZoneId }
            highlightedZone?.let { zone ->
                Marker(
                    state = MarkerState(position = LatLng(zone.hiddenPointLat, zone.hiddenPointLng)),
                    title = "Здесь спрятана кость!",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                )
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        ExpandingHintMenu(
            isExpanded = isHintMenuExpanded,
            onToggle = onHintMenuToggle,
            onHighlightHintClick = onHighlightHintClick,
            onRemoteCollectHintClick = onRemoteCollectHintClick,
            onSatelliteScanHintClick = onSatelliteScanHintClick,
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
fun ExpandingHintMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onHighlightHintClick: () -> Unit,
    onRemoteCollectHintClick: () -> Unit,
    onSatelliteScanHintClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(visible = isExpanded) {
            Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MenuItem(
                    icon = painterResource(id = R.drawable.ic_hint_highlight),
                    text = "Подсветить кость (1000)",
                    visible = isExpanded,
                    onClick = onHighlightHintClick
                )
                MenuItem(
                    icon = painterResource(id = R.drawable.ic_hint_collect),
                    text = "Удаленный сбор (2500)",
                    visible = isExpanded,
                    onClick = onRemoteCollectHintClick
                )
                MenuItem(
                    icon = painterResource(id = R.drawable.ic_hint_scan),
                    text = "Сканирование зон (1000)",
                    visible = isExpanded,
                    onClick = onSatelliteScanHintClick
                )
            }
        }
        VectorIconFab(imageVector = Icons.Default.Add, isExpanded = isExpanded, onClick = onToggle)
    }
}

@Composable
private fun MenuItem(
    icon: Painter,
    text: String,
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInHorizontally(), exit = fadeOut() + slideOutHorizontally()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CustomIconFab(icon = icon, onClick = onClick)
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
