package com.moneytrack.core

import com.moneytrack.data.Transaction
import java.security.MessageDigest
import java.util.Locale

/** Conservative parser for common Indian bank/UPI notification wording. */
object TransactionParser {
    private val amountRegex = Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val upiAmountRegex = Regex("(?i)(?:debited|credited|paid|received|sent|spent).*?(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")

    fun parse(text: String, source: String, timestamp: Long): Transaction? {
        val normalized = text.replace("\\n", " ").trim()
        val match = upiAmountRegex.find(normalized) ?: amountRegex.find(normalized) ?: return null
        val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
        val paise = runCatching { amount.movePointRight(2).longValueExact() }.getOrNull() ?: return null
        if (paise <= 0) return null
        val lower = normalized.lowercase(Locale.ROOT)
        val direction = when {
            listOf("credited", "received", "deposited", "refund received").any(lower::contains) -> "INCOME"
            listOf("debited", "paid", "sent", "spent", "withdrawn").any(lower::contains) -> "EXPENSE"
            else -> return null
        }
        val merchant = Regex("(?i)(?:to|from|at)\\s+([a-z0-9 ._@-]{2,40})")
            .find(normalized)?.groupValues?.get(1)?.trim() ?: "Unknown"
        val category = categorize(merchant + " " + normalized)
        val fingerprint = fingerprint("$source|$timestamp|$paise|$direction|$normalized")
        return Transaction(amountPaise = paise, direction = direction, merchant = merchant,
            source = source, category = category, occurredAt = timestamp,
            fingerprint = fingerprint, rawNote = normalized.take(300))
    }

    fun categorize(text: String): String {
        val s = text.lowercase(Locale.ROOT)
        return when {
            listOf("swiggy", "zomato", "restaurant", "food", "cafe").any(s::contains) -> "Food"
            listOf("uber", "ola", "metro", "petrol", "fuel").any(s::contains) -> "Transport"
            listOf("amazon", "flipkart", "shopping", "myntra").any(s::contains) -> "Shopping"
            listOf("electricity", "water bill", "recharge", "broadband").any(s::contains) -> "Bills"
            listOf("salary", "payroll").any(s::contains) -> "Salary"
            s.contains("refund") -> "Refund"
            else -> "Other"
        }
    }

    fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
