package com.moneytrack

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.moneytrack.data.MoneyTrackDatabase
import com.moneytrack.data.Transaction
import com.moneytrack.importer.CsvStatementImporter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = MoneyTrackDatabase.get(this).transactionDao()
        setContent {
            MaterialTheme {
                var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
                var importMessage by remember { mutableStateOf("Local-only storage enabled") }
                val currencyFormat = remember {
                    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                        currency = Currency.getInstance("INR")
                    }
                }
                LaunchedEffect(Unit) { dao.observeAll().collect { transactions = it } }
                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    if (uri != null) lifecycleScope.launch {
                        val added = CsvStatementImporter.import(this@MainActivity, uri)
                        importMessage = "Imported $added new transactions"
                    }
                }
                val income = transactions.filter { it.direction == "INCOME" }.sumOf { it.amountPaise }
                val expense = transactions.filter { it.direction == "EXPENSE" }.sumOf { it.amountPaise }
                Scaffold(topBar = { TopAppBar(title = { Text("MoneyTrack") }, actions = {
                    Text("READ-ONLY", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 16.dp))
                }) }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card {
                                Column(Modifier.padding(18.dp)) {
                                    Text("Net tracked cash flow", style = MaterialTheme.typography.titleMedium)
                                    Text(currencyFormat.format((income - expense) / 100.0), style = MaterialTheme.typography.headlineMedium)
                                    Text("Not your live bank balance", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryCard("Money in", currencyFormat.format(income / 100.0), Modifier.weight(1f))
                            SummaryCard("Money out", currencyFormat.format(expense / 100.0), Modifier.weight(1f))
                        } }
                        item {
                            Text("Data sources", style = MaterialTheme.typography.titleLarge)
                            Text(importMessage, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Enable transaction notifications")
                            }
                            OutlinedButton(onClick = { picker.launch(arrayOf("text/*", "text/csv")) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Import bank CSV statement")
                            }
                        }
                        item { Text("Recent transactions", style = MaterialTheme.typography.titleLarge) }
                        if (transactions.isEmpty()) item {
                            Text("No transactions captured yet. Enable notification access or import a CSV statement.")
                        }
                        items(transactions, key = { it.id }) { tx ->
                            Card {
                                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(tx.merchant, style = MaterialTheme.typography.titleMedium)
                                        Text("${tx.category} • ${tx.source.substringBefore(':')}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text((if (tx.direction == "INCOME") "+" else "−") + currencyFormat.format(tx.amountPaise / 100.0))
                                }
                            }
                        }
                        item {
                            Text("Privacy: transactions stay in the app's local database. No payment or transfer features exist.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge)
    } }
}
