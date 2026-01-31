package com.rbm.exptracker

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rbm.exptracker.data.Expense
import java.text.NumberFormat
import java.util.*

// --- DASHBOARD COMPONENTS ---

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboard(viewModel: ExpenseViewModel, expenses: List<Expense>) {
    val dailyAvg = viewModel.getDailyAverage(expenses)
    val projection = viewModel.getProjectedMonthlyTotal(expenses)
    val trend = viewModel.getMonthOverMonthTrend(expenses)

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Premium Dynamic Gradient Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor, tertiaryColor)
                    )
                ).padding(24.dp).fillMaxWidth()
            ) {
                Column {
                    Text("Monthly Projection", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text(formatCurrency(projection), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (trend > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${String.format("%.1f", Math.abs(trend))}% ${if (trend > 0) "increase" else "decrease"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Daily Avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(formatCurrency(dailyAvg), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("${expenses.size} entries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        CategoryDonutChart(viewModel, expenses)

        Spacer(Modifier.height(24.dp))

        Text("Frequent Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        FlowRow(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.categories.take(5).forEach { tag ->
                AssistChip(
                    onClick = { viewModel.updateSearch(tag) },
                    label = { Text(tag) },
                    leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(tag)), contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(viewModel: ExpenseViewModel, expenses: List<Expense>) {
    val categoryTotals = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    val total = categoryTotals.values.sum()

    val colors = listOf(
        Color(0xFF6750A4), Color(0xFF958DA5), Color(0xFFD0BCFF), Color(0xFF4F378B),
        Color(0xFFB39DDB), Color(0xFFEADDFF), Color(0xFF21005D)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spending Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    var startAngle = -90f
                    val strokeWidth = 35.dp.toPx()

                    categoryTotals.values.forEachIndexed { index, amt ->
                        val sweepAngle = (amt.toFloat() / total.toFloat().coerceAtLeast(1f)) * 360f
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatCurrency(total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))

            categoryTotals.keys.forEachIndexed { index, cat ->
                val catTotal = categoryTotals[cat] ?: 0.0
                val percentage = if(total > 0) (catTotal / total) * 100 else 0.0

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(colors[index % colors.size]))
                    Spacer(Modifier.width(12.dp))
                    Text(cat, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("${String.format("%.0f", percentage)}%", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                    Text(formatCurrency(catTotal), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
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
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Total Spending ($currentFilter)", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(text = formatCurrency(total), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = (total / budgetToCompare).toFloat().coerceAtMost(1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if(isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Today", "This Week", "This Month", "Range").forEach { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { if (filter == "Range") onRangeClick() else onFilterSelected(filter) },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }
    }
}

// --- FIXED EXPENSE ITEM WITH DELETE BUTTON ---
@Composable
fun ExpenseItem(viewModel: ExpenseViewModel, expense: Expense, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Squircle Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconFromName(viewModel.getCategoryIconName(expense.category)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(expense.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(expense.getFormattedDate(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Amount
            Text(formatCurrency(expense.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.width(8.dp))

            // RESTORED DELETE BUTTON
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// --- SETTINGS SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ExpenseViewModel, onExport: () -> Unit, onClearAll: () -> Unit) {
    var weeklyInput by remember { mutableStateOf(viewModel.weeklyBudget.toString()) }
    var monthlyInput by remember { mutableStateOf(viewModel.monthlyBudget.toString()) }
    var newCategory by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf("Label") }

    val iconOptions = listOf("Fastfood" to Icons.Default.Fastfood, "Transport" to Icons.Default.DirectionsCar, "Rent" to Icons.Default.Home, "Shopping" to Icons.Default.ShoppingCart, "Entertainment" to Icons.Default.Movie, "Ciggerate" to Icons.Default.SmokingRooms, "Travel" to Icons.Default.FlightTakeoff, "School" to Icons.Default.School, "Gym" to Icons.Default.FitnessCenter, "Health" to Icons.Default.MedicalServices)

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        SettingsSectionCard(title = "Security") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(" Biometric Lock", modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.titleMedium)
                }
                Switch(checked = viewModel.isBiometricEnabled, onCheckedChange = { viewModel.toggleBiometric(it) })
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSectionCard(title = "Budget Limits") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weeklyInput,
                    onValueChange = { weeklyInput = it; it.toDoubleOrNull()?.let { v -> viewModel.updateWeeklyBudget(v) } },
                    label = { Text("Weekly Limit") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.DateRange, null) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = monthlyInput,
                    onValueChange = { monthlyInput = it; it.toDoubleOrNull()?.let { v -> viewModel.updateMonthlyBudget(v) } },
                    label = { Text("Monthly Limit") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        SettingsSectionCard(title = "Manage Categories") {
            Column {
                Text("Select Icon:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                    iconOptions.forEach { (name, icon) ->
                        val isSelected = selectedIconName == name
                        IconButton(
                            onClick = { selectedIconName = name },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(imageVector = icon, contentDescription = name, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = { if (newCategory.isNotBlank()) { viewModel.addCategory(newCategory, selectedIconName); newCategory = "" } },
                        containerColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) { Icon(Icons.Default.Add, null) }
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                viewModel.categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(cat)), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text(cat, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                            Icon(Icons.Default.RemoveCircleOutline, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(8.dp))
            Text("Export to CSV")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onClearAll,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.DeleteForever, null)
            Spacer(Modifier.width(8.dp))
            Text("Clear All Data")
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            content()
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(selectedCategory)), contentDescription = null) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(cat)), contentDescription = null) }, onClick = { selectedCategory = cat; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.let { onConfirm(it, selectedCategory, note) } }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(viewModel: ExpenseViewModel, expense: Expense, onDismiss: () -> Unit, onConfirm: (Expense) -> Unit) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var selectedCategory by remember { mutableStateOf(expense.category) }
    var note by remember { mutableStateOf(expense.note) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Edit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(selectedCategory)), contentDescription = null) },
                        modifier = Modifier.menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        viewModel.categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, leadingIcon = { Icon(imageVector = getIconFromName(viewModel.getCategoryIconName(cat)), contentDescription = null) }, onClick = { selectedCategory = cat; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = { Button(onClick = { val amt = amount.toDoubleOrNull() ?: 0.0; if (amt > 0) onConfirm(expense.copy(amount = amt, category = selectedCategory, note = note)) }) { Text("Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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

fun formatCurrency(amount: Double): String = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN")).format(amount)