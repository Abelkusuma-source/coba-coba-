package com.at.coba.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TradeDealEntity::class,
        AssetChoiceEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class CobaDatabase : RoomDatabase() {
    abstract fun tradeDealDao(): TradeDealDao
    abstract fun assetChoiceDao(): AssetChoiceDao
}
