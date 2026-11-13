package com.egepaksoy.interner

import com.egepaksoy.interner.data.Backup
import com.egepaksoy.interner.data.BackupData
import com.egepaksoy.interner.data.Internship
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.Note
import com.egepaksoy.interner.data.computeStats
import com.egepaksoy.interner.data.dayNumbers
import com.egepaksoy.interner.data.diaryText
import com.egepaksoy.interner.ui.matches
import com.egepaksoy.interner.ui.weekdaysBetween
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    private val d1 = LocalDate.of(2026, 11, 9).toEpochDay() // Pazartesi
    private val entries = listOf(
        LogEntry(id = 3, date = d1 + 1, title = "Fren kontrolü", area = "Hat Bakım", aircraft = "a320", hours = 7.5),
        LogEntry(id = 2, date = d1, title = "Öğleden sonra", area = "Hat Bakım", aircraft = "A320", hours = 4.0),
        LogEntry(id = 1, date = d1, work = "Tekerlek değişimi\n\"tırnak\"", area = "Hangar / Üs Bakım", ata = "32 - İniş Takımları", hours = 4.0),
    )

    @Test
    fun statsCountDistinctDaysAndGroupCaseInsensitively() {
        val s = computeStats(entries)
        assertEquals(2, s.days)
        assertEquals(listOf("Hat Bakım" to 2, "Hangar / Üs Bakım" to 1), s.byArea)
        assertEquals(listOf("A320" to 2), s.byAircraft)
        assertEquals(mapOf(d1 to 1, d1 + 1 to 2), dayNumbers(entries))
    }

    @Test
    fun backupRoundTripsEverything() {
        val data = BackupData(
            Internship(company = "THY Teknik", department = "Hangar 2", startDate = d1, endDate = null, targetDays = 30),
            entries.map { it.copy(id = 0) },
            listOf(Note(title = "Tork", content = "Satır 1\nSatır 2 ğüşiöç", pinned = true, updatedAt = 42)),
        )
        assertEquals(data, Backup.parse(Backup.toJson(data)))
        assertNull(Backup.parse(Backup.toJson(BackupData(null, emptyList(), emptyList()))).internship)
    }

    @Test
    fun backupRejectsForeignJson() {
        assertThrows(IllegalArgumentException::class.java) { Backup.parse("""{"entries": []}""") }
    }

    @Test
    fun diaryIsChronologicalWithDayNumbers() {
        val text = diaryText(Internship(company = "THY Teknik"), entries)
        assertTrue(text.startsWith("STAJ DEFTERİ\nTHY Teknik\nToplam 2 gün\n"))
        assertTrue(text.indexOf("1. Gün — 9 Kasım 2026, Pazartesi") < text.indexOf("2. Gün — 10 Kasım 2026, Salı"))
        assertTrue(text.indexOf("Tekerlek değişimi") < text.indexOf("Öğleden sonra"))
    }

    @Test
    fun searchUsesTurkishCasing() {
        assertTrue(matches("iniş", "İNİŞ TAKIMI"))
        assertTrue(matches("ISI", "ısı eşanjörü"))
        assertFalse(matches("motor", "kanat"))
        assertTrue(matches("  ", "her şey"))
    }

    @Test
    fun xlsxExportIsAValidWorkbook() {
        val file = java.io.File("build/test-export.xlsx").apply { parentFile.mkdirs() }
        val tricky = entries + LogEntry(id = 9, date = d1 + 2, title = "A & B <test> \u0001", work = "satır1\nsatır2")
        file.outputStream().use {
            com.egepaksoy.interner.data.writeDiaryXlsx(it, tricky, listOf(Note(title = "Tork", content = "değer")))
        }
        val parts = java.util.zip.ZipFile(file).use { zip ->
            zip.entries().asSequence().associate { it.name to zip.getInputStream(it).readBytes().decodeToString() }
        }
        assertEquals(
            setOf("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
                "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml"),
            parts.keys,
        )
        // Her parça geçerli XML olmalı.
        val builder = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
        parts.values.forEach { builder.parse(it.byteInputStream()) }
        val sheet = parts.getValue("xl/worksheets/sheet1.xml")
        assertTrue(sheet.contains("A &amp; B &lt;test&gt; </t>"))
        // 9 Kasım 2026 = Excel 46335
        assertTrue(sheet.contains("<c r=\"B2\" s=\"3\"><v>46335</v></c>"))
        assertTrue(sheet.contains("<c r=\"H4\" t=\"inlineStr\" s=\"2\"><is><t xml:space=\"preserve\">a320</t></is></c>"))
        assertFalse(sheet.contains("Tescil") || sheet.contains("Süre"))
    }

    @Test
    fun weekdayCountSkipsWeekends() {
        assertEquals(5, weekdaysBetween(d1, d1 + 6))
        assertEquals(1, weekdaysBetween(d1, d1))
    }
}
