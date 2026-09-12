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

    fun explainMissing(permission: String): String = when (permission) {
        Manifest.permission.RECORD_AUDIO -> "Baat sunne ke liye mujhe Microphone permission chahiye."
        Manifest.permission.READ_CONTACTS -> "Contact dhundh kar call karne ke liye Contacts permission chahiye."
        Manifest.permission.CALL_PHONE -> "Call karne ke liye Phone permission chahiye."
        Manifest.permission.READ_CALL_LOG -> "Recent call dekhne ke liye Call Log permission chahiye."
        Manifest.permission.CAMERA -> "Flashlight chalane ke liye Camera permission chahiye."
        Manifest.permission.POST_NOTIFICATIONS -> "Background mein rehne ke liye notification permission chahiye."
        else -> "Ye karne ke liye ek extra permission chahiye."
    }
}
