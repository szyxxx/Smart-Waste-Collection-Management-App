package com.bluebin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BluebinApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase App Check (optional, for additional security)
        // This can help resolve some Firebase security warnings
        try {
            // Firebase App Check initialization would go here if needed
            // For development, we can skip this or use debug provider
        } catch (e: Exception) {
            // Ignore App Check errors in development
        }
    }
} 