package com.mmusa.qadatracker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget 4: Widget 1 + Widget 3 combined - countdown at top, full 5-prayer
 *  list with toggles below. Resizable; colors follow the app's theme setting. */
class CombinedWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateOne(context, appWidgetManager, it) }
        CountdownTicker.reconcile(context)
    }

    override fun onEnabled(context: Context) { CountdownTicker.reconcile(context) }
    override fun onDisabled(context: Context) { CountdownTicker.reconcile(context) }

    companion object {
        fun updateOne(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_combined_layout)
            val colors = WidgetData.getColors(context)
            views.setInt(R.id.widget_root, "setBackgroundColor", colors.card)
            val en = WidgetData.getLang(context) == "en"

            val cycleDate = WidgetData.getCycleDate(context)
            val times = WidgetData.getPrayerTimes(context)
            val now = System.currentTimeMillis()

            // --- top: countdown ---
            val current = WidgetData.currentPrayer(times, now)
            if (current != null && current.end > 0) {
                val done = cycleDate != null && WidgetData.isDone(context, cycleDate, current.id)
                val remaining = current.end - now
                if (done) {
                    val nextId = WidgetData.nextPrayerIdAfter(current.id)
                    views.setTextViewText(R.id.combined_prayer_name, "✓ " + WidgetData.prayerLabel(context, current.id) + (if (en) " - until " + WidgetData.prayerLabel(context, nextId) else " - متبقي لدخول وقت " + WidgetData.prayerLabel(context, nextId)))
                    views.setTextColor(R.id.combined_prayer_name, colors.done)
                    views.setTextViewText(R.id.combined_countdown, WidgetData.formatRemaining(context, remaining))
                    views.setTextColor(R.id.combined_countdown, colors.muted)
                } else {
                    views.setTextViewText(R.id.combined_prayer_name, WidgetData.prayerLabel(context, current.id) + (if (en) " - left to pray" else " - متبقي للصلاة"))
                    views.setTextColor(R.id.combined_prayer_name, colors.text)
                    views.setTextViewText(R.id.combined_countdown, WidgetData.formatRemaining(context, remaining))
                    views.setTextColor(R.id.combined_countdown, if (remaining < 15 * 60_000) colors.missed else colors.active)
                }
            } else {
                val next = WidgetData.nextUpcoming(times, now)
                if (next != null) {
                    views.setTextViewText(R.id.combined_prayer_name, WidgetData.prayerLabel(context, next.id) + (if (en) " - starts in" else " - هيدخل بعد"))
                    views.setTextColor(R.id.combined_prayer_name, colors.muted)
                    views.setTextViewText(R.id.combined_countdown, WidgetData.formatRemaining(context, next.start - now))
                    views.setTextColor(R.id.combined_countdown, colors.muted)
                } else {
                    views.setTextViewText(R.id.combined_prayer_name, if (en) "Open the app" else "افتح التطبيق")
                    views.setTextColor(R.id.combined_prayer_name, colors.muted)
                    views.setTextViewText(R.id.combined_countdown, "--:--")
                    views.setTextColor(R.id.combined_countdown, colors.muted)
                }
            }

            // --- bottom: 5-row list, same pattern as TodayPrayersWidget ---
            val rowIds = listOf(
                Triple(R.id.crow1_name, R.id.crow1_time, R.id.crow1_toggle),
                Triple(R.id.crow2_name, R.id.crow2_time, R.id.crow2_toggle),
                Triple(R.id.crow3_name, R.id.crow3_time, R.id.crow3_toggle),
                Triple(R.id.crow4_name, R.id.crow4_time, R.id.crow4_toggle),
                Triple(R.id.crow5_name, R.id.crow5_time, R.id.crow5_toggle)
            )
            WidgetData.PRAYER_ORDER.forEachIndexed { i, id ->
                val (nameId, timeId, toggleId) = rowIds[i]
                val pt = times.find { it.id == id }
                views.setTextViewText(nameId, WidgetData.prayerLabel(context, id))
                views.setTextColor(nameId, colors.text)
                views.setTextViewText(timeId, if (pt != null) WidgetData.formatTime(context, pt.start) else "--:--")
                views.setTextColor(timeId, colors.muted)

                val status = if (cycleDate != null && pt != null) WidgetData.statusOf(context, cycleDate, pt, now) else "upcoming"
                views.setTextViewText(toggleId, if (status == "done") "✓" else "○")
                views.setTextColor(toggleId, when (status) {
                    "done" -> colors.done
                    "active" -> colors.active
                    "missed" -> colors.missed
                    else -> colors.muted
                })

                if (cycleDate != null) {
                    val intent = Intent(context, WidgetClickReceiver::class.java).apply {
                        action = WidgetClickReceiver.ACTION_TOGGLE
                        putExtra("cycleDate", cycleDate)
                        putExtra("prayerId", id)
                    }
                    val reqCode = widgetId * 10 + i
                    val pi = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    views.setOnClickPendingIntent(toggleId, pi)
                }
            }

            mgr.updateAppWidget(widgetId, views)
        }
    }
}
