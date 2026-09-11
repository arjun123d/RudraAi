package com.arjun.rudra.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CallLog

class CallManager(private val context: Context) {

    /** Places a real call. Caller must have already confirmed CALL_PHONE permission is granted. */
    fun call(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Returns the most recent call log entry's number, or null if call log is empty/unreadable. */
    fun mostRecentNumber(): String? {
        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(CallLog.Calls.NUMBER)
        context.contentResolver.query(uri, projection, null, null, "${CallLog.Calls.DATE} DESC LIMIT 1")
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                }
            }
        return null
    }
}
