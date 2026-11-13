package com.egepaksoy.interner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.TR
import com.egepaksoy.interner.data.dayNumbers
import com.egepaksoy.interner.data.formatHours
import com.egepaksoy.interner.data.fullDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", TR)
private val tableDateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy EEE", TR)

// ---------------------------------------------------------------- Tablo

private class Column(val title: String, val width: Dp, val value: (LogEntry, Int) -> String)

private val columns = listOf(
    Column("Gün", 48.dp) { _, n -> n.toString() },
    Column("Tarih", 116.dp) { e, _ -> LocalDate.ofEpochDay(e.date).format(tableDateFmt) },
    Column("Başlık", 180.dp) { e, _ -> e.title },
    Column("Yapılan işler", 260.dp) { e, _ -> e.work },
    Column("Alan", 140.dp) { e, _ -> e.area },
    Column("Uçak", 110.dp) { e, _ -> listOf(e.aircraft, e.registration).filter { it.isNotBlank() }.joinToString("\n") },
    Column("ATA", 56.dp) { e, _ -> e.ata.substringBefore(" -") },
    Column("Saat", 56.dp) { e, _ -> if (e.hours > 0) formatHours(e.hours) else "" },
)

/** Kayıtları eskiden yeniye, yatay kaydırılabilen bir tabloda gösterir. */
@Composable
fun LogTable(entries: List<LogEntry>, pad: PaddingValues, onOpen: (Long) -> Unit) {
    if (entries.isEmpty()) {
        EmptyState(Icons.Default.DateRange, "Henüz kayıt yok", "Kayıt ekledikçe burada tablo halinde görünür.")
        return
    }
    val numbers = remember(entries) { dayNumbers(entries) }
    val rows = remember(entries) { entries.sortedWith(compareBy({ it.date }, { it.id })) }
    val totalWidth = columns.fold(0.dp) { sum, c -> sum + c.width } + 24.dp

    Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        Column(Modifier.width(totalWidth)) {
            TableRow(
                modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                bold = true,
            ) { it.title }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = pad.calculateBottomPadding() + 88.dp),
            ) {
                items(rows, key = { it.id }) { entry ->
                    TableRow(Modifier.clickable { onOpen(entry.id) }) { it.value(entry, numbers[entry.date] ?: 0) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                item(key = "total") {
                    val days = numbers.size
                    val hours = formatHours(entries.sumOf { it.hours })
                    Text(
                        "Toplam: $days gün, $hours saat",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
    text: (Column) -> String,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
        columns.forEach { column ->
            Text(
                text(column),
                modifier = Modifier.width(column.width).padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (bold) FontWeight.SemiBold else null,
                color = color,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------- Takvim

private val weekdays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

/** Aylık takvim: kayıt olan günler işaretlidir, bir güne dokununca o günün kayıtları altta listelenir. */
@Composable
fun LogCalendar(
    entries: List<LogEntry>,
    pad: PaddingValues,
    onOpen: (Long) -> Unit,
    onNew: (Long) -> Unit,
) {
    val today = today()
    var selected by rememberSaveable { mutableLongStateOf(today) }
    var monthIndex by rememberSaveable {
        mutableIntStateOf(LocalDate.ofEpochDay(today).let { it.year * 12 + it.monthValue - 1 })
    }
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val byDate = remember(entries) { entries.groupBy { it.date } }
    val numbers = remember(entries) { dayNumbers(entries) }
    val monthEntries = entries.filter { YearMonth.from(LocalDate.ofEpochDay(it.date)) == month }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = pad.calculateBottomPadding() + 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { monthIndex-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Önceki ay")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    month.format(monthFmt).replaceFirstChar { it.titlecase(TR) },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${monthEntries.map { it.date }.distinct().size} gün · " +
                        "${formatHours(monthEntries.sumOf { it.hours })} saat",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { monthIndex++ }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sonraki ay")
            }
        }

        Row {
            weekdays.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // Haftalar pazartesiden başlar; ayın ilk gününden önceki hücreler boş kalır.
        val lead = month.atDay(1).dayOfWeek.value - 1
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it).toEpochDay() }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (day != null) {
                            DayCell(
                                day = day,
                                hours = byDate[day]?.sumOf { it.hours },
                                selected = day == selected,
                                today = day == today,
                                onClick = { selected = day },
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }

        SectionTitle(fullDate(selected), Modifier.padding(top = 8.dp, start = 4.dp))
        val dayEntries = byDate[selected].orEmpty()
        if (dayEntries.isEmpty()) {
            Text(
                "Bu gün için kayıt yok.",
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { onNew(selected) }, modifier = Modifier.fillMaxWidth()) {
                Text("Bu güne kayıt ekle")
            }
        } else {
            dayEntries.forEach { entry ->
                EntryCard(entry, numbers[entry.date] ?: 0) { onOpen(entry.id) }
            }
            TextButton(onClick = { onNew(selected) }) { Text("Bu güne bir kayıt daha ekle") }
        }
    }
}

@Composable
private fun DayCell(day: Long, hours: Double?, selected: Boolean, today: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val hasEntry = hours != null
    val background = when {
        selected -> scheme.primary
        hasEntry -> scheme.primaryContainer
        else -> Color.Transparent
    }
    val content = when {
        selected -> scheme.onPrimary
        hasEntry -> scheme.onPrimaryContainer
        else -> scheme.onSurface
    }
    val number = LocalDate.ofEpochDay(day).dayOfMonth
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(background)
            .then(if (today && !selected) Modifier.border(1.5.dp, scheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = fullDate(day) + if (hasEntry) ", kayıt var" else ""
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (hasEntry || today) FontWeight.Bold else null,
            color = content,
        )
        if (hours != null && hours > 0) {
            Text("${formatHours(hours)} sa", style = MaterialTheme.typography.labelSmall, color = content)
        }
    }
}
