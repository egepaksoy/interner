package com.egepaksoy.interner

import android.app.Application
import androidx.room.Room
import com.egepaksoy.interner.data.AppDatabase
import com.egepaksoy.interner.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class InternerApp : Application() {
    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "interner.db").build()
    }

    override fun onCreate() {
        super.onCreate()
        // Kayıtlar ya da staj bilgileri her değiştiğinde ana ekran widget'larını güncelle.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val dao = db.dao()
            combine(dao.entries(), dao.internship()) { _, _ -> }
                .drop(1) // ilk değer mevcut durumdur, değişiklik değil
                .collect { Widgets.updateAll(this@InternerApp) }
        }
    }
}
