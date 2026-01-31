package com.rbm.exptracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    // For the UI list (Displays "Jan 29, 2026")
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // NEW: For CSV Export (Displays "2026-01-29" - No commas to break columns)
    fun getCsvDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}