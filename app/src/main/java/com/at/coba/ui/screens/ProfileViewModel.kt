package com.at.coba.ui.screens

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.at.coba.data.DataStoreManager
import com.at.coba.data.ThemeMode
import com.at.coba.data.repository.ProfileFetchResult
import com.at.coba.data.repository.UserProfileRepository
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProfileViewModel(
    application: Application,
    private val dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    private val _isPullRefreshing = MutableStateFlow(false)
    val isPullRefreshing: StateFlow<Boolean> = _isPullRefreshing.asStateFlow()

    private val fetchMutex = Mutex()
    private var lastSuccessfulFetchElapsedMs = 0L

    companion object {
        /** Debounce sebelum throttle — hindari spam saat event resume berdekatan */
        private const val RESUME_DEBOUNCE_MS = 400L

        /** Jangan fetch lagi jika berhasil baru saja dalam jendela ini (refresh diam saat tab bolak-balik cepat). */
        private const val MIN_SUCCESS_INTERVAL_MS = 15_000L
    }

    val themeMode: StateFlow<ThemeMode> = dataStoreManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM_DEFAULT
        )

    val profileImageUri: StateFlow<String?> = dataStoreManager.profileImageUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profileRemoteAvatarUrl: StateFlow<String?> = dataStoreManager.profileRemoteAvatarUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Untuk Coil memory/disk cache key — naik tiap avatar di-refresh server. */
    val profileAvatarEpoch: StateFlow<Long> = dataStoreManager.profileAvatarEpoch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val avatarDisplayUrl: StateFlow<String?> =
        combine(profileImageUri, profileRemoteAvatarUrl) { local, remote ->
            when {
                !remote.isNullOrBlank() -> remote
                !local.isNullOrBlank() -> local
                else -> null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userEmail: StateFlow<String?> =
        dataStoreManager.userEmail.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPhone: StateFlow<String?> =
        dataStoreManager.userPhone.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userNickname: StateFlow<String?> =
        dataStoreManager.userNickname.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isEmailVerified: StateFlow<Boolean> =
        dataStoreManager.isEmailVerified.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPhoneVerified: StateFlow<Boolean> =
        dataStoreManager.isPhoneVerified.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDocsVerified: StateFlow<Boolean> =
        dataStoreManager.isDocsVerified.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userFirstName: StateFlow<String?> =
        dataStoreManager.userFirstName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userLastName: StateFlow<String?> =
        dataStoreManager.userLastName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userGenderRaw: StateFlow<String?> =
        dataStoreManager.userGenderRaw.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userBirthdayIso: StateFlow<String?> =
        dataStoreManager.userBirthdayIso.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            val email = dataStoreManager.userEmail.first()
            if (email.isNullOrBlank()) {
                _uiState.value = ProfileUiState.Loading
            }
            runProfileFetch(forceRefresh = true)
        }
    }

    /**
     * Dipanggil saat layar Profil dapat [Lifecycle.Event.ON_RESUME] (mis. kembali dari tab lain).
     */
    fun onProfileScreenResumed() {
        viewModelScope.launch {
            runProfileFetch(forceRefresh = false)
        }
    }

    /** Pull-to-refresh manual — selalu memaksa fetch baru. */
    fun refreshProfileFromPull() {
        viewModelScope.launch {
            _isPullRefreshing.value = true
            try {
                runProfileFetch(forceRefresh = true)
            } finally {
                _isPullRefreshing.value = false
            }
        }
    }

    /**
     * @param forceRefresh true untuk init/login/pull; false untuk throttle + debounce (resume tab).
     */
    private suspend fun runProfileFetch(forceRefresh: Boolean) {
        fetchMutex.withLock {
            if (!forceRefresh) {
                delay(RESUME_DEBOUNCE_MS)
                val now = SystemClock.elapsedRealtime()
                if (lastSuccessfulFetchElapsedMs > 0L &&
                    now - lastSuccessfulFetchElapsedMs < MIN_SUCCESS_INTERVAL_MS
                ) {
                    return@withLock
                }
            }

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
                    lastSuccessfulFetchElapsedMs = SystemClock.elapsedRealtime()
                    applySuccessUi(result)
                }
            }
        }
    }

    fun retryInitialLoad() {
        viewModelScope.launch {
            if (hasCachedProfile()) return@launch
            _uiState.value = ProfileUiState.Loading
            when (
                val result = UserProfileRepository.fetchAndSyncFullProfile(getApplication())
            ) {
                is ProfileFetchResult.Success -> {
                    lastSuccessfulFetchElapsedMs = SystemClock.elapsedRealtime()
                    applySuccessUi(result)
                }
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

    private suspend fun applySuccessUi(result: ProfileFetchResult.Success) {
        _uiState.value = ProfileUiState.Idle
        if (result.avatarLocalCacheFailed) {
            _message.emit("Foto tidak tersimpan secara lokal. Menampilkan dari server.")
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
        object Loading : ProfileUiState()
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
