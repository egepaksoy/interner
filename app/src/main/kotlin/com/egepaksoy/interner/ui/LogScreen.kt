package com.egepaksoy.interner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.egepaksoy.interner.data.Internship
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.TR
import com.egepaksoy.interner.data.computeStats
import com.egepaksoy.interner.data.dayNumbers
import com.egepaksoy.interner.ui.theme.areaColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", TR)
private val weekdayFmt = DateTimeFormatter.ofPattern("EEE", TR)
private val monthShortFmt = DateTimeFormatter.ofPattern("MMM", TR)

private val LOG_VIEWS = listOf("Liste", "Tablo", "Takvim")

/** Alt çubuk ve "ekle" düğmesi içeriği kapatmasın diye liste sonuna eklenen boşluk. */
@Composable
fun listPadding(pad: PaddingValues): PaddingValues {
    val dir = LocalLayoutDirection.current
    return PaddingValues(
        start = pad.calculateStartPadding(dir) + 16.dp,
        end = pad.calculateEndPadding(dir) + 16.dp,
        top = pad.calculateTopPadding() + 8.dp,
        bottom = pad.calculateBottomPadding() + 88.dp,
    )
}

@Composable
fun LogScreen(
    entries: List<LogEntry>,
    internship: Internship?,
    pad: PaddingValues,
    onOpen: (Long) -> Unit,
    onNew: (Long) -> Unit,
    onInternship: () -> Unit,
) {
    var view by rememberSaveable { mutableIntStateOf(0) }
    // Üst boşluğu burada, alt boşluğu (alt çubuk) her görünüm kendi içinde uygular.
    val inner = PaddingValues(bottom = pad.calculateBottomPadding())
    Column(Modifier.fillMaxSize().padding(top = pad.calculateTopPadding())) {
        ViewSwitcher(
            LOG_VIEWS, view, { view = it },
            Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
        )
        when (view) {
            0 -> LogList(entries, internship, inner, onOpen, onInternship)
            1 -> LogTable(entries, inner, onOpen)
            else -> LogCalendar(entries, inner, onOpen, onNew)
        }
    }
}

@Composable
private fun LogList(
    entries: List<LogEntry>,
    internship: Internship?,
    pad: PaddingValues,
    onOpen: (Long) -> Unit,
    onInternship: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val numbers = remember(entries) { dayNumbers(entries) }
    val stats = remember(entries) { computeStats(entries) }
    val groups = remember(entries, query) {
        entries
            .filter {
                matches(
                    query, it.title, it.work, it.learned, it.area, it.aircraft,
                    it.ata,
                )
            }
            .groupBy { YearMonth.from(LocalDate.ofEpochDay(it.date)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding(pad),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "progress") { InternshipHero(internship, stats.days, onInternship) }
        if (entries.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    Icons.Default.DateRange,
                    "Henüz kayıt yok",
                    "Stajda bugün neler yaptığını yazmak için \"Gün ekle\"ye dokun.",
                )
            }
        } else {
            item(key = "search") {
                SearchField(query, { query = it }, "Kayıtlarda ara", Modifier.padding(top = 4.dp))
            }
            if (groups.isEmpty()) {
                item(key = "none") {
                    EmptyState(Icons.Default.Search, "Sonuç yok", "\"$query\" ile eşleşen kayıt bulunamadı.")
                }
            }
            groups.forEach { (month, list) ->
                item(key = "m$month") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            month.format(monthFmt).replaceFirstChar { it.titlecase(TR) },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${list.size} kayıt",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(list, key = { it.id }) { entry ->
                    EntryCard(entry, numbers[entry.date] ?: 0) { onOpen(entry.id) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryCard(entry: LogEntry, dayNumber: Int, onClick: () -> Unit) {
    val date = LocalDate.ofEpochDay(entry.date)
    val accent = areaColor(entry.area)
    AppCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            // Sol kenarda çalışma alanının renginde ince şerit.
            Modifier
                .drawBehind { drawRect(accent, size = Size(5.dp.toPx(), size.height)) }
                .padding(start = 17.dp, top = 14.dp, end = 14.dp, bottom = 14.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(accent.copy(alpha = 0.10f))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    date.format(weekdayFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    date.format(monthShortFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val heading = entry.title.ifBlank { entry.work.lineSequence().firstOrNull().orEmpty() }
                Text(
                    heading.ifBlank { "Başlıksız kayıt" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.title.isNotBlank() && entry.work.isNotBlank()) {
                    Text(
                        entry.work,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Pill(
                        "$dayNumber. gün",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    if (entry.area.isNotBlank()) Pill(entry.area, accent.copy(alpha = 0.14f), accent)
                    if (entry.aircraft.isNotBlank()) Pill(entry.aircraft)
                    if (entry.ata.isNotBlank()) Pill("ATA ${entry.ata.substringBefore(" -")}")
                }
            }
        }
    }
}
