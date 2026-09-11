package com.arjun.rudra.manager

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.text.format.DateFormat
import java.util.Date

class DeviceToolsManager(private val context: Context) {

    fun batteryPercent(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun currentTimeText(): String = DateFormat.format("h:mm a", Date()).toString()

    fun currentDateText(): String = DateFormat.format("dd MMMM yyyy", Date()).toString()

    fun setFlashlight(on: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, on)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun adjustVolume(raise: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    /**
     * Locks the screen using Android's official Device Admin API.
     * Requires the user to have granted RUDRA Device Admin rights beforehand
     * (a real, explicit Android system dialog the user must approve — this is
     * NOT a way to bypass the lock screen, only to trigger the same lock
     * Android's power button does).
     */
    fun lockDeviceIfAdmin(): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return try {
            dpm.lockNow()
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
