package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: V2Database? = null

    fun getDatabase(context: Context): V2Database {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                V2Database::class.java,
                "v2ray_bento_db"
            ).fallbackToDestructiveMigration().build()
            instance = db
            db
        }
    }
}
