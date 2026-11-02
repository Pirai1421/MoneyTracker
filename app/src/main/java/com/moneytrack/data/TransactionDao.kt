package com.moneytrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(transaction: Transaction): Long

    @Query("SELECT COALESCE(SUM(CASE WHEN direction='INCOME' THEN amountPaise ELSE -amountPaise END),0) FROM transactions")
    fun observeNetFlowPaise(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountPaise),0) FROM transactions WHERE direction='EXPENSE' AND occurredAt >= :from AND occurredAt < :to")
    fun observeExpenses(from: Long, to: Long): Flow<Long>

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
