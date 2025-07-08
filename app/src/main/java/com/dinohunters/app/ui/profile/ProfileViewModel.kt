package com.dinohunters.app.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinohunters.app.data.model.UserProfile
import com.dinohunters.app.data.repository.DinoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: DinoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getUserProfile().collect { profile ->
                _uiState.update { it.copy(profile = profile ?: UserProfile()) }
            }
        }
    }

    fun syncDinoCoins() {
        viewModelScope.launch {
            try {
                repository.syncDinoCoins()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to sync DinoCoins", e)
            }
        }
    }

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            syncDinoCoins()
        }
    }
}

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = false
)