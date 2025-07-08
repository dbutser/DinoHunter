// Путь: app/src/main/java/com/dinohunters/app/ui/map/MapViewModel.kt

package com.dinohunters.app.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinohunters.app.data.model.BoneZone
import com.dinohunters.app.data.repository.DinoRepository
import com.dinohunters.app.service.LocationService
import com.dinohunters.app.data.repository.DinoCoinRepository
import com.dinohunters.app.utils.BoneGenerator // Предполагается, что это object или класс с DI
import com.dinohunters.app.utils.DistanceUtil // Убедитесь, что этот util внедряется через DI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: DinoRepository,
    private val locationService: LocationService,
    private val distanceUtil: DistanceUtil, // Внедряем DistanceUtil для расчетов
    private val coinRepository: DinoCoinRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            coinRepository.coinBalance.collect { balance ->
                _uiState.update { it.copy(dinoCoins = balance) }
            }
        }
    }

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Переменная для хранения последней обработанной локации, чтобы не делать лишнюю работу
    private var lastCheckedLocation: Location? = null
    // Job для отмены предыдущей проверки, если пришла новая локация
    private var zoneUpdateJob: Job? = null

    // Радиус, в пределах которого можно собрать кость
    private val BONE_COLLECTION_RADIUS_METERS = 25.0

    // init блок теперь пуст. Вся логика запускается после получения первой локации.

    fun startLocationUpdates() {
        // Мы используем distinctUntilChanged, чтобы не реагировать на незначительные изменения локации.
        // Это оптимизация для батареи и производительности.
        locationService.getLocationUpdates(5000L) // Обновление каждые 5 секунд
            .distinctUntilChanged { old, new ->
                // Игнорируем обновление, если игрок сдвинулся меньше чем на 10 метров
                (old.distanceTo(new) < 10.0f)
            }
            .onEach { newLocation ->
                _uiState.update { it.copy(currentLocation = newLocation) }

                // Запускаем основную логику в отдельной корутине
                processNewLocation(newLocation)

                // Проверяем, можно ли собрать кость (это быстрая операция, можно оставить здесь)
                checkForBoneCollection(newLocation)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Основной метод, который запускает проверку и обновление зон.
     */
    private fun processNewLocation(location: Location) {
        // Отменяем предыдущую задачу, если она еще выполняется
        zoneUpdateJob?.cancel()

        zoneUpdateJob = viewModelScope.launch {
            // 1. Устанавливаем флаг загрузки, чтобы показать пользователю индикатор
            _uiState.update { it.copy(isLoading = true) }

            // 2. Проверяем, нужно ли генерировать новые зоны, и если нужно - генерируем
            repository.ensureZonesExist(location)

            // 3. Получаем ВСЕ видимые зоны в "ауре" игрока
            val visibleZones = repository.getVisibleZones(location)

            // 4. Обновляем UI со списком видимых зон и выключаем флаг загрузки
            _uiState.update {
                it.copy(
                    boneZones = visibleZones,
                    isLoading = false
                )
            }

            // Сохраняем локацию, чтобы не делать лишних проверок
            lastCheckedLocation = location
        }
    }

    /**
     * Проверяет, находится ли игрок достаточно близко к скрытой точке, чтобы собрать кость.
     */
    private fun checkForBoneCollection(location: Location) {
        // Ищем первую зону, которую можно собрать
        val zoneToCollect = uiState.value.boneZones.firstOrNull { zone ->
            !zone.isCollected && distanceUtil.calculateDistance(
                location.latitude, location.longitude,
                zone.hiddenPointLat, zone.hiddenPointLng
            ) <= BONE_COLLECTION_RADIUS_METERS
        }

        // Если такая зона найдена, собираем кость и выходим
        if (zoneToCollect != null) {
            viewModelScope.launch {
                val newBone = BoneGenerator.generateRandomBone()
                // Сначала обновляем репозиторий
                repository.boneFound(newBone, zoneToCollect.id)
                // Затем показываем сообщение
                _snackbarMessage.emit("Найдено: ${newBone.name}!")

                // После сбора кости немедленно обновляем зоны на карте,
                // чтобы собранная зона изменила свой вид.
                processNewLocation(location)
            }
        }
    }
}

data class MapUiState(
    val currentLocation: Location? = null,
    val boneZones: List<BoneZone> = emptyList(), // Теперь здесь только видимые зоны
    val isLoading: Boolean = true, // Начинаем с состояния загрузки
    val dinoCoins: Int = 0)