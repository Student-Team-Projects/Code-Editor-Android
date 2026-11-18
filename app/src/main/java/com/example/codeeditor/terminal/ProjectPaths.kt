package com.example.codeeditor.terminal

import android.net.Uri
import android.provider.DocumentsContract

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

/**
    The filesystem path behind a folder picked with the system file picker, or null if it has none
    (e.g. a cloud provider). Termux can open it only after `termux-setup-storage`.
 */
fun filesystemPathOf(treeUri: Uri?): String? {
    if (treeUri == null) return null
    val documentId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return null
    return externalStoragePath(treeUri.authority, documentId)
}

/** `primary:Documents/proj` → `/storage/emulated/0/Documents/proj`; `1A2B-3C4D:x` → `/storage/1A2B-3C4D/x`. */
fun externalStoragePath(authority: String?, documentId: String): String? {
    if (authority != EXTERNAL_STORAGE_AUTHORITY) return null
    val volume = documentId.substringBefore(':', missingDelimiterValue = "")
    if (volume.isEmpty()) return null
    val root = if (volume == "primary") "/storage/emulated/0" else "/storage/$volume"
    val relative = documentId.substringAfter(':').trim('/')
    return if (relative.isEmpty()) root else "$root/$relative"
}
