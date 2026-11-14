package com.egepaksoy.interner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.egepaksoy.interner.InternerApp
import com.egepaksoy.interner.MainActivity
import com.egepaksoy.interner.R
import com.egepaksoy.interner.data.Internship
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.TR
import com.egepaksoy.interner.data.computeStats
import com.egepaksoy.interner.data.dayNumbers
import com.egepaksoy.interner.ui.entryRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Widget'ların çizmek için ihtiyaç duyduğu veri. */
class WidgetData(val internship: Internship?, val entries: List<LogEntry>, val today: Long)

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private val dayMonthFmt = DateTimeFormatter.ofPattern("d MMMM, EEEE", TR)
private val shortFmt = DateTimeFormatter.ofPattern("d MMM", TR)
private val weekdayFmt = DateTimeFormatter.ofPattern("EEE", TR)

/** Veri her değiştiğinde ana ekrandaki tüm widget'ları yeniden çizer. */
object Widgets {
    private val providers = listOf(
        ProgressWidget::class.java, TodayWidget::class.java, LastEntryWidget::class.java, WeekWidget::class.java,
    )

    fun updateAll(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app) ?: return
        for (provider in providers) {
            val ids = manager.getAppWidgetIds(ComponentName(app, provider))
            if (ids.isNotEmpty()) provider.getDeclaredConstructor().newInstance().onUpdate(app, manager, ids)
        }
    }

    /** [route] ekranını açan dokunma niyeti; `null` ise uygulamanın ana ekranı açılır. */
    fun openApp(context: Context, route: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (route != null) {
            intent.putExtra(MainActivity.EXTRA_ROUTE, route)
            // Farklı rotalar ayrı PendingIntent sayılsın diye veri alanı da benzersiz.
            intent.data = Uri.parse("interner://open/$route")
        }
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

abstract class BaseWidget : AppWidgetProvider() {
    /** @param widthDp widget'ın ana ekrandaki genişliği (bilinmiyorsa makul bir varsayılan) */
    abstract fun render(context: Context, data: WidgetData, widthDp: Int = DEFAULT_WIDTH_DP): RemoteViews

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Yayın alıcısı içinden çağrıldıysa veritabanı okunana kadar süreci canlı tut.
        val pending = runCatching { goAsync() }.getOrNull()
        scope.launch {
            try {
                val dao = (context.applicationContext as InternerApp).db.dao()
                val data = WidgetData(dao.currentInternship(), dao.allEntries(), LocalDate.now().toEpochDay())
                for (id in ids) manager.updateAppWidget(id, render(context, data, widthDp(manager, id)))
            } finally {
                pending?.finish()
            }
        }
    }

    // Boyutu değiştirilince metin sığdırma yeniden hesaplansın.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        newOptions: android.os.Bundle,
    ) = onUpdate(context, manager, intArrayOf(id))

    /** Dikey ekrandaki genişlik; ana ekran bildirmezse varsayılan kullanılır. */
    private fun widthDp(manager: AppWidgetManager, id: Int): Int =
        manager.getAppWidgetOptions(id)?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            ?.takeIf { it > 0 } ?: DEFAULT_WIDTH_DP

    companion object {
        const val DEFAULT_WIDTH_DP = 250
    }
}

/**
 * [text]'i [maxWidthPx] genişliğe tek satır olarak sığdırır: sığmazsa kelime kelime kısaltıp
 * sonuna "…" ekler. İlk kelime bile sığmıyorsa harf düzeyinde kısaltır.
 */
fun fitWords(text: String, paint: android.graphics.Paint, maxWidthPx: Float): String {
    val clean = text.trim().replace(Regex("\\s+"), " ")
    if (paint.measureText(clean) <= maxWidthPx) return clean
    val words = clean.split(' ')
    var fitted = ""
    for (word in words) {
        val candidate = if (fitted.isEmpty()) word else "$fitted $word"
        if (paint.measureText("$candidate…") > maxWidthPx) break
        fitted = candidate
    }
    if (fitted.isNotEmpty()) return "${fitted.trimEnd(',', '.', ';', ':', '-')}…"
    var chars = clean
    while (chars.length > 1 && paint.measureText("$chars…") > maxWidthPx) chars = chars.dropLast(1)
    return "$chars…"
}

/** Gün sayacı ve hedefe göre ilerleme çubuğu. */
class ProgressWidget : BaseWidget() {
    override fun render(context: Context, data: WidgetData, widthDp: Int) =
        RemoteViews(context.packageName, R.layout.widget_progress).apply {
            val stats = computeStats(data.entries)
            val target = data.internship?.targetDays ?: 0
            setTextViewText(R.id.progress_company, data.internship?.company?.ifBlank { null } ?: "Staj Defteri")
            setTextViewText(R.id.progress_days, stats.days.toString())
            if (target > 0) {
                val left = (target - stats.days).coerceAtLeast(0)
                setTextViewText(R.id.progress_target, "/ $target gün")
                setViewVisibility(R.id.progress_bar, View.VISIBLE)
                setProgressBar(R.id.progress_bar, target, stats.days.coerceAtMost(target), false)
                setTextViewText(
                    R.id.progress_detail,
                    if (left == 0) "Hedef tamamlandı" else "$left gün kaldı",
                )
            } else {
                setTextViewText(R.id.progress_target, "gün")
                setViewVisibility(R.id.progress_bar, View.GONE)
                setTextViewText(R.id.progress_detail, "${data.entries.size} kayıt")
            }
            setOnClickPendingIntent(R.id.widget_root, Widgets.openApp(context))
        }
}

