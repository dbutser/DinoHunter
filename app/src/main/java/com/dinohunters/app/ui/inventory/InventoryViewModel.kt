// com.dinohunters.app.ui.inventory.InventoryViewModel.kt
package com.dinohunters.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinohunters.app.data.model.Bone
import com.dinohunters.app.data.model.BoneKey
import com.dinohunters.app.data.repository.DinoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch // <-- ДОБАВИТЬ ЭТОТ ИМПОРТ
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: DinoRepository
) : ViewModel() {

    // Класс состояния UI
    data class InventoryUiState(
        val groupedBones: Map<BoneKey, Int> = emptyMap(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )

    // Этот StateFlow будет отражать текущее состояние инвентаря
    val uiState: StateFlow<InventoryUiState> = repository.getAllBones()
        .map { bonesList ->
            val groupedBonesMap = bonesList
                .groupBy { BoneKey.fromBone(it) }
                .mapValues { it.value.size }

            InventoryUiState(groupedBones = groupedBonesMap, isLoading = false)
        }
        .onStart { emit(InventoryUiState(isLoading = true)) }
        .catch { e -> emit(InventoryUiState(errorMessage = "Ошибка загрузки инвентаря: ${e.message}")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventoryUiState(isLoading = true)
        )

    // Если вам нужна функция для добавления кости в репозиторий, она может быть здесь:
    fun addBone(bone: Bone) {
        viewModelScope.launch { // <-- Теперь 'launch' будет распознан
            repository.addBone(bone) // <-- Эта строка вызовет ошибку, если addBone в репозитории private
        }
    }

    // Если нужна функция для удаления одной кости из группы (это сложнее)
    fun removeOneBone(boneKey: BoneKey) {
        viewModelScope.launch { // <-- Теперь 'launch' будет распознан
            // ... (комментарий) ...
        }
    }
}

// data class InventoryUiState остается без изменений
data class InventoryUiState(
    val groupedBones: Map<BoneKey, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)