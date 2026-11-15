package com.egepaksoy.interner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tarihler `LocalDate.toEpochDay()` olarak saklanır.
 * `registration`, `reference`, `hours` ve `supervisor` artık arayüzde yok; eski kayıtlardaki
 * değerler ve yedeklerle uyum bozulmasın diye veritabanında tutuluyor.
 */
@Entity(tableName = "entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val title: String = "",
    val work: String = "",
    val learned: String = "",
    val area: String = "",
    val aircraft: String = "",
    val registration: String = "",
    val ata: String = "",
    val reference: String = "",
    val hours: Double = 0.0,
    val supervisor: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val pinned: Boolean = false,
    val updatedAt: Long = 0,
)

@Entity(tableName = "internship")
data class Internship(
    @PrimaryKey val id: Int = 1,
    val company: String = "",
    val department: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val targetDays: Int = 0,
)

val WORK_AREAS = listOf(
    "Hat Bakım",
    "Hangar / Üs Bakım",
    "C-Check",
    "Kabin",
    "Yapısal",
    "Takımhane",
    "Eğitim",
)

val AIRCRAFT_TYPES = listOf("A320", "A321", "B737")

/** Eski sürümlerde elle yazılmış tip adlarının ("a320", "737", "B 737" ...) karşılığı. */
private val AIRCRAFT_ALIASES = mapOf(
    "A320" to "A320", "AIRBUSA320" to "A320",
    "A321" to "A321", "AIRBUSA321" to "A321",
    "B737" to "B737", "737" to "B737", "BOEING737" to "B737",
)

/** Bilinen bir tipin farklı yazımını listedeki ada çevirir; bilinmeyen değer olduğu gibi kalır. */
fun normalizeAircraft(value: String): String {
    val key = value.uppercase(java.util.Locale.ROOT).filter { it.isLetterOrDigit() }
    return AIRCRAFT_ALIASES[key] ?: value.trim()
}

val ATA_CHAPTERS = listOf(
    "05 - Periyodik Kontroller",
    "07 - Kaldırma ve Destekleme",
    "09 - Çekme ve Taksi",
    "10 - Park ve Bağlama",
    "12 - Servis",
    "20 - Standart Uygulamalar",
    "21 - Klima ve Basınçlandırma",
    "22 - Otomatik Uçuş",
    "23 - Haberleşme",
    "24 - Elektrik Gücü",
    "25 - Kabin Donanımı",
    "26 - Yangın Koruma",
    "27 - Uçuş Kumandaları",
    "28 - Yakıt",
    "29 - Hidrolik Güç",
    "30 - Buz ve Yağmur Koruma",
    "31 - Gösterge ve Kayıt Sistemleri",
    "32 - İniş Takımları",
    "33 - Işıklar",
    "34 - Seyrüsefer",
    "35 - Oksijen",
    "36 - Pnömatik",
    "38 - Su ve Atık",
    "49 - APU",
    "51 - Yapısal Standartlar",
    "52 - Kapılar",
    "53 - Gövde",
    "54 - Nasel ve Pylon",
    "55 - Stabilizeler",
    "56 - Pencereler",
    "57 - Kanatlar",
    "61 - Pervaneler",
    "71 - Güç Ünitesi",
    "72 - Motor",
    "73 - Motor Yakıt ve Kontrol",
    "74 - Ateşleme",
    "75 - Motor Havası",
    "76 - Motor Kumandaları",
    "77 - Motor Göstergeleri",
    "78 - Egzoz",
    "79 - Yağ",
    "80 - Starter",
)

/** Türkçe harfleri sadeleştirir: "İniş" → "inis". Klavyede Türkçe karakter kullanmadan da arama yapılabilsin. */
private fun fold(text: String): String = text.lowercase(TR).map {
    when (it) {
        'ı' -> 'i'; 'ş' -> 's'; 'ğ' -> 'g'; 'ü' -> 'u'; 'ö' -> 'o'; 'ç' -> 'c'
        else -> it
    }
}.joinToString("").replace(Regex("\\s+"), " ").trim()

/**
 * Yazılan metne uyan ATA bölümleri, en iyi eşleşme önce:
 * numarası yazılanla başlayanlar, sonra adı yazılanla başlayanlar, sonra adında bir kelimesi
 * yazılanla başlayanlar, en son adında yazılan geçenler. Boş metinde tüm liste döner.
 */
fun searchAtaChapters(query: String): List<String> {
    val q = fold(query)
    if (q.isEmpty()) return ATA_CHAPTERS
    return ATA_CHAPTERS.mapNotNull { chapter ->
        val number = chapter.substringBefore(" -")
        val name = fold(chapter.substringAfter("- "))
        val full = fold(chapter)
        val rank = when {
            number.startsWith(q) || full.startsWith(q) -> 0
            name.startsWith(q) -> 1
            name.split(' ', '/').any { it.startsWith(q) } -> 2
            name.contains(q) -> 3
            else -> return@mapNotNull null
        }
        rank to chapter
    }.sortedBy { it.first }.map { it.second }
}
