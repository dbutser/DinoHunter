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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val currentLocation: Location? = null,
    val boneZones: List<BoneZone> = emptyList(),
    val isLoading: Boolean = true,
    val highlightedZoneId: String? = null
)

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

    private var isInitialized = false

    fun initializeMapData() {
        if (isInitialized) return
        isInitialized = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val lastKnownLocation = locationService.getLastKnownLocation()
            if (lastKnownLocation != null) {
                _uiState.update { it.copy(currentLocation = lastKnownLocation) }
                processNewLocation(lastKnownLocation)
            }

            startContinuousLocationUpdates()
        }
    }

    fun onHighlightHintClicked() {
        viewModelScope.launch {
            val playerLocation = _uiState.value.currentLocation
            if (playerLocation == null) {
                _snackbarMessage.emit("Не удалось определить вашу позицию.")
                return@launch
            }

            val currentZone = _uiState.value.boneZones.firstOrNull { zone ->
                !zone.isCollected && distanceUtil.calculateDistance(
                    playerLocation.latitude, playerLocation.longitude,
                    zone.centerLat, zone.centerLng
                ) <= zone.radius
            }

            if (currentZone == null) {
                _snackbarMessage.emit("Вы должны находиться в несобранной зоне раскопок.")
                return@launch
            }

            if (_uiState.value.highlightedZoneId == currentZone.id) {
                _snackbarMessage.emit("Эта зона уже подсвечена!")
                return@launch
            }

            val purchaseSuccess = repository.purchaseHighlightHint()

            if (purchaseSuccess) {
                _uiState.update { it.copy(highlightedZoneId = currentZone.id) }
                _snackbarMessage.emit("Подсказка куплена! Точное местоположение кости отмечено.")
            } else {
                _snackbarMessage.emit("Недостаточно динокоинов! Нужно 1000.")
            }
        }
    }

    /**
     * [НОВЫЙ МЕТОД] Обрабатывает нажатие на кнопку подсказки "Удаленный сбор".
     */
    fun onRemoteCollectHintClicked() {
        viewModelScope.launch {
            val playerLocation = _uiState.value.currentLocation
            if (playerLocation == null) {
                _snackbarMessage.emit("Не удалось определить вашу позицию.")
                return@launch
            }

            // Ищем несобранную зону, в которой находится игрок
            val currentZone = _uiState.value.boneZones.firstOrNull { zone ->
                !zone.isCollected && distanceUtil.calculateDistance(
                    playerLocation.latitude, playerLocation.longitude,
                    zone.centerLat, zone.centerLng
                ) <= zone.radius
            }

            if (currentZone == null) {
                _snackbarMessage.emit("Вы должны находиться в несобранной зоне раскопок.")
                return@launch
            }

            // Пытаемся купить подсказку
            val purchaseSuccess = repository.purchaseRemoteCollectHint()

            if (purchaseSuccess) {
                // Если покупка удалась, генерируем кость и сразу же "находим" ее
                val newBone = BoneGenerator.generateRandomBone()
                repository.boneFound(newBone, currentZone.id)
                _snackbarMessage.emit("Удаленный сбор успешен! Найдено: ${newBone.name}!")

                // Если собранная зона была подсвечена, убираем подсветку
                if (currentZone.id == _uiState.value.highlightedZoneId) {
                    _uiState.update { it.copy(highlightedZoneId = null) }
                }

                // Обновляем состояние карты, чтобы убрать зону
                processNewLocation(playerLocation)
            } else {
                // Если не хватило денег
                _snackbarMessage.emit("Недостаточно динокоинов! Нужно 2500.")
            }
        }
    }

    private fun startContinuousLocationUpdates() {
        locationService.getLocationUpdates(5000L)
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

                if (zoneToCollect.id == _uiState.value.highlightedZoneId) {
                    _uiState.update { it.copy(highlightedZoneId = null) }
                }

                processNewLocation(location)
            }
        }
    }
}