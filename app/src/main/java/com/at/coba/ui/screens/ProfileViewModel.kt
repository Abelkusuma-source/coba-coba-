package com.at.coba.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.at.coba.data.DataStoreManager
import com.at.coba.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val dataStoreManager: DataStoreManager) : ViewModel() {

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

    fun onImageSelected(uri: Uri?) {
        viewModelScope.launch {
            dataStoreManager.persistProfileImageFromPicker(uri)
        }
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch {
            dataStoreManager.setThemeMode(mode)
        }
    }

    class Factory(private val dataStoreManager: DataStoreManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(dataStoreManager) as T
        }
    }
}
