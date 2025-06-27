package com.bluebin.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PhotoUtils {
    
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_proof"
        return File(context.cacheDir, "$imageFileName.jpg")
    }
    
    fun isValidPhotoUri(uri: Uri?): Boolean {
        return uri != null && uri != Uri.EMPTY && uri.toString().isNotEmpty()
    }
    
    fun extractFileNameFromUrl(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            uri.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }
} 