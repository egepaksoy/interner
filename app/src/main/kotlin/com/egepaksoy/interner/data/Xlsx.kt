package com.egepaksoy.interner.data

import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Harici kütüphane kullanmadan küçük bir .xlsx (Excel) dosyası yazar.
 * Hücre değerleri: [String] metin, [Number] sayı, [LocalDate] gerçek Excel tarihi olur.
 */
object Xlsx {
    class Sheet(val name: String, val widths: List<Int>, val header: List<String>, val rows: List<List<Any?>>)

    const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    private const val HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
    private const val MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val CT = "application/vnd.openxmlformats-officedocument.spreadsheetml"

    // Hücre biçimleri (styles.xml içindeki cellXfs sırası)
    private const val S_HEADER = 1
    private const val S_TEXT = 2
    private const val S_DATE = 3
    private const val S_NUMBER = 4

    /** Excel'in tarih sayısı: 1970-01-01 = 25569. */
    private const val EXCEL_EPOCH_OFFSET = 25569L

    fun write(out: OutputStream, sheets: List<Sheet>) {
        ZipOutputStream(out).use { zip ->
            fun put(path: String, body: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write((HEAD + body).toByteArray())
                zip.closeEntry()
            }
            put("[Content_Types].xml", buildString {
                append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"$CT.sheet.main+xml\"/>")
                append("<Override PartName=\"/xl/styles.xml\" ContentType=\"$CT.styles+xml\"/>")
                sheets.indices.forEach {
                    append("<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" ContentType=\"$CT.worksheet+xml\"/>")
                }
                append("</Types>")
            })
            put(
                "_rels/.rels",
                "<Relationships xmlns=\"$PKG_REL\">" +
                    "<Relationship Id=\"rId1\" Type=\"$REL/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>",
            )
            put("xl/workbook.xml", buildString {
                append("<workbook xmlns=\"$MAIN\" xmlns:r=\"$REL\"><sheets>")
                sheets.forEachIndexed { i, s ->
                    append("<sheet name=\"${escape(s.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
                }
                append("</sheets></workbook>")
            })
            put("xl/_rels/workbook.xml.rels", buildString {
                append("<Relationships xmlns=\"$PKG_REL\">")
                sheets.indices.forEach {
                    append("<Relationship Id=\"rId${it + 1}\" Type=\"$REL/worksheet\" Target=\"worksheets/sheet${it + 1}.xml\"/>")
                }
                append("<Relationship Id=\"rId${sheets.size + 1}\" Type=\"$REL/styles\" Target=\"styles.xml\"/>")
                append("</Relationships>")
            })
            put("xl/styles.xml", STYLES)
            sheets.forEachIndexed { i, s -> put("xl/worksheets/sheet${i + 1}.xml", sheetXml(s)) }
        }
    }

    private val STYLES = """
        <styleSheet xmlns="$MAIN">
        <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
        <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFD5E3FF"/><bgColor indexed="64"/></patternFill></fill></fills>
        <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="5">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>
        <xf numFmtId="14" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment vertical="top" horizontal="left"/></xf>
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0" applyAlignment="1"><alignment vertical="top"/></xf>
        </cellXfs>
        </styleSheet>
    """.trimIndent().replace("\n", "")

    private fun sheetXml(sheet: Sheet) = buildString {
        append("<worksheet xmlns=\"$MAIN\">")
        append("<sheetViews><sheetView workbookViewId=\"0\">")
        append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
        append("</sheetView></sheetViews><cols>")
        sheet.widths.forEachIndexed { i, w ->
            append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
        }
        append("</cols><sheetData>")
        row(1, sheet.header, header = true)
        sheet.rows.forEachIndexed { i, r -> row(i + 2, r, header = false) }
        append("</sheetData></worksheet>")
    }

    private fun StringBuilder.row(number: Int, cells: List<Any?>, header: Boolean) {
        append("<row r=\"$number\">")
        cells.forEachIndexed { i, value ->
            val ref = "${columnName(i)}$number"
            when {
                value == null || value == "" -> Unit
                header -> text(ref, value.toString(), S_HEADER)
                value is LocalDate ->
                    append("<c r=\"$ref\" s=\"$S_DATE\"><v>${value.toEpochDay() + EXCEL_EPOCH_OFFSET}</v></c>")
                value is Number -> append("<c r=\"$ref\" s=\"$S_NUMBER\"><v>$value</v></c>")
                else -> text(ref, value.toString(), S_TEXT)
            }
        }
        append("</row>")
    }

    private fun StringBuilder.text(ref: String, value: String, style: Int) {
        append("<c r=\"$ref\" t=\"inlineStr\" s=\"$style\"><is><t xml:space=\"preserve\">")
        append(escape(value))
        append("</t></is></c>")
    }

    private fun columnName(index: Int): String =
        if (index < 26) ('A' + index).toString() else columnName(index / 26 - 1) + ('A' + index % 26)

    /** XML'de geçersiz kontrol karakterlerini atar, özel karakterleri kaçışlar. */
    private fun escape(s: String) = buildString {
        for (c in s) when {
            c == '&' -> append("&amp;")
            c == '<' -> append("&lt;")
            c == '>' -> append("&gt;")
            c == '"' -> append("&quot;")
            c == '\n' || c == '\t' -> append(c)
            c < ' ' || c == '￾' || c == '￿' -> Unit
            else -> append(c)
        }
    }
}

private val weekdayFmt = DateTimeFormatter.ofPattern("EEEE", TR)

/** Staj defterini Excel dosyası olarak yazar: "Günlük" ve (varsa) "Notlar" sayfaları. */
fun writeDiaryXlsx(out: OutputStream, entries: List<LogEntry>, notes: List<Note>) {
    val numbers = dayNumbers(entries)
    val log = Xlsx.Sheet(
        name = "Günlük",
        widths = listOf(7, 12, 12, 30, 50, 40, 20, 12, 12, 24, 22, 8, 22),
        header = listOf(
            "Gün", "Tarih", "Haftanın günü", "Başlık", "Yapılan işler", "Öğrendiklerim / notlar",
            "Çalışma alanı", "Uçak tipi", "Tescil", "ATA bölümü", "Referans doküman", "Süre (saat)",
            "Sorumlu teknisyen",
        ),
        rows = entries.sortedWith(compareBy({ it.date }, { it.id })).map { e ->
            val date = LocalDate.ofEpochDay(e.date)
            listOf(
                numbers[e.date], date, date.format(weekdayFmt), e.title, e.work, e.learned, e.area,
                e.aircraft, e.registration, e.ata, e.reference, e.hours.takeIf { it > 0 }, e.supervisor,
            )
        },
    )
    val noteSheet = Xlsx.Sheet(
        name = "Notlar",
        widths = listOf(30, 80),
        header = listOf("Başlık", "Not"),
        rows = notes.map { listOf(it.title, it.content) },
    )
    Xlsx.write(out, if (notes.isEmpty()) listOf(log) else listOf(log, noteSheet))
}
