package com.at.coba.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.at.coba.data.repository.AssetsRepository
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

    /** Nilai **ric** dari `/bo-assets/v6/assets` untuk filter Pair (diselaraskan dengan `asset_ric` deals). */
    private val _assetPairRics = MutableStateFlow<List<String>>(emptyList())
    val assetPairRics: StateFlow<List<String>> = _assetPairRics.asStateFlow()

    /** Filter akun terakhir (All / Real / Demo) untuk pull-to-refresh. */
    private var lastAccountFilter: String = "All"

    init {
        viewModelScope.launch {
            loadAssetPairs()
        }
    }

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
                loadAssetPairs()
                executeFetch()
            } finally {
                _isPullRefreshing.value = false
            }
        }
    }

    private suspend fun loadAssetPairs() {
        val ctx = getApplication<Application>()
        AssetsRepository.fetchChoices(ctx).fold(
            onSuccess = { list -> _assetPairRics.value = list.map { it.ric } },
            onFailure = { /* Dropdown tetap "All"; daftar dapat diisi lagi saat tarik refresh */ }
        )
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
