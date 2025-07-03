package com.dinohunters.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dinohunters.app.data.model.UserProfile
import com.dinohunters.app.data.repository.DinoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: DinoRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = repository.getUserProfile()
        .map { profile -> ProfileUiState(profile = profile ?: UserProfile()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState()
        )
}

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = false
)