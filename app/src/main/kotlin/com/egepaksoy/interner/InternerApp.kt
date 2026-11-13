package com.egepaksoy.interner

import android.app.Application
import androidx.room.Room
import com.egepaksoy.interner.data.AppDatabase

class InternerApp : Application() {
    val db: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "interner.db").build()
    }
}