/** Bugünün yazılıp yazılmadığını gösterir; düğme bugüne yeni kayıt açar. */
class TodayWidget : BaseWidget() {
    override fun render(context: Context, data: WidgetData, widthDp: Int) =
        RemoteViews(context.packageName, R.layout.widget_today).apply {
            val todays = data.entries.filter { it.date == data.today }
            setTextViewText(R.id.today_date, LocalDate.ofEpochDay(data.today).format(dayMonthFmt))
            val addLabel = if (todays.isEmpty()) "+ Yaz" else "+ Ekle"
            // Bugünün en son kaydının başlığı; başlık yoksa genel durum metni.
            val title = todays.firstOrNull { it.title.isNotBlank() }?.title
            val status = when {
                todays.isEmpty() -> "Bugün henüz yazılmadı"
                title == null -> "Bugün yazıldı"
                else -> {
                    val res = context.resources
                    val sp = res.displayMetrics.scaledDensity
                    val dp = res.displayMetrics.density
                    val titlePaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 15 * sp
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val buttonPaint = android.text.TextPaint(titlePaint).apply { textSize = 14 * sp }
                    // widget_today.xml: kenar boşlukları 16+12dp, düğme 16+16dp iç + 8dp dış boşluk.
                    val button = buttonPaint.measureText(addLabel) + 40 * dp
                    fitWords(title, titlePaint, widthDp * dp - 28 * dp - button)
                }
            }
            setTextViewText(R.id.today_status, status)
            setTextViewText(R.id.today_add, addLabel)
            // Karta dokunmak bugünün kaydını (yoksa yeni kaydı), düğme her zaman yeni kaydı açar.
            val newEntry = Widgets.openApp(context, entryRoute(0, data.today))
            val open = todays.firstOrNull()?.let { Widgets.openApp(context, entryRoute(it.id)) } ?: newEntry
            setOnClickPendingIntent(R.id.widget_root, open)
            setOnClickPendingIntent(R.id.today_add, newEntry)
        }
}

/** En son tarihli kaydın özeti; dokununca o kayıt açılır. */
class LastEntryWidget : BaseWidget() {
    override fun render(context: Context, data: WidgetData, widthDp: Int) =
        RemoteViews(context.packageName, R.layout.widget_last).apply {
            val entry = data.entries.firstOrNull()
            if (entry == null) {
                setTextViewText(R.id.last_date, "")
                setTextViewText(R.id.last_title, "Henüz kayıt yok")
                setTextViewText(R.id.last_work, "Stajda neler yaptığını yazmaya başlamak için dokun.")
                setTextViewText(R.id.last_meta, "")
                setOnClickPendingIntent(R.id.widget_root, Widgets.openApp(context, entryRoute(0, data.today)))
                return@apply
            }
            val number = dayNumbers(data.entries)[entry.date]
            val date = LocalDate.ofEpochDay(entry.date).format(shortFmt)
            setTextViewText(R.id.last_date, "$number. gün · $date")
            val heading = entry.title.ifBlank { entry.work.lineSequence().firstOrNull().orEmpty() }
            setTextViewText(R.id.last_title, heading.ifBlank { "Başlıksız kayıt" })
            setTextViewText(R.id.last_work, if (entry.title.isBlank()) "" else entry.work)
            val meta = listOf(
                entry.area,
                entry.aircraft,
            ).filter { it.isNotBlank() }.joinToString(" · ")
            setTextViewText(R.id.last_meta, meta)
            setOnClickPendingIntent(R.id.widget_root, Widgets.openApp(context, entryRoute(entry.id)))
        }
}

/** İçinde bulunulan haftanın yedi günü; kayıt olan günler dolu, bugün çerçeveli. */
class WeekWidget : BaseWidget() {
    override fun render(context: Context, data: WidgetData, widthDp: Int) =
        RemoteViews(context.packageName, R.layout.widget_week).apply {
            val monday = LocalDate.ofEpochDay(data.today).with(DayOfWeek.MONDAY)
            val byDate = data.entries.groupBy { it.date }
            removeAllViews(R.id.widget_root)
            for (offset in 0L..6L) {
                val date = monday.plusDays(offset)
                val day = date.toEpochDay()
                val entry = byDate[day]?.firstOrNull()
                val cell = RemoteViews(context.packageName, R.layout.widget_week_cell)
                cell.setTextViewText(R.id.cell_label, date.format(weekdayFmt))
                cell.setTextViewText(R.id.cell_day, date.dayOfMonth.toString())
                val background = when {
                    entry != null -> R.drawable.widget_day_filled
                    day == data.today -> R.drawable.widget_day_today
                    else -> 0
                }
                cell.setInt(R.id.cell_day, "setBackgroundResource", background)
                cell.setTextColor(
                    R.id.cell_day,
                    context.getColor(
                        when {
                            entry != null -> R.color.widget_on_primary
                            day == data.today -> R.color.widget_primary
                            else -> R.color.widget_on_surface
                        },
                    ),
                )
                // Güne dokunmak o günün kaydını, kayıt yoksa o tarihli yeni kaydı açar.
                val route = entry?.let { entryRoute(it.id) } ?: entryRoute(0, day)
                cell.setOnClickPendingIntent(R.id.cell_root, Widgets.openApp(context, route))
                addView(R.id.widget_root, cell)
            }
        }
}
