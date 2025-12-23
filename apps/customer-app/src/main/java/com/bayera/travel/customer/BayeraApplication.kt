package com.bayera.travel.customer

import android.app.Application
import com.google.firebase.FirebaseApp
import org.osmdroid.config.Configuration

class BayeraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Wake up Firebase immediately
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Ignore if already initialized
        }

        // 2. Wake up Map Engine immediately
        Configuration.getInstance().userAgentValue = packageName
    }
}
