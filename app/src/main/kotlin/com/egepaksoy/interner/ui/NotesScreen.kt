package com.egepaksoy.interner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.egepaksoy.interner.data.Note
import java.time.Instant
import java.time.ZoneId

private fun updatedLabel(millis: Long): String =
    shortDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay())

@Composable
fun NotesScreen(
    notes: List<Note>,
    pad: PaddingValues,
    onOpen: (Long) -> Unit,
    onPin: (Note, Boolean) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(notes, query) { notes.filter { matches(query, it.title, it.content) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listPadding(pad),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (notes.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    Icons.Default.Create,
                    "Henüz not yok",
                    "Tork değerleri, kısaltmalar, sorulacak sorular... Aklında kalsın istediklerini buraya yaz.",
                )
            }
        } else {
            item(key = "search") { SearchField(query, { query = it }, "Notlarda ara") }
            if (shown.isEmpty()) {
                item(key = "none") {
                    EmptyState(Icons.Default.Search, "Sonuç yok", "\"$query\" ile eşleşen not bulunamadı.")
                }
            }
            items(shown, key = { it.id }) { note ->
                NoteCard(note, onClick = { onOpen(note.id) }, onPin = { onPin(note, !note.pinned) })
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onPin: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 14.dp, end = 4.dp)) {
            Column(
                modifier = Modifier.weight(1f).padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    note.title.ifBlank { note.content.lineSequence().firstOrNull().orEmpty() },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.title.isNotBlank() && note.content.isNotBlank()) {
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    updatedLabel(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPin) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = if (note.pinned) "Sabitlemeyi kaldır" else "Başa sabitle",
                    tint = if (note.pinned) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/** Geri çıkıldığında otomatik kaydeder; tamamen boş not kaydedilmez. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    var ready by rememberSaveable { mutableStateOf(false) }
    var dirty by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var pinned by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (ready) return@LaunchedEffect
        if (id != 0L) {
            vm.note(id)?.let {
                title = it.title
                content = it.content
                pinned = it.pinned
            }
        }
        ready = true
    }

    fun saveAndLeave() {
        val blank = title.isBlank() && content.isBlank()
        when {
            blank && id != 0L -> vm.deleteNote(id)
            !blank && dirty -> vm.save(Note(id, title.trim(), content.trim(), pinned))
        }
        onBack()
    }

    BackHandler(onBack = ::saveAndLeave)

    val bare = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
    )
    val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (id == 0L) "Yeni not" else "Not") },
                navigationIcon = {
                    IconButton(onClick = ::saveAndLeave) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kaydet ve geri dön")
                    }
                },
                actions = {
                    IconButton(onClick = { pinned = !pinned; dirty = true }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = if (pinned) "Sabitlemeyi kaldır" else "Başa sabitle",
                            tint = if (pinned) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    if (id != 0L) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Notu sil")
                        }
                    }
                },
            )
        },
    ) { pad ->
        if (!ready) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding(),
            horizontalAlignment = Alignment.Start,
        ) {
            TextField(
                value = title,
                onValueChange = { title = it; dirty = true },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Başlık", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = bare,
                keyboardOptions = sentences,
            )
            TextField(
                value = content,
                onValueChange = { content = it; dirty = true },
                modifier = Modifier.fillMaxWidth().weight(1f),
                placeholder = { Text("Notunu yaz...") },
                colors = bare,
                keyboardOptions = sentences,
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Not silinsin mi?",
            text = "Bu not kalıcı olarak silinecek.",
            confirm = "Sil",
            onConfirm = {
                confirmDelete = false
                vm.deleteNote(id)
                onBack()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
