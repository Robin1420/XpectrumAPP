package com.example.xpectrumapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class XpectrumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable vector drawable support for all versions
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
}
