package com.egepaksoy.interner.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val TR: Locale = Locale.forLanguageTag("tr-TR")

private val fullFmt = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", TR)

fun fullDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(fullFmt)

fun formatHours(h: Double): String =
    if (h % 1.0 == 0.0) h.toLong().toString() else String.format(TR, "%.1f", h)

/** Kayıt olan her tarihe kronolojik sıra numarası verir (1. gün, 2. gün...). */
fun dayNumbers(entries: List<LogEntry>): Map<Long, Int> =
    entries.map { it.date }.distinct().sorted().withIndex().associate { (i, d) -> d to i + 1 }

data class Stats(
    val days: Int,
    val totalHours: Double,
    val byArea: List<Pair<String, Int>>,
    val byAircraft: List<Pair<String, Int>>,
    val byAta: List<Pair<String, Int>>,
)

fun computeStats(entries: List<LogEntry>): Stats {
    fun count(key: (LogEntry) -> String) = entries
        .map { key(it).trim() }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    return Stats(
        days = entries.map { it.date }.distinct().size,
        totalHours = entries.sumOf { it.hours },
        byArea = count { it.area },
        byAircraft = count { it.aircraft.uppercase(TR) },
        byAta = count { it.ata },
    )
}

/** Staj defterine aktarmak için tüm kayıtları düz metne çevirir (eskiden yeniye). */
fun diaryText(internship: Internship?, entries: List<LogEntry>): String = buildString {
    appendLine("STAJ DEFTERİ")
    internship?.let {
        if (it.company.isNotBlank()) appendLine(it.company)
        if (it.department.isNotBlank()) appendLine(it.department)
    }
    val stats = computeStats(entries)
    appendLine("Toplam ${stats.days} gün, ${formatHours(stats.totalHours)} saat")
    val numbers = dayNumbers(entries)
    for (e in entries.sortedWith(compareBy({ it.date }, { it.id }))) {
        appendLine()
        appendLine("${numbers[e.date]}. Gün — ${fullDate(e.date)}")
        if (e.title.isNotBlank()) appendLine(e.title)
        if (e.area.isNotBlank()) appendLine("Çalışma alanı: ${e.area}")
        val aircraft = listOf(e.aircraft, e.registration).filter { it.isNotBlank() }.joinToString(" / ")
        if (aircraft.isNotBlank()) appendLine("Uçak: $aircraft")
        if (e.ata.isNotBlank()) appendLine("ATA: ${e.ata}")
        if (e.reference.isNotBlank()) appendLine("Referans: ${e.reference}")
        if (e.hours > 0) appendLine("Süre: ${formatHours(e.hours)} saat")
        if (e.supervisor.isNotBlank()) appendLine("Sorumlu: ${e.supervisor}")
        if (e.work.isNotBlank()) {
            appendLine("Yapılan işler:")
            appendLine(e.work.trim())
        }
        if (e.learned.isNotBlank()) {
            appendLine("Öğrendiklerim:")
            appendLine(e.learned.trim())
        }
    }
}.trimEnd()

data class BackupData(
    val internship: Internship?,
    val entries: List<LogEntry>,
    val notes: List<Note>,
)

object Backup {
    private const val FORMAT = "interner-backup"

    fun toJson(data: BackupData): String = JSONObject().apply {
        put("format", FORMAT)
        put("version", 1)
        data.internship?.let {
            put("internship", JSONObject().apply {
                put("company", it.company)
                put("department", it.department)
                put("startDate", it.startDate ?: JSONObject.NULL)
                put("endDate", it.endDate ?: JSONObject.NULL)
                put("targetDays", it.targetDays)
            })
        }
        put("entries", JSONArray().apply {
            data.entries.forEach { e ->
                put(JSONObject().apply {
                    put("date", e.date)
                    put("title", e.title)
                    put("work", e.work)
                    put("learned", e.learned)
                    put("area", e.area)
                    put("aircraft", e.aircraft)
                    put("registration", e.registration)
                    put("ata", e.ata)
                    put("reference", e.reference)
                    put("hours", e.hours)
                    put("supervisor", e.supervisor)
                    put("createdAt", e.createdAt)
                    put("updatedAt", e.updatedAt)
                })
            }
        })
        put("notes", JSONArray().apply {
            data.notes.forEach { n ->
                put(JSONObject().apply {
                    put("title", n.title)
                    put("content", n.content)
                    put("pinned", n.pinned)
                    put("updatedAt", n.updatedAt)
                })
            }
        })
    }.toString(2)

    /** Geçersiz dosyada [IllegalArgumentException] veya `JSONException` fırlatır. */
    fun parse(text: String): BackupData {
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "Bu dosya bir Staj Defteri yedeği değil" }
        val internship = root.optJSONObject("internship")?.let {
            Internship(
                company = it.optString("company"),
                department = it.optString("department"),
                startDate = if (it.isNull("startDate")) null else it.getLong("startDate"),
                endDate = if (it.isNull("endDate")) null else it.getLong("endDate"),
                targetDays = it.optInt("targetDays"),
            )
        }
        val entries = root.optJSONArray("entries").objects().map {
            LogEntry(
                date = it.getLong("date"),
                title = it.optString("title"),
                work = it.optString("work"),
                learned = it.optString("learned"),
                area = it.optString("area"),
                aircraft = it.optString("aircraft"),
                registration = it.optString("registration"),
                ata = it.optString("ata"),
                reference = it.optString("reference"),
                hours = it.optDouble("hours", 0.0),
                supervisor = it.optString("supervisor"),
                createdAt = it.optLong("createdAt"),
                updatedAt = it.optLong("updatedAt"),
            )
        }
        val notes = root.optJSONArray("notes").objects().map {
            Note(
                title = it.optString("title"),
                content = it.optString("content"),
                pinned = it.optBoolean("pinned"),
                updatedAt = it.optLong("updatedAt"),
            )
        }
        return BackupData(internship, entries, notes)
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }
}
