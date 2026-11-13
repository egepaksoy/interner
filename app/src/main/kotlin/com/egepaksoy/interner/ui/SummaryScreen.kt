package com.egepaksoy.interner.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.egepaksoy.interner.data.Internship
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.computeStats
import com.egepaksoy.interner.data.diaryText
import com.egepaksoy.interner.data.formatHours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SummaryScreen(
    vm: AppViewModel,
    entries: List<LogEntry>,
    internship: Internship?,
    pad: PaddingValues,
    onInternship: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stats = remember(entries) { computeStats(entries) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val backup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = vm.backupJson()
                withContext(Dispatchers.IO) { context.writeText(uri, json) }
            }.fold({ toast("Yedek kaydedildi") }, { toast("Yedek kaydedilemedi: ${it.message}") })
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(listPadding(pad)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InternshipCard(internship, stats.days, onInternship)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Kayıtlı gün", stats.days.toString(), Modifier.weight(1f))
            StatTile("Toplam saat", formatHours(stats.totalHours), Modifier.weight(1f))
            StatTile(
                "Günlük ort.",
                if (stats.days > 0) formatHours(stats.totalHours / stats.days) + " sa" else "–",
                Modifier.weight(1f),
            )
        }

        Breakdown("Çalışma alanları", stats.byArea)
        Breakdown("Uçak tipleri", stats.byAircraft)
        Breakdown("ATA bölümleri", stats.byAta)

        SectionTitle("Dışa aktar ve yedekle", Modifier.padding(top = 8.dp))
        FilledTonalButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Staj Defteri")
                    .putExtra(Intent.EXTRA_TEXT, diaryText(internship, entries))
                context.startActivity(Intent.createChooser(send, "Staj defterini paylaş"))
            },
            enabled = entries.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Staj defterini metin olarak paylaş") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { backup.launch("staj-defteri-${LocalDate.now()}.json") },
                modifier = Modifier.weight(1f),
            ) { Text("Yedekle") }
            OutlinedButton(
                onClick = { restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.weight(1f),
            ) { Text("Yedekten yükle") }
        }
        Text(
            "Veriler yalnızca bu telefonda saklanır. Telefon değiştirmeden ya da uygulamayı " +
                "silmeden önce yedek almayı unutma.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = "Yedekten yüklensin mi?",
            text = "Telefondaki tüm günlük kayıtları, notlar ve staj bilgileri silinip " +
                "yedektekilerle değiştirilecek.",
            confirm = "Yükle",
            onConfirm = {
                pendingRestore = null
                scope.launch {
                    runCatching {
                        val json = withContext(Dispatchers.IO) { context.readText(uri) }
                        vm.restore(json)
                    }.fold(
                        { (e, n) -> toast("$e günlük kaydı ve $n not yüklendi") },
                        { toast("Yedek okunamadı: geçerli bir Staj Defteri yedeği değil") },
                    )
                }
            },
            onDismiss = { pendingRestore = null },
        )
    }
}

private fun Context.writeText(uri: Uri, text: String) {
    val stream = contentResolver.openOutputStream(uri, "wt") ?: error("Dosya açılamadı")
    stream.use { it.write(text.toByteArray()) }
}

private fun Context.readText(uri: Uri): String {
    val stream = contentResolver.openInputStream(uri) ?: error("Dosya açılamadı")
    return stream.use { it.readBytes().decodeToString() }
}

@Composable
private fun InternshipCard(internship: Internship?, days: Int, onEdit: () -> Unit) {
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (internship == null || internship.company.isBlank()) {
                Text("Staj bilgilerini ekle", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Kurum, tarih ve hedef gün sayısını gir.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            Text(
                internship.company,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (internship.department.isNotBlank()) {
                Text(internship.department, style = MaterialTheme.typography.bodyLarge)
            }
            val start = internship.startDate
            val end = internship.endDate
            if (start != null || end != null) {
                Text(
                    "${start?.let(::shortDate) ?: "?"} – ${end?.let(::shortDate) ?: "?"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (internship.targetDays > 0) {
                val left = (internship.targetDays - days).coerceAtLeast(0)
                Text(
                    if (left == 0) "Hedef tamamlandı: $days / ${internship.targetDays} gün"
                    else "$days / ${internship.targetDays} gün · $left gün kaldı",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                LinearProgressIndicator(
                    progress = { (days.toFloat() / internship.targetDays).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surface,
                    drawStopIndicator = {},
                )
            }
            Text(
                "Düzenlemek için dokun",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Kayıt sayısına göre sıralı yatay çubuk listesi; veri yoksa hiç gösterilmez. */
@Composable
private fun Breakdown(title: String, rows: List<Pair<String, Int>>) {
    if (rows.isEmpty()) return
    val max = rows.maxOf { it.second }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(title)
            rows.forEach { (name, count) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(
                            name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "$count kayıt",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { count.toFloat() / max },
                        modifier = Modifier.fillMaxWidth(),
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}
