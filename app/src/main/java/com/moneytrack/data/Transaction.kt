package com.moneytrack.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "transactions", indices = [Index(value = ["fingerprint"], unique = true)])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val direction: String,
    val merchant: String,
    val source: String,
    val category: String,
    val occurredAt: Long,
    val fingerprint: String,
    val rawNote: String = ""
)
