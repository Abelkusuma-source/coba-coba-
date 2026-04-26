package com.at.coba.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.at.coba.data.DataStoreManager
import com.at.coba.data.ThemeMode
import com.at.coba.data.network.ApiClient
import com.at.coba.data.network.UpdateProfileRequest
import com.at.coba.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    private val dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    val themeMode: StateFlow<ThemeMode> = dataStoreManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM_DEFAULT
        )

    val profileImageUri: StateFlow<String?> = dataStoreManager.profileImageUri
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userEmail: StateFlow<String?> = dataStoreManager.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPhone: StateFlow<String?> = dataStoreManager.userPhone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userNickname: StateFlow<String?> = dataStoreManager.userNickname
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isEmailVerified: StateFlow<Boolean> = dataStoreManager.isEmailVerified
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPhoneVerified: StateFlow<Boolean> = dataStoreManager.isPhoneVerified
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDocsVerified: StateFlow<Boolean> = dataStoreManager.isDocsVerified
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        refreshProfile()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            UserProfileRepository.fetchAndSyncFullProfile(getApplication())
        }
    }

    fun updatePhone(newPhone: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val apiService = ApiClient.getApiService(getApplication())
                apiService.updateProfile(UpdateProfileRequest(phone = newPhone))
                dataStoreManager.setUserProfileInfo(null, newPhone)
                _message.emit("Phone number updated successfully")
                _uiState.value = ProfileUiState.Idle
            } catch (e: Exception) {
                _message.emit("Update failed: ${e.message}")
                _uiState.value = ProfileUiState.Idle
            }
        }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val apiService = ApiClient.getApiService(getApplication())
                apiService.updateProfile(UpdateProfileRequest(nickname = nickname))
                dataStoreManager.setUserProfileInfo(null, null, nickname = nickname)
                _message.emit("Nickname updated successfully")
                _uiState.value = ProfileUiState.Idle
            } catch (e: Exception) {
                _message.emit("Update failed: ${e.message}")
                _uiState.value = ProfileUiState.Idle
            }
        }
    }


    fun onImageSelected(uri: Uri?) {
        viewModelScope.launch {
            dataStoreManager.persistProfileImageFromPicker(uri)
            if (uri != null) {
                UserProfileRepository.uploadCachedProfileAvatar(getApplication())
            }
        }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(mode)
        }
    }

    sealed class ProfileUiState {
        object Idle : ProfileUiState()
        object Loading : ProfileUiState()
    }

    class Factory(
        private val application: Application,
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(application, dataStoreManager) as T
        }
    }
}
