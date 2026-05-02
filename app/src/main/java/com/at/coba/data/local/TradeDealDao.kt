package com.at.coba.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDealDao {

    @Query("SELECT * FROM trade_deals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TradeDealEntity>>

    @Query("SELECT * FROM trade_deals WHERE accountMode = :accountMode ORDER BY createdAt DESC")
    fun observeByAccount(accountMode: String): Flow<List<TradeDealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TradeDealEntity>)

    @Query("DELETE FROM trade_deals WHERE accountMode = :accountMode")
    suspend fun deleteByAccount(accountMode: String)

    @Transaction
    suspend fun replaceAccountDeals(accountMode: String, items: List<TradeDealEntity>) {
        deleteByAccount(accountMode)
        if (items.isNotEmpty()) insertAll(items)
    }
}
