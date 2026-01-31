package com.rbm.exptracker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rbm.exptracker.data.Expense
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboard(viewModel: ExpenseViewModel, expenses: List<Expense>) {
    val dailyAvg = viewModel.getDailyAverage(expenses)
    val projection = viewModel.getProjectedMonthlyTotal(expenses)
    val trend = viewModel.getMonthOverMonthTrend(expenses)

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFD1C4E9))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Monthly Projection", style = MaterialTheme.typography.labelMedium)
                Text(formatCurrency(projection), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f", Math.abs(trend))}% ${if (trend > 0) "increase" else "decrease"} from last month", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Daily Avg", style = MaterialTheme.typography.labelSmall)
                    Text(formatCurrency(dailyAvg), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Count", style = MaterialTheme.typography.labelSmall)
                    Text("${expenses.size} entries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        CategoryPieChart(viewModel, expenses)
        Spacer(Modifier.height(24.dp))

        Text("Frequent Categories", style = MaterialTheme.typography.titleMedium)
        FlowRow(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.categories.take(5).forEach { tag ->
                AssistChip(
                    onClick = { viewModel.updateSearch(tag) },
                    label = { Text(tag) },
                    leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(tag)), contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }
    }
}

@Composable
fun CategoryPieChart(viewModel: ExpenseViewModel, expenses: List<Expense>) {
    val categoryTotals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    val total = categoryTotals.values.sum()
    val colors = listOf(Color(0xFF6750A4), Color(0xFF958DA5), Color(0xFFD0BCFF), Color(0xFF4F378B), Color(0xFFB39DDB))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spending Distribution", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Canvas(modifier = Modifier.size(150.dp)) {
                var startAngle = -90f
                categoryTotals.values.forEachIndexed { index, amt ->
                    val sweepAngle = (amt.toFloat() / total.toFloat().coerceAtLeast(1f)) * 360f
                    drawArc(color = colors[index % colors.size], startAngle, sweepAngle, true)
                    startAngle += sweepAngle
                }
            }
            Spacer(Modifier.height(16.dp))
            categoryTotals.keys.forEach { cat ->
                val index = categoryTotals.keys.toList().indexOf(cat)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(colors[index % colors.size]))
                    Spacer(Modifier.width(8.dp))
                    Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(cat)), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Text(" $cat", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(formatCurrency(categoryTotals[cat] ?: 0.0), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHeader(total: Double, weeklyBudget: Double, monthlyBudget: Double, currentFilter: String, searchQuery: String, onSearchChange: (String) -> Unit, onFilterSelected: (String) -> Unit, onRangeClick: () -> Unit) {
    val budgetToCompare = if (currentFilter == "This Week") weeklyBudget else monthlyBudget
    val isOverBudget = (currentFilter == "This Week" || currentFilter == "This Month") && total > budgetToCompare

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = onSearchChange, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            placeholder = { Text("Search by category or note") }, leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }, shape = MaterialTheme.shapes.large
        )
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Total Spending ($currentFilter)", style = MaterialTheme.typography.labelMedium)
                Text(text = formatCurrency(total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Today", "This Week", "This Month", "Range").forEach { filter ->
                FilterChip(selected = currentFilter == filter, onClick = { if (filter == "Range") onRangeClick() else onFilterSelected(filter) }, label = { Text(filter) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(viewModel: ExpenseViewModel, categories: List<String>, onDismiss: () -> Unit, onConfirm: (Double, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "") }
    var note by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(selectedCategory)), contentDescription = null) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(cat)), contentDescription = null) },
                                onClick = { selectedCategory = cat; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.let { onConfirm(it, selectedCategory, note) } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SettingsScreen(viewModel: ExpenseViewModel, onExport: () -> Unit, onClearAll: () -> Unit) {
    var weeklyInput by remember { mutableStateOf(viewModel.weeklyBudget.toString()) }
    var monthlyInput by remember { mutableStateOf(viewModel.monthlyBudget.toString()) }
    var newCategory by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Label") }

    val iconOptions = listOf(
        "Fastfood" to Icons.Default.Fastfood,
        "Transport" to Icons.Default.DirectionsCar,
        "Rent" to Icons.Default.Home,
        "Shopping" to Icons.Default.ShoppingCart,
        "Entertainment" to Icons.Default.Movie,
        "Ciggerate" to Icons.Default.SmokingRooms,
        "Travel" to Icons.Default.FlightTakeoff,
        "School" to Icons.Default.School,
        "Gym" to Icons.Default.FitnessCenter,
        "Health" to Icons.Default.MedicalServices
    )

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Budget Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = weeklyInput, onValueChange = { weeklyInput = it; it.toDoubleOrNull()?.let { v -> viewModel.updateWeeklyBudget(v) } }, label = { Text("Weekly Budget (₹)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = monthlyInput, onValueChange = { monthlyInput = it; it.toDoubleOrNull()?.let { v -> viewModel.updateMonthlyBudget(v) } }, label = { Text("Monthly Budget (₹)") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        Text("Manage Categories", style = MaterialTheme.typography.titleMedium)

        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            iconOptions.forEach { (name, icon) ->
                IconButton(
                    onClick = { selectedIconName = name },
                    modifier = Modifier.background(if (selectedIconName == name) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape)
                ) {
                    Icon(imageVector = icon, contentDescription = name, tint = if (selectedIconName == name) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, label = { Text("New Category Name") }, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (newCategory.isNotBlank()) { viewModel.addCategory(newCategory, selectedIconName); newCategory = "" } }) { Icon(Icons.Default.AddCircle, null, tint = MaterialTheme.colorScheme.primary) }
        }

        viewModel.categories.forEach { cat ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(cat)), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(cat, Modifier.padding(start = 12.dp).weight(1f))
                IconButton(onClick = { viewModel.deleteCategory(cat) }) { Icon(Icons.Default.RemoveCircleOutline, null, tint = Color.Gray) }
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
        OutlinedButton(onClick = onClearAll, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) { Text("Clear All Data") }
    }
}

@Composable
fun ExpenseItem(viewModel: ExpenseViewModel, expense: Expense, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(expense.category)), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.category, fontWeight = FontWeight.Bold)
                Text(expense.getFormattedDate(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(formatCurrency(expense.amount), fontWeight = FontWeight.Bold)
            IconButton(onClick = onDelete) { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.Gray) }
        }
    }
}

fun getIconFromName(name: String): ImageVector = when (name) {
    "Fastfood" -> Icons.Default.Fastfood
    "Transport" -> Icons.Default.DirectionsCar
    "Rent" -> Icons.Default.Home
    "Shopping" -> Icons.Default.ShoppingCart
    "Entertainment" -> Icons.Default.Movie
    "Ciggerate" -> Icons.Default.SmokingRooms
    "Travel" -> Icons.Default.FlightTakeoff
    "School" -> Icons.Default.School
    "Gym" -> Icons.Default.FitnessCenter
    "Health" -> Icons.Default.MedicalServices
    else -> Icons.Default.Label
}

fun formatCurrency(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)