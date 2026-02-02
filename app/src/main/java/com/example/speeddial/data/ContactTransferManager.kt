package com.example.speeddial.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ContactTransferManager {
    private const val CONTACTS_JSON = "contacts.json"
    private const val PHOTO_DIRECTORY = "photos/"

    fun exportContacts(
        context: Context,
        contacts: List<Contact>,
        outputStream: OutputStream
    ) {
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            val contactsPayload = JSONArray()

            contacts.forEachIndexed { index, contact ->
                val contactJson = JSONObject()
                    .put("name", contact.name)
                    .put("phoneNumber", contact.phoneNumber)

                val photoPath = contact.photoUri
                    ?.let { uriString ->
                        val sourceUri = Uri.parse(uriString)
                        val extension = resolveExtension(context, sourceUri)
                        val entryName = "$PHOTO_DIRECTORY${buildPhotoFileName(index, extension)}"

                        openInputStream(context, sourceUri)?.use { input ->
                            zipOut.putNextEntry(ZipEntry(entryName))
                            input.copyTo(zipOut)
                            zipOut.closeEntry()
                            entryName
                        }
                    }

                contactJson.put("photoFile", photoPath)
                contactsPayload.put(contactJson)
            }

            val root = JSONObject()
                .put("version", 1)
                .put("contacts", contactsPayload)

            zipOut.putNextEntry(ZipEntry(CONTACTS_JSON))
            zipOut.write(root.toString().toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }
    }

    fun importContacts(
        context: Context,
        inputStream: InputStream
    ): List<Contact> {
        val photoBlobs = mutableMapOf<String, ByteArray>()
        var contactsJson: String? = null

        ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val bytes = zipIn.readEntryBytes()
                    if (entry.name == CONTACTS_JSON) {
                        contactsJson = String(bytes, Charsets.UTF_8)
                    } else if (entry.name.startsWith(PHOTO_DIRECTORY)) {
                        photoBlobs[entry.name] = bytes
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        val payload = contactsJson ?: throw IllegalArgumentException("Archive missing contacts.json")
        val root = JSONObject(payload)
        val contactsArray = root.optJSONArray("contacts") ?: JSONArray()

        val importedContacts = mutableListOf<Contact>()

        for (i in 0 until contactsArray.length()) {
            val item = contactsArray.optJSONObject(i) ?: continue
            val name = item.optString("name").takeIf { it.isNotBlank() } ?: continue
            val phoneNumber = item.optString("phoneNumber").takeIf { it.isNotBlank() } ?: continue
            val photoPath = item.optString("photoFile").takeIf { it.isNotBlank() }

            val storedPhotoUri = photoPath?.let { path ->
                photoBlobs[path]?.let { data ->
                    writePhotoToInternalStorage(context, data, path)
                }
            }

            importedContacts.add(
                Contact(
                    name = name,
                    phoneNumber = phoneNumber,
                    photoUri = storedPhotoUri
                )
            )
        }

        return importedContacts
    }

    private fun ZipInputStream.readEntryBytes(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(chunk)
            if (read <= 0) break
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun writePhotoToInternalStorage(
        context: Context,
        bytes: ByteArray,
        originalPath: String
    ): String? {
        return try {
            val extension = originalPath.substringAfterLast('.', "jpg")
            val fileName = "contact_import_${UUID.randomUUID()}.$extension"
            val destination = File(context.filesDir, fileName)
            FileOutputStream(destination).use { out ->
                out.write(bytes)
            }
            "file://${destination.absolutePath}"
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveExtension(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_ANDROID_RESOURCE ->
                contentResolver.getType(uri)
            ContentResolver.SCHEME_FILE -> {
                val path = uri.path
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    path?.substringAfterLast('.', "")?.lowercase()
                )
            }
            else -> contentResolver.getType(uri)
        }

        val extensionFromMime = mimeType?.let { type ->
            MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        }

        if (!extensionFromMime.isNullOrBlank()) {
            return extensionFromMime
        }

        val extFromUrl = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        if (!extFromUrl.isNullOrBlank()) {
            return extFromUrl
        }

        return "jpg"
    }

    private fun buildPhotoFileName(index: Int, extension: String): String {
        val sanitizedExtension = extension.lowercase().ifBlank { "jpg" }
        return "contact_${index + 1}.$sanitizedExtension"
    }

    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return try {
            when (uri.scheme) {
                ContentResolver.SCHEME_FILE -> {
                    uri.path?.let { path ->
                        val file = File(path)
                        if (file.exists()) file.inputStream() else null
                    }
                }
                else -> context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}
