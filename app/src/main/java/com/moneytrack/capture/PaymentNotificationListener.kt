package com.moneytrack.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.moneytrack.core.TransactionParser
import com.moneytrack.data.MoneyTrackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val allowed = packageName in setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "com.sbi.lotus",
            "com.google.android.gm"
        ) || packageName.contains("hdfc", true) ||
            packageName.contains("icici", true) ||
            packageName.contains("axis", true) ||
            packageName.contains("kotak", true)
        if (!allowed) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val body = extras.getCharSequence("android.text")?.toString().orEmpty()
        val text = "$title $body".trim()
        if (text.isBlank()) return
        val tx = TransactionParser.parse(text, "notification:$packageName", sbn.postTime) ?: return
        scope.launch {
            MoneyTrackDatabase.get(applicationContext).transactionDao().insertIfNew(tx)
        }
    }
}
