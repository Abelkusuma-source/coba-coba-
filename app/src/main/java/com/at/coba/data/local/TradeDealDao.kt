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

    @Query("SELECT dealId FROM trade_deals WHERE accountMode = :accountMode")
    suspend fun getDealIdsByAccount(accountMode: String): List<Long>

    @Query("SELECT dealId FROM trade_deals")
    suspend fun getAllDealIds(): List<Long>

    @Query("DELETE FROM trade_deals WHERE accountMode = :accountMode")
    suspend fun deleteByAccount(accountMode: String)

    @Transaction
    suspend fun replaceAccountDeals(accountMode: String, items: List<TradeDealEntity>) {
        deleteByAccount(accountMode)
        if (items.isNotEmpty()) insertAll(items)
    }

    /**
     * Insert hanya deal yang belum ada di lokal; deal yang sudah ada di-update via REPLACE.
     * Tidak menghapus deal lokal yang tidak ada di [items] (server mungkin hanya return halaman terbaru).
     */
    @Transaction
    suspend fun mergeAccountDeals(accountMode: String, items: List<TradeDealEntity>) {
        if (items.isEmpty()) return
        val existing = getDealIdsByAccount(accountMode).toSet()
        val newDeals = items.filter { it.dealId !in existing }
        val updatedDeals = items.filter { it.dealId in existing }
        if (newDeals.isNotEmpty()) insertAll(newDeals)
        if (updatedDeals.isNotEmpty()) insertAll(updatedDeals)
    }
}
