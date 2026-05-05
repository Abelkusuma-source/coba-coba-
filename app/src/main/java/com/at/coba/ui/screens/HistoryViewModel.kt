package com.at.coba.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.at.coba.data.DataStoreManager
import com.at.coba.data.local.DatabaseProvider
import com.at.coba.data.local.toEntity
import com.at.coba.data.local.toHistoryItem
import com.at.coba.data.local.toTradeDealEntity
import com.at.coba.data.repository.AssetsRepository
import com.at.coba.data.repository.TradeHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val db = DatabaseProvider.get(application)
    private val dataStore = DataStoreManager(application)

    private val accountFilterFlow = MutableStateFlow("All")

    val items: StateFlow<List<HistoryItem>> = accountFilterFlow
        .flatMapLatest { filter ->
            when (filter) {
                "Real" -> db.tradeDealDao().observeByAccount("Real")
                "Demo" -> db.tradeDealDao().observeByAccount("Demo")
                else -> db.tradeDealDao().observeAll()
            }
        }
        .map { entities -> entities.map { it.toHistoryItem() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Daftar **ric** untuk filter Pair: cache Room (`asset_choices`), sinkron dari `/bo-assets/v6/assets`. */
    val assetPairRics: StateFlow<List<String>> = db.assetChoiceDao().observeRicsOrdered()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyLastSyncedAtEpochMs: StateFlow<Long?> = dataStore.historyLastSyncedAtEpochMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPullRefreshing = MutableStateFlow(false)
    val isPullRefreshing: StateFlow<Boolean> = _isPullRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
            accountFilterFlow.value = accountFilter
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
            onSuccess = { list ->
                db.assetChoiceDao().replaceAll(list.map { it.toEntity() })
            },
            onFailure = { /* Dropdown pakai cache Room jika ada */ },
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
            onSuccess = { list ->
                persistFetched(lastAccountFilter, list)
                dataStore.setHistoryLastSyncedAtEpochMs(System.currentTimeMillis())
            },
            onFailure = { e -> _error.value = e.message ?: e.javaClass.simpleName }
        )
    }

    private suspend fun persistFetched(accountFilter: String, deals: List<HistoryItem>) {
        val dao = db.tradeDealDao()
        val entities = deals.map { it.toTradeDealEntity() }
        when (accountFilter) {
            "Real" -> dao.mergeAccountDeals("Real", entities)
            "Demo" -> dao.mergeAccountDeals("Demo", entities)
            else -> {
                dao.mergeAccountDeals(
                    "Real",
                    entities.filter { it.accountMode == "Real" },
                )
                dao.mergeAccountDeals(
                    "Demo",
                    entities.filter { it.accountMode == "Demo" },
                )
            }
        }
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
