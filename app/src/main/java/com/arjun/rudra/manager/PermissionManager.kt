package com.arjun.rudra.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionManager {

    val ALL_REQUIRED = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CAMERA,
        Manifest.permission.POST_NOTIFICATIONS
    )

    fun has(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun missing(context: Context): List<String> =
        ALL_REQUIRED.filter { !has(context, it) }

    /** Human-readable Bengali/Banglish explanation shown when a command needs a missing permission. */
    fun explainMissing(permission: String): String = when (permission) {
        Manifest.permission.RECORD_AUDIO -> "Arjun, kotha shonar jonno amar Microphone permission lagbe."
        Manifest.permission.READ_CONTACTS -> "Arjun, contact khuje call korte amar Contacts permission lagbe."
        Manifest.permission.CALL_PHONE -> "Arjun, call korte amar Phone permission lagbe."
        Manifest.permission.READ_CALL_LOG -> "Arjun, recent call dekhte amar Call Log permission lagbe."
        Manifest.permission.CAMERA -> "Arjun, flashlight chalate Camera permission lagbe."
        Manifest.permission.POST_NOTIFICATIONS -> "Arjun, background e thakte notification permission lagbe."
        else -> "Arjun, eta korte ekta extra permission lagbe."
    }
}
