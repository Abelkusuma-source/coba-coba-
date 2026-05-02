package com.at.coba.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetChoiceDao {

    /** Urutan sama seperti API: `sortedBy { it.label.lowercase() }` → COLLATE NOCASE. */
    @Query("SELECT ric FROM asset_choices ORDER BY label COLLATE NOCASE ASC")
    fun observeRicsOrdered(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AssetChoiceEntity>)

    @Query("DELETE FROM asset_choices")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(items: List<AssetChoiceEntity>) {
        deleteAll()
        if (items.isNotEmpty()) insertAll(items)
    }
}
