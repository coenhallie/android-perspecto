package com.example.perspecto

import android.app.Application
import com.example.perspecto.di.SupabaseModule

class PerspectoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseModule.initialize(this)
    }
}
