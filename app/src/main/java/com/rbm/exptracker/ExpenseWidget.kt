package com.rbm.exptracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rbm.exptracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ExpenseWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)

                // Get Today's Date
                val now = Calendar.getInstance()
                val todayDay = now.get(Calendar.DAY_OF_YEAR)
                val todayYear = now.get(Calendar.YEAR)

                // Fetch & Calculate
                val expenses = db.expenseDao().getAllExpensesList()
                val todayTotal = expenses.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    c.get(Calendar.DAY_OF_YEAR) == todayDay && c.get(Calendar.YEAR) == todayYear
                }.sumOf { it.amount }

                val formattedTotal = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(todayTotal)

                withContext(Dispatchers.Main) {
                    val views = RemoteViews(context.packageName, R.layout.widget_today_expense)
                    views.setTextViewText(R.id.widget_amount, formattedTotal)

                    // Add Button Click -> Open App with Dialog Flag
                    val addIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("OPEN_ADD_DIALOG", true)
                    }
                    val addPendingIntent = PendingIntent.getActivity(context, 0, addIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_add_btn, addPendingIntent)

                    // Total Click -> Open App Normally
                    val openIntent = Intent(context, MainActivity::class.java)
                    val openPendingIntent = PendingIntent.getActivity(context, 1, openIntent, PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(R.id.widget_amount, openPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}