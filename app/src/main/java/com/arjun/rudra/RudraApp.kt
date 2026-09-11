package com.arjun.rudra

import android.app.Application
import com.arjun.rudra.manager.MemoryManager

/**
 * RUDRA AI — Application entry point.
 * Holds long-lived singletons (Memory) so any component can reach them
 * without re-reading SharedPreferences everywhere.
 */
class RudraApp : Application() {

    lateinit var memory: MemoryManager
        private set

    override fun onCreate() {
        super.onCreate()
        memory = MemoryManager(this)
    }
}
