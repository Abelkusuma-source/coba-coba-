package com.at.coba.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.at.coba.MainActivity
import com.at.coba.R
import com.at.coba.data.DataStoreManager
import com.at.coba.data.TradeSignal
import com.at.coba.data.TradingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service agar WebSocket + proses sinyal tetap berjalan saat app di background.
 */
class TradingService : Service() {

    private lateinit var engine: TradingEngine
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private var signalObserveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val app = application
        val dsm = DataStoreManager(app)
        engine = TradingEngine.getInstance(app, dsm)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                ensureChannel()
                val notification = buildNotification(subText = getString(R.string.trading_service_running))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    startForeground(NOTIFICATION_ID, notification)
                }
                engine.startConnection(applicationContext)
                observeSignalForNotification()
            }
            ACTION_STOP -> {
                signalObserveJob?.cancel()
                signalObserveJob = null
                engine.stopConnection()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun observeSignalForNotification() {
        signalObserveJob?.cancel()
        signalObserveJob = serviceScope.launch {
            engine.tradeSignal.collect { signal ->
                val sub = when (signal) {
                    TradeSignal.BUY -> getString(R.string.trading_signal_buy)
                    TradeSignal.SELL -> getString(R.string.trading_signal_sell)
                    TradeSignal.SCANNING -> getString(R.string.trading_signal_scan)
                }
                val nm = getSystemService(NotificationManager::class.java) ?: return@collect
                nm.notify(NOTIFICATION_ID, buildNotification(subText = sub))
            }
        }
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.trading_service_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.trading_service_channel_desc)
        }
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(subText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.trading_service_title))
            .setContentText(subText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        signalObserveJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.at.coba.trading.ACTION_START"
        const val ACTION_STOP = "com.at.coba.trading.ACTION_STOP"
        private const val CHANNEL_ID = "trading_engine"
        private const val NOTIFICATION_ID = 71001

        fun start(context: Context) {
            val i = Intent(context, TradingService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            val i = Intent(context, TradingService::class.java).setAction(ACTION_STOP)
            context.startService(i)
        }
    }
}
