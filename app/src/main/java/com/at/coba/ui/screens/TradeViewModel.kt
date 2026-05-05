package com.at.coba.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.at.coba.data.DataStoreManager
import com.at.coba.data.TradingConfig
import com.at.coba.data.TradingEngine
import com.at.coba.data.TradingStrategy
import com.at.coba.data.local.DatabaseProvider
import com.at.coba.data.local.toAssetChoice
import com.at.coba.data.local.toEntity
import com.at.coba.data.network.CookieManager
import com.at.coba.data.repository.AssetChoice
import com.at.coba.data.repository.AssetsRepository
import com.at.coba.data.network.BoCreateDealResult
import com.at.coba.service.TradingService
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TradeViewModel(
    private val application: Application,
    private val engine: TradingEngine,
    private val dataStoreManager: DataStoreManager,
) : ViewModel() {

    private val db = DatabaseProvider.get(application)

    val assetChoices: StateFlow<List<AssetChoice>> = db.assetChoiceDao().observeChoicesOrdered()
        .map { entities -> entities.map { it.toAssetChoice() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedAsset: StateFlow<AssetChoice?> = engine.selectedAsset
    val selectedTimeframe: StateFlow<Int> = engine.selectedTimeframe
    val tradeSignal = engine.tradeSignal
    val indicatorState = engine.indicatorState
    val tradingConfig: StateFlow<TradingConfig> = engine.tradingConfig
    val candleHistory = engine.candleHistory

    val wsStatus = engine.webSocketManager.connectionStatus
    val asStatus = engine.assetSocketManager.connectionStatus
    val wsReceivedMessage = engine.webSocketManager.receivedMessage
    val boCreateResults: SharedFlow<BoCreateDealResult> = engine.webSocketManager.boCreateResults
    val asReceivedMessage = engine.assetSocketManager.receivedMessage
    val tickData = engine.assetSocketManager.tickData

    private val _assetsLoadError = MutableStateFlow<String?>(null)
    val assetsLoadError: StateFlow<String?> = _assetsLoadError.asStateFlow()

    private val _isAssetsLoading = MutableStateFlow(false)
    val isAssetsLoading: StateFlow<Boolean> = _isAssetsLoading.asStateFlow()

    init {
        viewModelScope.launch {
            assetChoices.collect { engine.syncSelectionWithChoices(it) }
        }
        viewModelScope.launch {
            loadAssetChoices()
        }
    }

    suspend fun loadAssetChoices() {
        _isAssetsLoading.value = true
        _assetsLoadError.value = null
        AssetsRepository.fetchChoices(application).fold(
            onSuccess = { list ->
                db.assetChoiceDao().replaceAll(list.map { it.toEntity() })
                _assetsLoadError.value = null
            },
            onFailure = { e ->
                val cached = db.assetChoiceDao().count() > 0
                if (!cached) {
                    _assetsLoadError.value = e.message ?: e.javaClass.simpleName
                }
            },
        )
        _isAssetsLoading.value = false
    }

    fun retryLoadAssetChoices() {
        viewModelScope.launch { loadAssetChoices() }
    }

    fun selectAssetPair(choice: AssetChoice) {
        engine.selectAssetPair(choice)
    }

    fun setTradingStrategy(strategy: TradingStrategy) {
        engine.setTradingStrategy(strategy)
    }

    fun setTimeframe(seconds: Int) {
        changeTimeframe(seconds)
    }

    fun changeTimeframe(seconds: Int) {
        engine.changeTimeframe(seconds)
    }

    fun updateConfig(newConfig: TradingConfig) {
        engine.updateConfig(newConfig)
    }

    fun sendBoTurboDeal(
        trend: String,
        amountDisplay: Double,
        durationSeconds: Int,
        dealType: String,
    ): Boolean = engine.sendBoTurboDeal(
        trend = trend,
        amountDisplay = amountDisplay,
        durationSeconds = durationSeconds,
        dealType = dealType,
    )

    fun startConnection(context: Context) {
        TradingService.start(context.applicationContext)
    }

    fun stopConnection(context: Context) {
        TradingService.stop(context.applicationContext)
    }

    fun performLogout() {
        viewModelScope.launch {
            TradingService.stop(application)
            engine.stopConnection()
            engine.clearTradingState()
            dataStoreManager.clearAuthData()
            CookieManager.setServerCookiesFromDataStore(null)
            CookieManager.setAuthToken(null)
        }
    }

    /**
     * Koneksi dipegang [TradingService] / [TradingEngine]; jangan putus saat layar ditinggalkan.
     */
    override fun onCleared() {
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val dataStoreManager: DataStoreManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TradeViewModel::class.java)) {
                val engine = TradingEngine.getInstance(application, dataStoreManager)
                @Suppress("UNCHECKED_CAST")
                return TradeViewModel(application, engine, dataStoreManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
