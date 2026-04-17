package com.at.coba.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.at.coba.data.DataStoreManager
import com.at.coba.data.network.ApiClient
import com.at.coba.data.network.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val userAgreed: Boolean) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = ApiClient.apiService.login(LoginRequest(email, password))
                
                // Simpan data ke DataStore
                dataStoreManager.saveAuthToken(response.token)
                dataStoreManager.set2FAEnabled(response.is_2fa_enabled)
                
                // Cek status user agreement
                val hasAgreed = dataStoreManager.hasUserAgreed.first()
                
                _uiState.value = LoginUiState.Success(hasAgreed)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Login failed. Please try again.")
            }
        }
    }

    class Factory(private val dataStoreManager: DataStoreManager) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(dataStoreManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
