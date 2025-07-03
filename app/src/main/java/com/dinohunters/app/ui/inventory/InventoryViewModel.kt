package com.dinohunters.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinohunters.app.data.model.Bone
import com.dinohunters.app.data.repository.DinoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: DinoRepository
) : ViewModel() {

    val uiState: StateFlow<InventoryUiState> = repository.getAllBones()
        .map { InventoryUiState(bones = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InventoryUiState()
        )
}

data class InventoryUiState(
    val bones: List<Bone> = emptyList(),
    val isLoading: Boolean = false
)