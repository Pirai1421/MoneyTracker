package com.moneytrack.core

import org.junit.Assert.*
import org.junit.Test

class TransactionParserTest {
    @Test fun parsesDebit() {
        val tx = TransactionParser.parse("Rs. 250 debited from your account to Swiggy", "test", 1000L)
        assertNotNull(tx)
        assertEquals(25000L, tx!!.amountPaise)
        assertEquals("EXPENSE", tx.direction)
    }

    @Test fun parsesCredit() {
        val tx = TransactionParser.parse("INR 1000 credited to your account", "test", 1000L)
        assertNotNull(tx)
        assertEquals("INCOME", tx!!.direction)
    }

    @Test fun ignoresAmbiguousMessage() {
        assertNull(TransactionParser.parse("Your account balance is Rs. 5000", "test", 1000L))
    }

    @Test fun categorizesFood() {
        assertEquals("Food", TransactionParser.categorize("Payment to Swiggy"))
    }

    @Test fun fingerprintIsDeterministic() {
        assertEquals(TransactionParser.fingerprint("abc"), TransactionParser.fingerprint("abc"))
    }
}
