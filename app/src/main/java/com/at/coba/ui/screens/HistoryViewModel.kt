package com.at.coba.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.at.coba.data.repository.TradeHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPullRefreshing = MutableStateFlow(false)
    val isPullRefreshing: StateFlow<Boolean> = _isPullRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Filter akun terakhir (All / Real / Demo) untuk pull-to-refresh. */
    private var lastAccountFilter: String = "All"

    fun load(accountFilter: String) {
        viewModelScope.launch {
            lastAccountFilter = accountFilter
            _isLoading.value = true
            _error.value = null
            executeFetch()
            _isLoading.value = false
        }
    }

    /** Tarik-ke-bawah seperti Profil — memuat ulang data dari API tanpa menutup aplikasi. */
    fun refreshFromPull() {
        viewModelScope.launch {
            _isPullRefreshing.value = true
            _error.value = null
            try {
                executeFetch()
            } finally {
                _isPullRefreshing.value = false
            }
        }
    }

    private suspend fun executeFetch() {
        val ctx = getApplication<Application>()
        val result = when (lastAccountFilter) {
            "Real" -> TradeHistoryRepository.fetchTradeDeals(ctx, "real")
            "Demo" -> TradeHistoryRepository.fetchTradeDeals(ctx, "demo")
            else -> TradeHistoryRepository.fetchAllMerged(ctx)
        }
        result.fold(
            onSuccess = { _items.value = it },
            onFailure = { e -> _error.value = e.message ?: e.javaClass.simpleName }
        )
    }

    fun clearError() {
        _error.value = null
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
