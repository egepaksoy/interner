package com.egepaksoy.interner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.egepaksoy.interner.data.AIRCRAFT_TYPES
import com.egepaksoy.interner.data.ATA_CHAPTERS
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.WORK_AREAS
import com.egepaksoy.interner.data.fullDate
import com.egepaksoy.interner.data.normalizeAircraft
import com.egepaksoy.interner.ui.theme.areaColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryEditScreen(vm: AppViewModel, id: Long, initialDate: Long?, onBack: () -> Unit) {
    var ready by rememberSaveable { mutableStateOf(false) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var createdAt by rememberSaveable { mutableLongStateOf(0L) }
    var date by rememberSaveable { mutableLongStateOf(initialDate ?: today()) }
    var title by rememberSaveable { mutableStateOf("") }
    var work by rememberSaveable { mutableStateOf("") }
    var learned by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var aircraft by rememberSaveable { mutableStateOf("") }
    var ata by rememberSaveable { mutableStateOf("") }

    var showErrors by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (ready) return@LaunchedEffect
        if (id != 0L) {
            vm.entry(id)?.let {
                createdAt = it.createdAt
                date = it.date
                title = it.title
                work = it.work
                learned = it.learned
                area = it.area
                aircraft = normalizeAircraft(it.aircraft)
                ata = it.ata
            }
        } else {
            // Yeni kayıtta genelde değişmeyen alanları son kayıttan al.
            vm.latestEntry()?.let {
                area = it.area
                // Listede olmayan eski bir değeri yeni kayda taşıma.
                aircraft = normalizeAircraft(it.aircraft).takeIf { a -> a in AIRCRAFT_TYPES }.orEmpty()
            }
        }
        ready = true
    }

    val emptyError = title.isBlank() && work.isBlank()

    fun save() {
        if (emptyError) {
            showErrors = true
            return
        }
        vm.save(
            LogEntry(
                id = id,
                date = date,
                title = title.trim(),
                work = work.trim(),
                learned = learned.trim(),
                area = area,
                aircraft = aircraft,
                ata = ata.trim(),
                createdAt = createdAt,
            ),
            onBack,
        )
    }

    fun leave() {
        if (dirty) confirmDiscard = true else onBack()
    }

    BackHandler(enabled = dirty) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Yeni kayıt" else "Kaydı düzenle") },
                navigationIcon = {
                    IconButton(onClick = ::leave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (id != 0L) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Kaydı sil")
                        }
                    }
                    Button(
                        onClick = ::save,
                        enabled = ready,
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp),
                    ) { Text("Kaydet") }
                },
            )
        },
    ) { pad ->
        if (!ready) return@Scaffold
        val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateRow(
                label = "Tarih",
                epochDay = date,
                onPick = { date = it; dirty = true },
                modifier = Modifier.fillMaxWidth(),
                format = ::fullDate,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; dirty = true },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Başlık") },
                placeholder = { Text("Örn. Ana iniş takımı tekerlek değişimi") },
                singleLine = true,
                keyboardOptions = sentences,
            )
            OutlinedTextField(
                value = work,
                onValueChange = { work = it; dirty = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                label = { Text("Yapılan işler") },
                placeholder = { Text("Bugün neler yaptın, hangi işlemlere katıldın?") },
                isError = showErrors && emptyError,
                supportingText = if (showErrors && emptyError) {
                    { Text("Başlık ya da yapılan işlerden en az birini yaz") }
                } else null,
                keyboardOptions = sentences,
            )
            OutlinedTextField(
                value = learned,
                onValueChange = { learned = it; dirty = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                label = { Text("Öğrendiklerim / notlar") },
                keyboardOptions = sentences,
            )

            SectionTitle("Çalışma alanı", Modifier.padding(top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Eski sürümlerden kalan, artık listede olmayan alan da seçili görünsün.
                val options = if (area.isBlank() || area in WORK_AREAS) WORK_AREAS else WORK_AREAS + area
                options.forEach { option ->
                    val color = areaColor(option)
                    FilterChip(
                        selected = area == option,
                        onClick = { area = if (area == option) "" else option; dirty = true },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.16f),
                            selectedLabelColor = color,
                        ),
                    )
                }
            }

            SectionTitle("Uçak tipi", Modifier.padding(top = 4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Eski sürümde elle yazılmış, listede olmayan tip de seçili görünsün.
                val options =
                    if (aircraft.isBlank() || aircraft in AIRCRAFT_TYPES) AIRCRAFT_TYPES else AIRCRAFT_TYPES + aircraft
                options.forEach { option ->
                    FilterChip(
                        selected = aircraft == option,
                        onClick = { aircraft = if (aircraft == option) "" else option; dirty = true },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            AtaField(ata) { ata = it; dirty = true }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Değişiklikler kaydedilmedi",
            text = "Kaydetmeden çıkarsan yazdıkların silinecek.",
            confirm = "Kaydetmeden çık",
            dismiss = "Düzenlemeye dön",
            onConfirm = { confirmDiscard = false; onBack() },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Kayıt silinsin mi?",
            text = "${fullDate(date)} tarihli kayıt kalıcı olarak silinecek.",
            confirm = "Sil",
            onConfirm = { confirmDelete = false; vm.deleteEntry(id, onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Serbest metin girilebilen, sağdaki oktan hazır ATA bölümü de seçilebilen alan. */
@Composable
private fun AtaField(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("ATA bölümü") },
            placeholder = { Text("Yaz ya da listeden seç") },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "ATA bölümü listesi")
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            ATA_CHAPTERS.forEach { chapter ->
                DropdownMenuItem(
                    text = { Text(chapter) },
                    onClick = {
                        onChange(chapter)
                        expanded = false
                    },
                )
            }
        }
    }
}
