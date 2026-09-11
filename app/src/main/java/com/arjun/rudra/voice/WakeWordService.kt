package com.arjun.rudra.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arjun.rudra.R

/**
 * Foreground service that should host the continuous "Hey Rudra" wake-word
 * engine so RUDRA can wake up while the phone is idle / screen off.
 *
 * IMPORTANT — real wake-word detection is NOT implemented here yet, because
 * Android's built-in SpeechRecognizer cannot do always-on offline wake-word
 * spotting (it needs an internet round-trip per session and stops after
 * silence). To get true "Hey Rudra" hotword detection you need a dedicated
 * on-device wake-word engine. The recommended, legitimate option is:
 *
 *   Picovoice Porcupine (https://picovoice.ai/platform/porcupine/)
 *   1. Create a free Picovoice account -> get an AccessKey.
 *   2. In Picovoice Console, train a CUSTOM wake word "Hey Rudra" -> download
 *      the resulting `.ppn` model file for Android.
 *   3. Uncomment the porcupine-android dependency in app/build.gradle.kts.
 *   4. Drop the `.ppn` file into app/src/main/assets/ and wire it up below
 *      (PorcupineManager.Builder().setAccessKey(...).setKeywordPath(...)).
 *
 * Until that's wired in, use the in-app "Push to talk" orb button in
 * MainActivity as the activation trigger — it uses SpeechManager directly
 * and needs no extra account.
 */
class WakeWordService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        // TODO (post Picovoice setup): initialize PorcupineManager here and
        // call CommandRouter when the "Hey Rudra" keyword fires.

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val channelId = "rudra_wakeword_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "RUDRA background listening",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("RUDRA is listening")
            .setContentText("Bolo \"Hey Rudra\" jokhon dorkar hobe")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
