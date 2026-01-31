package com.rbm.exptracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbm.exptracker.data.Expense
import com.rbm.exptracker.data.ExpenseDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ExpenseViewModel(private val dao: ExpenseDao, context: Context) : ViewModel() {

    private val prefs = context.getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)

    private val _selectedFilter = MutableStateFlow("This Month")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)

    var weeklyBudget by mutableStateOf(prefs.getFloat("weekly_limit", 5000f).toDouble())
    var monthlyBudget by mutableStateOf(prefs.getFloat("monthly_limit", 25000f).toDouble())

    private val defaultCategories = setOf("Food", "Transport", "Rent", "Shopping", "Entertainment", "Ciggerate", "Travel")
    var categories by mutableStateOf(prefs.getStringSet("custom_categories", defaultCategories)?.toList()?.sorted() ?: defaultCategories.toList())

    var activeCategoryFilter by mutableStateOf<String?>(null)

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        dao.getAllExpenses(),
        _selectedFilter,
        _searchQuery,
        _dateRange
    ) { expenses, filter, query, range ->
        val now = System.currentTimeMillis()
        var list = when (filter) {
            "Today" -> expenses.filter { isSameDay(it.timestamp, now) }
            "This Week" -> expenses.filter { it.timestamp > now - (7 * 24 * 60 * 60 * 1000) }
            "This Month" -> expenses.filter { isSameMonth(it.timestamp, now) }
            "Range" -> if (range != null) expenses.filter { it.timestamp in range.first..range.second } else expenses
            else -> expenses
        }
        if (query.isNotEmpty()) {
            list = list.filter { it.category.contains(query, ignoreCase = true) || it.note.contains(query, ignoreCase = true) }
        }
        activeCategoryFilter?.let { cat -> list = list.filter { it.category == cat } }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- ICON MAPPING ---
    fun getCategoryIconName(category: String): String {
        return prefs.getString("icon_$category", "Label") ?: "Label"
    }

    fun addCategory(name: String, iconName: String) {
        val newList = categories.toMutableList()
        if (!newList.contains(name)) {
            newList.add(name)
            categories = newList.sorted()
            prefs.edit()
                .putStringSet("custom_categories", categories.toSet())
                .putString("icon_$name", iconName)
                .apply()
        }
    }

    fun deleteCategory(name: String) {
        val newList = categories.toMutableList()
        newList.remove(name)
        categories = newList.sorted()
        prefs.edit()
            .putStringSet("custom_categories", categories.toSet())
            .remove("icon_$name")
            .apply()
    }

    // --- ANALYTICS ---
    fun getDailyAverage(expenses: List<Expense>): Double {
        if (expenses.isEmpty()) return 0.0
        val days = expenses.groupBy { isSameDay(it.timestamp, it.timestamp) }.size.coerceAtLeast(1)
        return expenses.sumOf { it.amount } / days
    }

    fun getProjectedMonthlyTotal(expenses: List<Expense>): Double {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalSoFar = expenses.sumOf { it.amount }
        return if (currentDay > 0) (totalSoFar / currentDay) * daysInMonth else 0.0
    }

    fun getMonthOverMonthTrend(expenses: List<Expense>): Double {
        val now = Calendar.getInstance()
        val currentMonthTotal = expenses.filter { isSameMonth(it.timestamp, now.timeInMillis) }.sumOf { it.amount }
        val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthTotal = expenses.filter { isSameMonth(it.timestamp, lastMonth.timeInMillis) }.sumOf { it.amount }
        if (lastMonthTotal == 0.0) return 0.0
        return ((currentMonthTotal - lastMonthTotal) / lastMonthTotal) * 100
    }

    fun updateMonthlyBudget(v: Double) {
        monthlyBudget = v
        prefs.edit().putFloat("monthly_limit", v.toFloat()).apply()
    }

    fun updateWeeklyBudget(v: Double) {
        weeklyBudget = v
        prefs.edit().putFloat("weekly_limit", v.toFloat()).apply()
    }

    fun updateSearch(query: String) { _searchQuery.value = query }
    fun updateFilter(filter: String) { _selectedFilter.value = filter }
    fun setRange(start: Long, end: Long) {
        _dateRange.value = Pair(start, end)
        _selectedFilter.value = "Range"
    }

    fun addExpense(amount: Double, category: String, note: String) {
        viewModelScope.launch { dao.insertExpense(Expense(amount = amount, category = category, note = note)) }
    }

    fun deleteExpense(expense: Expense) { viewModelScope.launch { dao.deleteExpense(expense) } }
    fun clearAllExpenses() { viewModelScope.launch { dao.deleteAllExpenses() } }

    fun exportToCSV(context: Context): File {
        val file = File(context.cacheDir, "expenses_report.csv")
        file.writeText("Date,Category,Amount,Note\n")
        filteredExpenses.value.forEach { expense ->
            file.appendText("${expense.getCsvDate()},${expense.category.trim()},${expense.amount},${expense.note.trim()}\n")
        }
        return file
    }

    private fun isSameDay(t1: Long, t2: Long) = Calendar.getInstance().apply { timeInMillis = t1 }.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().apply { timeInMillis = t2 }.get(Calendar.DAY_OF_YEAR)
    private fun isSameMonth(t1: Long, t2: Long) = Calendar.getInstance().apply { timeInMillis = t1 }.get(Calendar.MONTH) == Calendar.getInstance().apply { timeInMillis = t2 }.get(Calendar.MONTH)
}