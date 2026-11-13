package com.egepaksoy.interner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.egepaksoy.interner.InternerApp
import com.egepaksoy.interner.data.Backup
import com.egepaksoy.interner.data.BackupData
import com.egepaksoy.interner.data.Internship
import com.egepaksoy.interner.data.LogEntry
import com.egepaksoy.interner.data.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as InternerApp).db.dao()

    /** `null` = henüz yüklenmedi. */
    val entries = dao.entries().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val notes = dao.notes().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val internship = dao.internship().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    suspend fun entry(id: Long): LogEntry? = dao.entry(id)

    suspend fun latestEntry(): LogEntry? = dao.allEntries().firstOrNull()

    suspend fun note(id: Long): Note? = dao.note(id)

    fun save(entry: LogEntry, done: () -> Unit = {}) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        dao.upsert(entry.copy(createdAt = entry.createdAt.takeIf { it > 0 } ?: now, updatedAt = now))
        done()
    }

    fun deleteEntry(id: Long, done: () -> Unit = {}) = viewModelScope.launch {
        dao.deleteEntry(id)
        done()
    }

    fun save(note: Note) = viewModelScope.launch {
        dao.upsert(note.copy(updatedAt = System.currentTimeMillis()))
    }

    fun setPinned(note: Note, pinned: Boolean) = viewModelScope.launch {
        dao.upsert(note.copy(pinned = pinned))
    }

    fun deleteNote(id: Long) = viewModelScope.launch { dao.deleteNote(id) }

    fun save(internship: Internship, done: () -> Unit = {}) = viewModelScope.launch {
        dao.upsert(internship.copy(id = 1))
        done()
    }

    suspend fun backupJson(): String =
        Backup.toJson(BackupData(dao.currentInternship(), dao.allEntries(), dao.allNotes()))

    /** @return geri yüklenen günlük kaydı ve not sayısı */
    suspend fun restore(json: String): Pair<Int, Int> {
        val data = Backup.parse(json)
        dao.replaceAll(data)
        return data.entries.size to data.notes.size
    }
}
