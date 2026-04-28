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
import com.at.coba.data.repository.ProfileFetchResult
import com.at.coba.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

    val profileRemoteAvatarUrl: StateFlow<String?> = dataStoreManager.profileRemoteAvatarUrl
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /** File content:// / unduhan lokal atau https; jika kosong pakai remote URL dari API. */
    val avatarDisplayUrl: StateFlow<String?> = combine(
        profileImageUri,
        profileRemoteAvatarUrl
    ) { fileOrUrl, remote ->
        when {
            !fileOrUrl.isNullOrBlank() -> fileOrUrl
            !remote.isNullOrBlank() -> remote
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
        viewModelScope.launch {
            // Cek apakah data profil sudah ada di memori
            val email = dataStoreManager.userEmail.first()
            if (email.isNullOrBlank()) {
                _uiState.value = ProfileUiState.Loading // Tampilkan Skeleton
            }

            // Selalu tarik data terbaru dari server
            refreshProfile()
        }
    }

    fun retryInitialLoad() {
        viewModelScope.launch {
            if (hasCachedProfile()) return@launch
            _uiState.value = ProfileUiState.Loading
            when (
                val result = UserProfileRepository.fetchAndSyncFullProfile(getApplication())
            ) {
                is ProfileFetchResult.Success -> _uiState.value = ProfileUiState.Idle
                is ProfileFetchResult.Failure -> {
                    val shown = buildUserFacingError(
                        result.httpCode,
                        result.message ?: "Gagal memuat profil"
                    )
                    _uiState.value = ProfileUiState.LoadError(shown)
                }
            }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch {
            when (val result = UserProfileRepository.fetchAndSyncFullProfile(getApplication())) {
                is ProfileFetchResult.Failure -> {
                    val shown = buildUserFacingError(
                        result.httpCode,
                        result.message ?: "Gagal memperbarui profil"
                    )
                    if (_uiState.value == ProfileUiState.Loading) {
                        _uiState.value = ProfileUiState.LoadError(shown)
                    } else {
                        _message.emit(shown)
                    }
                }
                is ProfileFetchResult.Success -> {
                    _uiState.value = ProfileUiState.Idle
                }
            }
        }
    }

    fun updatePhone(newPhone: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Submitting
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
            _uiState.value = ProfileUiState.Submitting
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

    private suspend fun hasCachedProfile(): Boolean {
        val email = dataStoreManager.userEmail.first()
        return !email.isNullOrBlank()
    }

    private fun buildUserFacingError(httpCode: Int?, raw: String): String {
        if (httpCode == 401 || httpCode == 403) {
            return "Sesi kedaluwarsa atau tidak memiliki akses. Silakan login lagi."
        }
        return if (raw.length > 200) "${raw.take(197)}..." else raw
    }

    sealed class ProfileUiState {
        object Idle : ProfileUiState()
        /** Skeleton pertama kali halaman dibuka tanpa cache. */
        object Loading : ProfileUiState()
        /** Kirim formulir nama/telepon tanpa skeleton full screen. */
        object Submitting : ProfileUiState()
        data class LoadError(val message: String) : ProfileUiState()
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
