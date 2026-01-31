package com.rbm.exptracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.rbm.exptracker.data.AppDatabase
import com.rbm.exptracker.ui.theme.EXPTrackerTheme

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.expenseDao()
        var canAccessApp by mutableStateOf(false)

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                canAccessApp = true
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Expense Pro Security")
            .setSubtitle("Authenticate to unlock")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)

        setContent {
            EXPTrackerTheme {
                if (canAccessApp) {
                    val viewModel: ExpenseViewModel = remember { ExpenseViewModel(dao, applicationContext) }
                    val expenses by viewModel.filteredExpenses.collectAsState()
                    val currentFilter by viewModel.selectedFilter.collectAsState()
                    val searchQuery by viewModel.searchQuery.collectAsState()

                    var selectedTab by remember { mutableIntStateOf(0) }
                    var showAddDialog by remember { mutableStateOf(false) }
                    var showRangePicker by remember { mutableStateOf(false) }
                    val rangePickerState = rememberDateRangePickerState()

                    if (showAddDialog) {
                        AddExpenseDialog(viewModel = viewModel, categories = viewModel.categories, onDismiss = { showAddDialog = false }, onConfirm = { amt, cat, note ->
                            viewModel.addExpense(amt, cat, note)
                            showAddDialog = false
                        })
                    }

                    if (showRangePicker) {
                        DatePickerDialog(
                            onDismissRequest = { showRangePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    rangePickerState.selectedStartDateMillis?.let { s ->
                                        rangePickerState.selectedEndDateMillis?.let { e -> viewModel.setRange(s, e) }
                                    }
                                    showRangePicker = false
                                }) { Text("Apply") }
                            }
                        ) { DateRangePicker(state = rangePickerState, modifier = Modifier.weight(1f)) }
                    }

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(imageVector = Icons.Default.List, contentDescription = null) }, label = { Text("Tracker") })
                                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) }, label = { Text("Dashboard") })
                                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) }, label = { Text("Settings") })
                            }
                        },
                        floatingActionButton = {
                            if (selectedTab == 0) FloatingActionButton(onClick = { showAddDialog = true }) { Icon(imageVector = Icons.Default.Add, contentDescription = null) }
                        }
                    ) { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            when (selectedTab) {
                                0 -> {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        item { DashboardHeader(expenses.sumOf { it.amount }, viewModel.weeklyBudget, viewModel.monthlyBudget, currentFilter, searchQuery, { viewModel.updateSearch(it) }, { viewModel.updateFilter(it) }, { showRangePicker = true }) }
                                        items(expenses, key = { it.id }) { expense -> ExpenseItem(viewModel, expense, { viewModel.deleteExpense(expense) }) }
                                    }
                                }
                                1 -> AnalyticsDashboard(viewModel, expenses)
                                2 -> SettingsScreen(viewModel, onExport = {
                                    val file = viewModel.exportToCSV(this@MainActivity)
                                    val uri = FileProvider.getUriForFile(this@MainActivity, "${packageName}.provider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(intent, "Share CSV Report"))
                                }, onClearAll = { viewModel.clearAllExpenses() })
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { biometricPrompt.authenticate(promptInfo) }) { Text("Unlock App") }
                    }
                }
            }
        }
    }
}