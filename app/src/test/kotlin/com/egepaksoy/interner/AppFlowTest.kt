package com.egepaksoy.interner

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.Note
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Uygulamayı gerçek veritabanıyla baştan sona kullanır; ekran görüntüleri build/screenshots altına yazılır. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "tr-rTR-w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val dao get() = (rule.activity.application as InternerApp).db.dao()

    private fun shot(name: String) {
        System.setProperty("robolectric.pixelCopyRenderMode", "hardware")
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(rule.activity.window, bitmap, {}, Handler(Looper.getMainLooper()))
        shadowOf(Looper.getMainLooper()).idle()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitForText(text: String) = rule.waitUntil(5_000) {
        rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun widgetIntentOpensNewEntryForThatDate() {
        waitForText("Henüz kayıt yok")
        val date = LocalDate.of(2026, 3, 9).toEpochDay()
        val intent = android.content.Intent(rule.activity, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_ROUTE, com.egepaksoy.interner.ui.entryRoute(0, date))
        rule.runOnUiThread { rule.activity.onNewIntent(intent) }
        waitForText("Yeni kayıt")
        rule.onNodeWithText("9 Mart 2026, Pazartesi").assertIsDisplayed()

        // Tanınmayan rota yok sayılır, uygulama çökmez.
        rule.runOnUiThread {
            rule.activity.onNewIntent(android.content.Intent().putExtra(MainActivity.EXTRA_ROUTE, "olmayan/ekran"))
        }
        rule.waitForIdle()
        rule.onNodeWithText("Yeni kayıt").assertIsDisplayed()
    }

    @Test
    fun oldLowercaseAircraftDoesNotShowAsExtraChoice() {
        waitForText("Henüz kayıt yok")
        runBlocking { dao.insertEntries(listOf(LogEntry(date = LocalDate.now().toEpochDay() - 1, title = "Eski kayıt", aircraft = "a320"))) }
        waitForText("Eski kayıt")
        rule.onNodeWithText("Gün ekle", useUnmergedTree = true).performClick()
        waitForText("Uçak tipi")
        // Tek bir A320 seçeneği var, seçili geliyor; küçük harfli kopya yok.
        rule.onNodeWithText("A320").performScrollTo().assertIsSelected()
        assertEquals(0, rule.onAllNodesWithText("a320").fetchSemanticsNodes().size)
        rule.onNodeWithText("B737").performScrollTo().performClick()
        rule.onNodeWithText("A320").assertIsNotSelected()
    }

    @Test
    fun writeADayTakeANoteAndSeeTheSummary() {
        waitForText("Henüz kayıt yok")
        shot("1-bos")

        // Staj bilgileri
        rule.onNodeWithText("Staj bilgilerini ekle").performClick()
        rule.onNodeWithText("Kurum / bakım kuruluşu").performTextInput("Örnek Havacılık Teknik")
        rule.onNodeWithText("Hedef staj günü sayısı").performTextInput("30")
        rule.onNodeWithText("Kaydet").performClick()
        waitForText("/ 30 gün")
        rule.onNodeWithText("Örnek Havacılık Teknik").assertIsDisplayed()

        // Günlük kaydı
        rule.onNodeWithText("Gün ekle", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Başlık").performTextInput("Ana iniş takımı tekerlek değişimi")
        rule.onNodeWithText("Yapılan işler")
            .performTextInput("Uçak krikoya alındı, 3 numaralı tekerlek söküldü ve yenisi takıldı.")
        rule.onNodeWithText("Hat Bakım").performScrollTo().performClick()
        rule.onNodeWithText("A320").performScrollTo().performClick()
        rule.onNodeWithContentDescription("ATA bölümü listesi").performScrollTo().performClick()
        rule.onNodeWithText("32 - İniş Takımları").performScrollTo().performClick()
        shot("2-kayit-formu")
        rule.onNodeWithText("Kaydet").performClick()

        waitForText("1. gün")
        rule.onNodeWithText("Ana iniş takımı tekerlek değişimi").assertIsDisplayed()
        val saved = runBlocking { dao.allEntries() }.single()
        assertEquals(LocalDate.now().toEpochDay(), saved.date)
        assertEquals("32 - İniş Takımları", saved.ata)
        assertEquals("A320", saved.aircraft)

        // Kaydı düzenle
        // Eski sürümden kalan gizli alanlar düzenlemede silinmemeli.
        runBlocking { dao.upsert(saved.copy(registration = "TC-ABC", hours = 7.5, supervisor = "Usta")) }
        rule.onNodeWithText("Ana iniş takımı tekerlek değişimi").performClick()
        waitForText("Kaydı düzenle")
        rule.onNodeWithText("A321").performScrollTo().performClick()
        rule.onNodeWithText("Kaydet").performClick()
        waitForText("1. gün")
        val edited = runBlocking { dao.allEntries() }.single()
        assertEquals("A321", edited.aircraft)
        assertEquals(Triple("TC-ABC", 7.5, "Usta"), Triple(edited.registration, edited.hours, edited.supervisor))

        // Liste görünümü için birkaç eski kayıt
        val today = LocalDate.now().toEpochDay()
        runBlocking {
            dao.insertEntries(
                listOf(
                    LogEntry(date = today - 1, title = "APU yağ seviyesi kontrolü", work = "Servis panelinden seviye okundu, yağ ikmali yapıldı.", area = "Hat Bakım", aircraft = "B737", registration = "TC-XYZ", ata = "49 - APU", hours = 8.0),
                    LogEntry(date = today - 2, title = "Boroskop muayenesi", work = "HPC kademelerinde boroskop ile hasar kontrolü izlendi.", area = "C-Check", aircraft = "A320", ata = "72 - Motor", hours = 6.0),
                    LogEntry(date = today - 40, title = "Emniyet ve hangar oryantasyonu", area = "Eğitim", hours = 8.0),
                ),
            )
        }
        waitForText("Boroskop muayenesi")
        shot("3-gunluk")

        // Tablo ve takvim görünümleri
        rule.onNodeWithText("Tablo").performClick()
        waitForText("Toplam: 4 gün, 4 kayıt")
        rule.onNodeWithText("Yapılan işler").assertIsDisplayed()
        shot("6-tablo")
        rule.onNodeWithText("Takvim").performClick()
        waitForText("Bu güne bir kayıt daha ekle")
        rule.onNodeWithText("Ana iniş takımı tekerlek değişimi").assertIsDisplayed()
        shot("7-takvim")
        // Önceki aydan boş bir güne takvimden kayıt ekle: tarih o gün olmalı.
        rule.onNodeWithContentDescription("Önceki ay").performClick()
        val emptyDay = LocalDate.now().minusMonths(1).withDayOfMonth(15).let {
            if (it.toEpochDay() == today - 40) it.plusDays(1) else it
        }
        rule.onNodeWithContentDescription(com.egepaksoy.interner.data.fullDate(emptyDay.toEpochDay())).performClick()
        rule.onNodeWithText("Bu güne kayıt ekle").performScrollTo().performClick()
        waitForText("Yeni kayıt")
        rule.onNodeWithText("Başlık").performTextInput("Takvimden eklenen kayıt")
        rule.onNodeWithText("Kaydet").performClick()
        waitForText("Bu güne bir kayıt daha ekle")
        assertEquals(
            emptyDay.toEpochDay(),
            runBlocking { dao.allEntries() }.single { it.title == "Takvimden eklenen kayıt" }.date,
        )
        rule.onNodeWithText("Liste").performClick()
        waitForText("Kayıtlarda ara")

        rule.onNodeWithText("Kayıtlarda ara").performTextInput("boroskop")
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("APU yağ seviyesi kontrolü").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("Boroskop muayenesi").assertIsDisplayed()

        // Notlar
        rule.onNodeWithText("Notlar").performClick()
        waitForText("Henüz not yok")
        rule.onNodeWithText("Not ekle", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Başlık").performTextInput("Tork değerleri")
        rule.onNodeWithText("Notunu yaz...").performTextInput("Tekerlek aks somunu: AMM'den kontrol et.")
        rule.onNodeWithContentDescription("Kaydet ve geri dön").performClick()
        waitForText("Tork değerleri")
        runBlocking { dao.upsert(Note(title = "Sorulacaklar", content = "Kalibrasyon etiketi nasıl okunur?", updatedAt = 1)) }
        waitForText("Sorulacaklar")
        shot("4-notlar")
        assertEquals(2, runBlocking { dao.allNotes() }.size)

        // Özet
        rule.onNodeWithText("Özet").performClick()
        waitForText("Kayıtlı gün")
        rule.onNodeWithText("25 gün kaldı", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Toplam kayıt").assertIsDisplayed()
        shot("5-ozet")
    }
}
