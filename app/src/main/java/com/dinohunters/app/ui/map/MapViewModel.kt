package com.dinohunters.app.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinohunters.app.data.model.BoneZone
import com.dinohunters.app.data.repository.DinoRepository
import com.dinohunters.app.service.LocationService
import com.dinohunters.app.utils.BoneGenerator
import com.dinohunters.app.utils.DistanceUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: DinoRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Флаг, чтобы генерация зон произошла только один раз
    private var zonesInitialized = false

    init {
        // Просто подписываемся на обновление зон из базы данных
        repository.getAllZones()
            .onEach { zones -> _uiState.update { it.copy(boneZones = zones) } }
            .launchIn(viewModelScope)
    }

    fun startLocationUpdates() {
        locationService.getLocationUpdates(5000L) // Обновление каждые 5 секунд
            .onEach { location ->
                // При получении первой валидной локации генерируем зоны
                if (!zonesInitialized) {
                    zonesInitialized = true
                    viewModelScope.launch {
                        repository.initializeBoneZones(location.latitude, location.longitude)
                    }
                }

                _uiState.update { it.copy(currentLocation = location) }
                checkForBoneCollection(location)
            }
            .launchIn(viewModelScope)
    }

    private suspend fun checkForBoneCollection(location: Location) {
        val unCollectedZones = uiState.value.boneZones.filter { !it.isCollected }

        for (zone in unCollectedZones) {
            val distance = DistanceUtil.calculateDistance(
                location.latitude, location.longitude,
                zone.hiddenPointLat, zone.hiddenPointLng
            )

            if (distance <= 25.0) { // 25 метров для сбора
                val newBone = BoneGenerator.generateRandomBone()
                repository.boneFound(newBone, zone.id)
                _snackbarMessage.emit("Найдено: ${newBone.name}!")
                break
            }
        }
    }
}

data class MapUiState(
    val currentLocation: Location? = null,
    val boneZones: List<BoneZone> = emptyList(),
    val isLoading: Boolean = false
)