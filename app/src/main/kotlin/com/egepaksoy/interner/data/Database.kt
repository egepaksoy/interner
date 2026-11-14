package com.egepaksoy.interner.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM entries ORDER BY date DESC, id DESC")
    fun entries(): Flow<List<LogEntry>>

    @Query("SELECT * FROM entries ORDER BY date DESC, id DESC")
    suspend fun allEntries(): List<LogEntry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun entry(id: Long): LogEntry?

    @Upsert
    suspend fun upsert(entry: LogEntry): Long

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    fun notes(): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC")
    suspend fun allNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun note(id: Long): Note?

    @Upsert
    suspend fun upsert(note: Note): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("SELECT * FROM internship WHERE id = 1")
    fun internship(): Flow<Internship?>

    @Query("SELECT * FROM internship WHERE id = 1")
    suspend fun currentInternship(): Internship?

    @Upsert
    suspend fun upsert(internship: Internship)

    @Query("SELECT DISTINCT aircraft FROM entries WHERE aircraft != ''")
    suspend fun aircraftValues(): List<String>

    @Query("UPDATE entries SET aircraft = :to WHERE aircraft = :from")
    suspend fun renameAircraft(from: String, to: String)

    /** Kayıtlı uçak tiplerini listedeki adlara çevirir ("a320" → "A320"). */
    @Transaction
    suspend fun normalizeAircraftValues() {
        for (value in aircraftValues()) {
            val normalized = normalizeAircraft(value)
            if (normalized != value) renameAircraft(value, normalized)
        }
    }

    @Insert
    suspend fun insertEntries(entries: List<LogEntry>)

    @Insert
    suspend fun insertNotes(notes: List<Note>)

    @Query("DELETE FROM entries")
    suspend fun clearEntries()

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM internship")
    suspend fun clearInternship()

    /** Yedekten geri yükleme: mevcut tüm veriyi yedektekiyle değiştirir. */
    @Transaction
    suspend fun replaceAll(data: BackupData) {
        clearEntries()
        clearNotes()
        clearInternship()
        data.internship?.let { upsert(it.copy(id = 1)) }
        insertEntries(data.entries.map { it.copy(id = 0) })
        insertNotes(data.notes.map { it.copy(id = 0) })
    }
}

@Database(
    entities = [LogEntry::class, Note::class, Internship::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
