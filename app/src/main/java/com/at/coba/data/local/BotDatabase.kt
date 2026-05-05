package com.at.coba.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BotDealEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class BotDatabase : RoomDatabase() {
    abstract fun botDealDao(): BotDealDao
}
