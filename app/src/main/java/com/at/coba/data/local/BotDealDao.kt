package com.at.coba.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BotDealDao {

    @Query("SELECT * FROM bot_deals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BotDealEntity>>

    @Insert
    suspend fun insert(deal: BotDealEntity): Long

    @Query("UPDATE bot_deals SET wsReplyStatus = :status, serverDealUuid = :uuid, wsReplyMessage = :message WHERE id = :id")
    suspend fun updateReplyStatus(id: Long, status: String, uuid: String?, message: String?)

    @Query("SELECT * FROM bot_deals WHERE wsRef = :ref AND wsReplyStatus = 'pending' LIMIT 1")
    suspend fun findPendingByRef(ref: String): BotDealEntity?

    @Query("SELECT COUNT(*) FROM bot_deals")
    suspend fun count(): Int

    @Query("DELETE FROM bot_deals")
    suspend fun deleteAll()
}
