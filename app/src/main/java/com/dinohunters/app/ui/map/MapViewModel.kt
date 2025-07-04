// Путь: app/src/main/java/com/dinohunters/app/ui/map/MapViewModel.kt

package com.dinohunters.app.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinohunters.app.data.model.Bone
import com.dinohunters.app.data.model.BoneZone
import com.dinohunters.app.data.repository.DinoRepository
import com.dinohunters.app.service.LocationService
import com.dinohunters.app.utils.BoneGenerator
import com.dinohunters.app.utils.DistanceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: DinoRepository,
    private val locationService: LocationService,
    private val distanceUtil: DistanceUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var zoneUpdateJob: Job? = null
    private val BONE_COLLECTION_RADIUS_METERS = 25.0

    // Флаг, чтобы избежать повторной инициализации
    private var isInitialized = false

    /**
     * [НОВЫЙ ПУБЛИЧНЫЙ МЕТОД]
     * Эта функция должна вызываться из UI ОДИН РАЗ, когда получены разрешения.
     */
    fun initializeMapData() {
        if (isInitialized) return // Защита от повторного вызова
        isInitialized = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Пытаемся быстро получить последнюю известную локацию
            val lastKnownLocation = locationService.getLastKnownLocation()
            if (lastKnownLocation != null) {
                // Если получилось, сразу обновляем UI и начинаем генерировать зоны
                _uiState.update { it.copy(currentLocation = lastKnownLocation) }
                processNewLocation(lastKnownLocation)
            }

            // 2. В любом случае, запускаем постоянное отслеживание для получения свежих данных
            startContinuousLocationUpdates()
        }
    }

    /**
     * [ИЗМЕНЕНО] Эта функция теперь приватная и запускает постоянное отслеживание.
     */
    private fun startContinuousLocationUpdates() {
        locationService.getLocationUpdates(5000L) // Обновление каждые 5 секунд
            .distinctUntilChanged { old, new ->
                (old != null && new != null && old.distanceTo(new) < 10.0f)
            }
            .onEach { newLocation ->
                _uiState.update { it.copy(currentLocation = newLocation) }
                processNewLocation(newLocation)
                checkForBoneCollection(newLocation)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Основной метод, который запускает проверку и обновление зон.
     * (Без изменений)
     */
    private fun processNewLocation(location: Location) {
        zoneUpdateJob?.cancel()
        zoneUpdateJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.ensureZonesExist(location)
            val visibleZones = repository.getVisibleZones(location)
            _uiState.update {
                it.copy(
                    boneZones = visibleZones,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Проверяет, находится ли игрок достаточно близко к скрытой точке, чтобы собрать кость.
     * (Без изменений)
     */
    private fun checkForBoneCollection(location: Location) {
        val zoneToCollect = uiState.value.boneZones.firstOrNull { zone ->
            !zone.isCollected && distanceUtil.calculateDistance(
                location.latitude, location.longitude,
                zone.hiddenPointLat, zone.hiddenPointLng
            ) <= BONE_COLLECTION_RADIUS_METERS
        }

        if (zoneToCollect != null) {
            viewModelScope.launch {
                val newBone = BoneGenerator.generateRandomBone()
                repository.boneFound(newBone, zoneToCollect.id)
                _snackbarMessage.emit("Найдено: ${newBone.name}!")
                processNewLocation(location)
            }
        }
    }
}

data class MapUiState(
    val currentLocation: Location? = null,
    val boneZones: List<BoneZone> = emptyList(),
    val isLoading: Boolean = true
)