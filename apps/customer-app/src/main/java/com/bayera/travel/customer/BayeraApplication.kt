package com.bayera.travel.customer

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration

class BayeraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize Firebase safely
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) { }

        // 2. Initialize Map Config (Fixes the "Push Away" crash)
        // This ensures the map has a place to save its cache before it tries to open
        val prefs = getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(this, prefs)
    }
}
