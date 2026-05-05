package com.at.coba.data.local

import android.content.Context
import androidx.room.Room

object BotDatabaseProvider {

    @Volatile
    private var instance: BotDatabase? = null

    fun get(context: Context): BotDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BotDatabase::class.java,
                "bot.db",
            ).build().also { instance = it }
        }
    }
}
