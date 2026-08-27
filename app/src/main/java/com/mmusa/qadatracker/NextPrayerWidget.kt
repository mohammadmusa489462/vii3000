package com.mmusa.qadatracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget 2: just the prayer that's currently due (or the next one, if none is due
 *  yet today), with a single done/not-done toggle - same rule as the main app: you
 *  can only mark a prayer done once its own time has actually started. */
class NextPrayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateOne(context, appWidgetManager, it) }
        CountdownTicker.reconcile(context)
    }

    override fun onEnabled(context: Context) { CountdownTicker.reconcile(context) }
    override fun onDisabled(context: Context) { CountdownTicker.reconcile(context) }

    companion object {
        fun updateOne(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_next_layout)
            val colors = WidgetData.getColors(context)
            views.setInt(R.id.widget_root, "setBackgroundColor", colors.card)
            val en = WidgetData.getLang(context) == "en"

            val cycleDate = WidgetData.getCycleDate(context)
            val times = WidgetData.getPrayerTimes(context)
            val now = System.currentTimeMillis()
            val current = WidgetData.currentPrayer(times, now)
            val target = current ?: WidgetData.nextUpcoming(times, now)

            if (target == null || cycleDate == null) {
                views.setTextViewText(R.id.widget_title, if (en) "No data yet - open the app" else "لسه مفيش بيانات - افتح التطبيق")
                views.setTextColor(R.id.widget_title, colors.muted)
                views.setTextViewText(R.id.next_name, "")
                views.setTextViewText(R.id.next_time, "")
                views.setTextViewText(R.id.next_status, "")
                views.setTextViewText(R.id.next_toggle, "")
                mgr.updateAppWidget(widgetId, views)
                return
            }

            val status = WidgetData.statusOf(context, cycleDate, target, now)
            views.setTextViewText(R.id.widget_title, if (en) "Prayer" else "الصلاة")
            views.setTextColor(R.id.widget_title, colors.muted)
            views.setTextViewText(R.id.next_name, WidgetData.prayerLabel(context, target.id))
            views.setTextColor(R.id.next_name, colors.text)
            views.setTextViewText(R.id.next_time, WidgetData.formatTime(context, target.start))
            views.setTextColor(R.id.next_time, colors.muted)
            views.setTextViewText(R.id.next_status, WidgetData.statusLabel(context, status))
            views.setTextColor(R.id.next_status, when (status) {
                "done" -> colors.done
                "active" -> colors.active
                "missed" -> colors.missed
                else -> colors.muted
            })

            val toggleEnabled = status != "upcoming"
            views.setTextViewText(R.id.next_toggle, if (status == "done") "✓" else if (toggleEnabled) "○" else "")
            views.setTextColor(R.id.next_toggle, if (status == "done") colors.done else colors.accent)

            if (toggleEnabled) {
                val intent = Intent(context, WidgetClickReceiver::class.java).apply {
                    action = WidgetClickReceiver.ACTION_TOGGLE
                    putExtra("cycleDate", cycleDate)
                    putExtra("prayerId", target.id)
                }
                val pi = PendingIntent.getBroadcast(context, widgetId * 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.next_toggle, pi)
            }

            mgr.updateAppWidget(widgetId, views)
        }
    }
}
