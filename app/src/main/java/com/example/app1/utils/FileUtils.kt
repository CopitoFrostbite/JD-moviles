package com.example.app1.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {
    fun getPath(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor != null && cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val path = cursor.getString(index)
            cursor.close()
            path
        } else {
            uri.path
        }
    }
}