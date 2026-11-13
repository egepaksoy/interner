package com.egepaksoy.interner.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.egepaksoy.interner.ui.theme.areaColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.egepaksoy.interner.data.Xlsx
import com.egepaksoy.interner.data.writeDiaryXlsx
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
    val excel = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(Xlsx.MIME),
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val notes = vm.notes.value.orEmpty()
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Dosya açılamadı")
                    stream.use { writeDiaryXlsx(it, entries, notes) }
                }
            }.fold({ toast("Excel dosyası kaydedildi") }, { toast("Excel dosyası kaydedilemedi: ${it.message}") })
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
        InternshipHero(internship, stats.days, stats.totalHours, onInternship)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Kayıtlı gün", stats.days.toString(), Modifier.weight(1f))
            StatTile("Toplam saat", formatHours(stats.totalHours), Modifier.weight(1f))
            StatTile(
                "Günlük ort.",
                if (stats.days > 0) formatHours(stats.totalHours / stats.days) + " sa" else "–",
                Modifier.weight(1f),
            )
        }

        Breakdown("Çalışma alanları", stats.byArea) { areaColor(it) }
        Breakdown("Uçak tipleri", stats.byAircraft) { MaterialTheme.colorScheme.primary }
        Breakdown("ATA bölümleri", stats.byAta) { MaterialTheme.colorScheme.primary }

        Text(
            "Dışa aktar ve yedekle",
            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        AppCard(Modifier.fillMaxWidth()) {
            Column {
                ActionRow(
                    "Metin olarak paylaş",
                    "Staj defterine geçirmek için gün gün düz metin",
                    enabled = entries.isNotEmpty(),
                ) {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "Staj Defteri")
                        .putExtra(Intent.EXTRA_TEXT, diaryText(internship, entries))
                    context.startActivity(Intent.createChooser(send, "Staj defterini paylaş"))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow(
                    "Excel dosyası olarak kaydet",
                    ".xlsx · her kayıt bir satır, notlar ayrı sayfada",
                    enabled = entries.isNotEmpty(),
                ) { excel.launch("staj-defteri-${LocalDate.now()}.xlsx") }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow("Yedekle", "Tüm kayıtları ve notları tek dosyaya kaydet") {
                    backup.launch("staj-defteri-${LocalDate.now()}.json")
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ActionRow("Yedekten yükle", "Telefondaki verilerin yerine yedektekileri koyar") {
                    restore.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }
            }
        }
        Text(
            "Veriler yalnızca bu telefonda saklanır. Telefon değiştirmeden ya da uygulamayı " +
                "silmeden önce yedek almayı unutma.",
            modifier = Modifier.padding(horizontal = 4.dp),
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
private fun ActionRow(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = alpha),
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
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
private fun Breakdown(title: String, rows: List<Pair<String, Int>>, color: @Composable (String) -> Color) {
    if (rows.isEmpty()) return
    val max = rows.maxOf { it.second }
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            rows.forEach { (name, count) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(count.toFloat() / max)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(color(name)),
                        )
                    }
                }
            }
        }
    }
}
