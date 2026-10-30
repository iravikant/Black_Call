package com.blackcall.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Uses [ContactsContract.PhoneLookup], which Android indexes and matches across number formats.
 * The old implementation scanned every contact on every call.
 */
class ContactsLookup(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /** Blocking; call from a background dispatcher. */
    fun isContact(number: String): Boolean {
        if (number.isBlank() || !hasPermission()) return false
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return runCatching {
            context.contentResolver.query(
                uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null
            )?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
    }
}
