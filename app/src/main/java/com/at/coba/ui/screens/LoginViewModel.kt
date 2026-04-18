package com.at.coba.ui.screens

import android.content.Context
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
import org.json.JSONObject
import retrofit2.HttpException

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Is2FARequired : LoginUiState()
    data class Success(val userAgreed: Boolean) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var savedEmail = ""
    private var savedPassword = ""

    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val apiService = ApiClient.getApiService(context)
                val response = apiService.login(LoginRequest(email, password))
                
                // Simpan data ke DataStore
                dataStoreManager.setAuthToken(response.data.authtoken)
                dataStoreManager.setIs2FAEnabled(response.data.is_2fa_enabled)
                
                // Cek status user agreement
                val hasAgreed = dataStoreManager.hasUserAgreed.first()
                
                _uiState.value = LoginUiState.Success(hasAgreed)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                
                if (e.code() == 422 && errorBody != null) {
                    try {
                        val json = JSONObject(errorBody)
                        val errors = json.optJSONArray("errors")
                        val code = errors?.getJSONObject(0)?.optString("code")
                        
                        if (code == "2fa_required") {
                            savedEmail = email
                            savedPassword = password
                            _uiState.value = LoginUiState.Is2FARequired
                            return@launch
                        }
                    } catch (ex: Exception) {
                        // JSON parsing failed, proceed to normal error handling
                    }
                }
                
                val message = if (e.code() == 422) {
                    "Invalid email or password."
                } else {
                    "Login failed (${e.code()}): ${e.message()}"
                }
                _uiState.value = LoginUiState.Error(message)
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Login failed. Please try again.")
            }
        }
    }

    fun verifyOtp(context: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val apiService = ApiClient.getApiService(context)

                // Ambil cookie dari DataStore (COOKIES_KEY)
                val cookieString = dataStoreManager.cookies.first()

                // Parse nilai 2fa_token dari cookie string
                val twoFaToken = cookieString
                    ?.split(";")
                    ?.firstOrNull { it.trim().startsWith("2fa_token=") }
                    ?.trim()
                    ?.removePrefix("2fa_token=")
                    ?.trim()
                
                val response = apiService.login(
                    LoginRequest(
                        email = savedEmail,
                        password = savedPassword,
                        two_fa_token = twoFaToken
                    )
                )

                // Simpan data ke DataStore
                dataStoreManager.setAuthToken(response.data.authtoken)
                dataStoreManager.setIs2FAEnabled(response.data.is_2fa_enabled)

                // Cek status user agreement
                val hasAgreed = dataStoreManager.hasUserAgreed.first()

                _uiState.value = LoginUiState.Success(hasAgreed)
            } catch (e: HttpException) {
                _uiState.value = LoginUiState.Error(
                    "Invalid code. Please check the app and try again"
                )
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    "Invalid code. Please check the app and try again"
                )
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
