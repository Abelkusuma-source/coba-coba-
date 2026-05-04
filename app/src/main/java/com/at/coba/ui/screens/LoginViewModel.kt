package com.at.coba.ui.screens

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.at.coba.data.DataStoreManager
import com.at.coba.data.network.ApiClient
import com.at.coba.data.network.CookieManager
import com.at.coba.data.network.LoginRequest
import com.at.coba.data.network.OtpRequest
import com.at.coba.data.repository.UserProfileRepository
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

class LoginViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        if (!pendingEmail.isNullOrBlank()) {
            _uiState.value = LoginUiState.Is2FARequired
        }
    }

    private var pendingEmail: String?
        get() = savedStateHandle[KEY_PENDING_EMAIL]
        set(value) {
            if (value.isNullOrBlank()) {
                savedStateHandle.remove<String>(KEY_PENDING_EMAIL)
            } else {
                savedStateHandle[KEY_PENDING_EMAIL] = value
            }
        }

    private var pendingPassword: String?
        get() = savedStateHandle[KEY_PENDING_PASSWORD]
        set(value) {
            if (value.isNullOrBlank()) {
                savedStateHandle.remove<String>(KEY_PENDING_PASSWORD)
            } else {
                savedStateHandle[KEY_PENDING_PASSWORD] = value
            }
        }

    private fun clearPendingCredentials() {
        pendingEmail = null
        pendingPassword = null
    }

    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                // Clear stale auth + unified cookies before a new login attempt
                dataStoreManager.clearAuthData()
                CookieManager.setServerCookiesFromDataStore(null)
                CookieManager.setAuthToken(null)

                val apiService = ApiClient.getApiService(context)
                val response = apiService.login(LoginRequest(email, password))

                clearPendingCredentials()

                // Simpan data ke DataStore + Update Cache Instan untuk Interceptor
                val token = response.data.authtoken
                CookieManager.setAuthToken(token)
                dataStoreManager.setAuthToken(token)
                dataStoreManager.setIs2FAEnabled(response.data.is_2fa_enabled)

                UserProfileRepository.syncAfterLogin(context.applicationContext, response.data)

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
                            pendingEmail = email
                            pendingPassword = password
                            _uiState.value = LoginUiState.Is2FARequired
                            return@launch
                        }
                    } catch (_: Exception) {
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

    fun verifyOtp(context: Context, otpCode: String) {
        viewModelScope.launch {
            val email = pendingEmail
            val password = pendingPassword
            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                _uiState.value = LoginUiState.Error(
                    "Sesi 2FA tidak valid. Silakan masukkan email dan password lagi.",
                )
                return@launch
            }

            _uiState.value = LoginUiState.Loading
            try {
                val apiService = ApiClient.getApiService(context)

                val otpResponse = apiService.validateOtp(OtpRequest(otp = otpCode))

                val twoFaToken = otpResponse.data?.twoFaToken?.takeIf { it.isNotBlank() }
                if (twoFaToken == null) {
                    if (!otpResponse.success) {
                        val msg = otpResponse.errors.firstOrNull()?.let { err ->
                            err.context?.message?.takeIf { it.isNotBlank() } ?: err.code
                        } ?: "Validasi OTP gagal"
                        _uiState.value = LoginUiState.Error(msg)
                    } else {
                        _uiState.value = LoginUiState.Error("Respon OTP tidak berisi token 2FA")
                    }
                    return@launch
                }

                val loginResponse = apiService.login(
                    LoginRequest(
                        email = email,
                        password = password,
                        two_fa_token = twoFaToken,
                    ),
                )

                clearPendingCredentials()

                val token = loginResponse.data.authtoken
                CookieManager.setAuthToken(token)
                dataStoreManager.setAuthToken(token)
                dataStoreManager.setIs2FAEnabled(loginResponse.data.is_2fa_enabled)

                UserProfileRepository.syncAfterLogin(context.applicationContext, loginResponse.data)

                val hasAgreed = dataStoreManager.hasUserAgreed.first()
                _uiState.value = LoginUiState.Success(hasAgreed)
            } catch (e: HttpException) {
                _uiState.value = LoginUiState.Error(
                    "Invalid code. Please check the app and try again",
                )
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    e.message ?: "Invalid code. Please check the app and try again",
                )
            }
        }
    }

    companion object {
        const val KEY_PENDING_EMAIL = "login_pending_email"
        const val KEY_PENDING_PASSWORD = "login_pending_password"
    }
}
