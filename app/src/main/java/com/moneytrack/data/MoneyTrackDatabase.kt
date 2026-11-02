package com.moneytrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class], version = 1, exportSchema = true)
abstract class MoneyTrackDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    companion object {
        @Volatile private var INSTANCE: MoneyTrackDatabase? = null
        fun get(context: Context): MoneyTrackDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext,
                MoneyTrackDatabase::class.java, "moneytrack-local.db").build().also { INSTANCE = it }
        }
    }
}
