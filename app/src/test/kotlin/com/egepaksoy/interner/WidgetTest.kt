package com.egepaksoy.interner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.egepaksoy.interner.data.Internship
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.widget.LastEntryWidget
import com.egepaksoy.interner.widget.ProgressWidget
import com.egepaksoy.interner.widget.TodayWidget
import com.egepaksoy.interner.widget.WeekWidget
import com.egepaksoy.interner.widget.WidgetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Widget'ları örnek veriyle çizer; görüntüler build/screenshots altına yazılır. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "tr-rTR-w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.of(2026, 11, 13).toEpochDay() // Cuma
    private val data = WidgetData(
        Internship(company = "Örnek Havacılık Teknik", targetDays = 30),
        listOf(
            LogEntry(id = 3, date = today, title = "Ana iniş takımı tekerlek değişimi", work = "Uçak krikoya alındı, 3 numaralı tekerlek söküldü ve yenisi takıldı.", area = "Hat Bakım", aircraft = "A320", hours = 7.5),
            LogEntry(id = 2, date = today - 2, title = "Boroskop", area = "C-Check", hours = 6.0),
            LogEntry(id = 1, date = today - 3, title = "Oryantasyon", area = "Eğitim", hours = 8.0),
        ),
        today,
    )
    private val empty = WidgetData(null, emptyList(), today)

    private fun show(views: RemoteViews, widthDp: Int, heightDp: Int, name: String): View {
        val density = context.resources.displayMetrics.density
        val w = (widthDp * density).toInt()
        val h = (heightDp * density).toInt()
        val view = views.apply(context, FrameLayout(context))
        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, w, h)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply { drawColor(Color.rgb(96, 125, 139)) }.also(view::draw)
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "widget-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return view
    }

    private fun View.text(id: Int) = findViewById<TextView>(id).text.toString()

    @Test
    fun progressWidget() {
        val view = show(ProgressWidget().render(context, data), 170, 170, "ilerleme")
        assertEquals("Örnek Havacılık Teknik", view.text(R.id.progress_company))
        assertEquals("3", view.text(R.id.progress_days))
        assertEquals("/ 30 gün", view.text(R.id.progress_target))
        assertEquals("27 gün kaldı", view.text(R.id.progress_detail))
        val bar = view.findViewById<ProgressBar>(R.id.progress_bar)
        assertEquals(30 to 3, bar.max to bar.progress)

        val none = show(ProgressWidget().render(context, empty), 170, 170, "ilerleme-bos")
        assertEquals("0", none.text(R.id.progress_days))
        assertEquals(View.GONE, none.findViewById<View>(R.id.progress_bar).visibility)
    }

    @Test
    fun todayWidget() {
        // Geniş widget: başlık tamamen sığar.
        val wide = show(TodayWidget().render(context, data, 400), 400, 80, "bugun-genis")
        assertEquals("13 Kasım, Cuma", wide.text(R.id.today_date))
        assertEquals("Ana iniş takımı tekerlek değişimi", wide.text(R.id.today_status))

        // Dar widget: sığan kelimeler kalır, sonu "…" ile biter, kelime ortadan bölünmez.
        val view = show(TodayWidget().render(context, data, 260), 260, 80, "bugun")
        val shown = view.text(R.id.today_status)
        assertTrue(shown, shown.endsWith("…"))
        assertTrue(shown, "Ana iniş takımı tekerlek değişimi".startsWith(shown.removeSuffix("…")))
        assertTrue(shown, shown.removeSuffix("…").split(" ").all { it in listOf("Ana", "iniş", "takımı", "tekerlek", "değişimi") })
        val status = view.findViewById<TextView>(R.id.today_status)
        assertTrue(status.paint.measureText(shown) <= status.width)

        // Başlıksız kayıt: "Bugün yazıldı".
        val untitled = WidgetData(null, listOf(LogEntry(id = 5, date = today, work = "Sadece iş metni")), today)
        assertEquals("Bugün yazıldı", show(TodayWidget().render(context, untitled), 260, 80, "bugun-basliksiz").text(R.id.today_status))
        val none = show(TodayWidget().render(context, empty), 260, 80, "bugun-bos")
        assertEquals("Bugün henüz yazılmadı", none.text(R.id.today_status))
        assertEquals("+ Yaz", none.text(R.id.today_add))
    }

    @Test
    fun lastEntryWidget() {
        val view = show(LastEntryWidget().render(context, data), 350, 170, "son-kayit")
        assertEquals("3. gün · 13 Kas", view.text(R.id.last_date))
        assertEquals("Ana iniş takımı tekerlek değişimi", view.text(R.id.last_title))
        assertEquals("Hat Bakım · A320", view.text(R.id.last_meta))
        assertEquals("Henüz kayıt yok", show(LastEntryWidget().render(context, empty), 350, 170, "son-kayit-bos").text(R.id.last_title))
    }

    @Test
    fun weekWidgetShowsMondayToSunday() {
        val view = show(WeekWidget().render(context, data), 350, 80, "hafta") as android.view.ViewGroup
        assertEquals(7, view.childCount)
        val days = (0 until 7).map { view.getChildAt(it).text(R.id.cell_day) }
        assertEquals(listOf("9", "10", "11", "12", "13", "14", "15"), days)
        assertEquals("Pzt", view.getChildAt(0).text(R.id.cell_label))
    }
}
