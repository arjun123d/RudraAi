package com.arjun.rudra.manager

import android.content.ContentUris
import android.content.Context
import android.provider.ContactsContract

data class ResolvedContact(val displayName: String, val phoneNumber: String, val lookupKey: String)

sealed class ContactResolution {
    data class Found(val contact: ResolvedContact) : ContactResolution()
    data class Ambiguous(val candidates: List<ResolvedContact>) : ContactResolution()
    object NotFound : ContactResolution()
}

/**
 * Resolves a spoken name OR a relationship label ("wife", "mom") to an actual
 * on-device contact. Relationship labels are looked up via MemoryManager first
 * (the user maps them once in Settings), then falls back to a direct name search.
 *
 * Never guesses when multiple contacts share a name — returns Ambiguous so the
 * caller (CommandRouter) can ask "Rahul Kumar na Rahul Das?" per the spec.
 */
class ContactManager(private val context: Context, private val memory: MemoryManager) {

    fun resolve(spokenNameOrLabel: String): ContactResolution {
        val label = spokenNameOrLabel.lowercase().trim()
        memory.getContactForLabel(label)?.let { savedLookupKey ->
            findByLookupKey(savedLookupKey)?.let { return ContactResolution.Found(it) }
        }
        val matches = findByDisplayName(spokenNameOrLabel)
        return when {
            matches.isEmpty() -> ContactResolution.NotFound
            matches.size == 1 -> ContactResolution.Found(matches.first())
            else -> ContactResolution.Ambiguous(matches)
        }
    }

    private fun findByDisplayName(name: String): List<ResolvedContact> {
        val results = mutableListOf<ResolvedContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$name%")

        context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val keyIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            while (cursor.moveToNext()) {
                results.add(
                    ResolvedContact(
                        displayName = cursor.getString(nameIdx),
                        phoneNumber = cursor.getString(numIdx),
                        lookupKey = cursor.getString(keyIdx)
                    )
                )
            }
        }
        return results.distinctBy { it.phoneNumber }
    }

    private fun findByLookupKey(lookupKey: String): ResolvedContact? {
        val uri = ContactsContract.Contacts.lookupContact(
            context.contentResolver,
            android.net.Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
        ) ?: return null
        // Re-query phone number for this specific contact.
        val contactId = ContentUris.parseId(uri)
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                val number = cursor.getString(1)
                return ResolvedContact(name, number, lookupKey)
            }
        }
        return null
    }
}
