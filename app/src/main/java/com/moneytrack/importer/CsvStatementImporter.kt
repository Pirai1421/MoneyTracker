package com.moneytrack.importer

import android.content.Context
import android.net.Uri
import com.moneytrack.core.TransactionParser
import com.moneytrack.data.MoneyTrackDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/** CSV columns: date,description,amount,type. Quoted commas are not supported yet. */
object CsvStatementImporter {
    suspend fun import(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readLines()
            ?: return@withContext 0
        if (lines.size < 2) return@withContext 0
        val dao = MoneyTrackDatabase.get(context).transactionDao()
        var count = 0
        val dateFormats = listOf("yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy", "dd-MM-yyyy")
        lines.drop(1).forEach { line ->
            val cols = line.split(",").map { it.trim().trim('"') }
            if (cols.size < 4) return@forEach
            val date = dateFormats.firstNotNullOfOrNull { pattern ->
                runCatching { SimpleDateFormat(pattern, Locale.US).parse(cols[0])?.time }.getOrNull()
            } ?: return@forEach
            val amount = cols[2].replace(",", "").toBigDecimalOrNull() ?: return@forEach
            val direction = when (cols[3].uppercase(Locale.ROOT)) {
                "CREDIT", "CR", "INCOME" -> "INCOME"
                "DEBIT", "DR", "EXPENSE" -> "EXPENSE"
                else -> return@forEach
            }
            val paise = runCatching { amount.abs().movePointRight(2).longValueExact() }.getOrNull() ?: return@forEach
            val desc = cols[1]
            val tx = com.moneytrack.data.Transaction(
                amountPaise = paise, direction = direction, merchant = desc.take(60),
                source = "csv_import", category = TransactionParser.categorize(desc),
                occurredAt = date, fingerprint = TransactionParser.fingerprint("csv|$date|$paise|$direction|$desc"),
                rawNote = desc.take(300)
            )
            if (dao.insertIfNew(tx) != -1L) count++
        }
        count
    }
}
