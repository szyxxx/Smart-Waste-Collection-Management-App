package com.bluebin.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudStorageService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "CloudStorageService"
        private const val BUCKET_NAME = "foto_angkut"
        private const val SERVICE_ACCOUNT_PATH = "service-account.json"
        private const val GCS_UPLOAD_URL = "https://storage.googleapis.com/upload/storage/v1/b/$BUCKET_NAME/o"
        private const val GCS_API_URL = "https://storage.googleapis.com/storage/v1/b/$BUCKET_NAME/o"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        try {
            // Read service account JSON from assets
            val serviceAccountJson = context.assets.open(SERVICE_ACCOUNT_PATH).use { 
                it.readBytes().toString(Charsets.UTF_8)
            }
            
            Log.d(TAG, "Service account JSON loaded successfully")
            
            // Create credentials from JSON string
            val credentials = ServiceAccountCredentials.fromStream(
                ByteArrayInputStream(serviceAccountJson.toByteArray())
            ).createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
            
            // Refresh and get access token
            credentials.refresh()
            val accessToken = credentials.accessToken
            
            if (accessToken == null) {
                Log.e(TAG, "Access token is null after refresh")
                throw RuntimeException("Failed to obtain access token - token is null")
            }
            
            val tokenValue = accessToken.tokenValue
            if (tokenValue.isNullOrEmpty()) {
                Log.e(TAG, "Access token value is null or empty")
                throw RuntimeException("Failed to obtain access token - token value is empty")
            }
            
            Log.d(TAG, "Access token obtained successfully")
            tokenValue
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get access token", e)
            throw RuntimeException("Failed to get access token", e)
        }
    }
    
    suspend fun uploadProofPhoto(
        photoUri: Uri,
        driverId: String,
        scheduleId: String,
        tpsId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting photo upload to Google Cloud Storage via REST API")
            Log.d(TAG, "Photo URI: $photoUri")
            Log.d(TAG, "Driver: $driverId, Schedule: $scheduleId, TPS: $tpsId")
            
            // Generate unique filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "proof_photos/${driverId}/${scheduleId}/${tpsId}_${timestamp}.jpg"
            
            // Read photo data
            val photoData = readPhotoData(photoUri)
            if (photoData.isEmpty()) {
                Log.e(TAG, "Photo data is empty or could not be read")
                return@withContext Result.failure(Exception("Photo data is empty"))
            }
            
            Log.d(TAG, "Photo data read successfully: ${photoData.size} bytes")
            
            // Get access token
            val accessToken = getAccessToken()
            
            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    """{"name":"$fileName","contentType":"image/jpeg"}""".toRequestBody("application/json".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    fileName,
                    photoData.toRequestBody("image/jpeg".toMediaType())
                )
                .build()
            
            // Create request
            val request = Request.Builder()
                .url("$GCS_UPLOAD_URL?uploadType=multipart")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            
            // Execute request
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val publicUrl = "https://storage.googleapis.com/${BUCKET_NAME}/${fileName}"
                Log.i(TAG, "Photo uploaded successfully to: $publicUrl")
                Result.success(publicUrl)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Upload failed with code ${response.code}: $errorBody")
                Result.failure(Exception("Upload failed: ${response.code} - $errorBody"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload photo to Google Cloud Storage", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadDriverPhoto(
        photoUri: Uri,
        driverId: String,
        fileName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting driver photo upload to Google Cloud Storage via REST API")
            
            // Generate unique filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val gcsFileName = fileName ?: "driver_photos/${driverId}/profile_${timestamp}.jpg"
            
            // Read photo data
            val photoData = readPhotoData(photoUri)
            if (photoData.isEmpty()) {
                Log.e(TAG, "Driver photo data is empty or could not be read")
                return@withContext Result.failure(Exception("Photo data is empty"))
            }
            
            Log.d(TAG, "Driver photo data read successfully: ${photoData.size} bytes")
            
            // Get access token
            val accessToken = getAccessToken()
            
            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    """{"name":"$gcsFileName","contentType":"image/jpeg"}""".toRequestBody("application/json".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    gcsFileName,
                    photoData.toRequestBody("image/jpeg".toMediaType())
                )
                .build()
            
            // Create request
            val request = Request.Builder()
                .url("$GCS_UPLOAD_URL?uploadType=multipart")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            
            // Execute request
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val publicUrl = "https://storage.googleapis.com/${BUCKET_NAME}/${gcsFileName}"
                Log.i(TAG, "Driver photo uploaded successfully to: $publicUrl")
                Result.success(publicUrl)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Driver photo upload failed with code ${response.code}: $errorBody")
                Result.failure(Exception("Upload failed: ${response.code} - $errorBody"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload driver photo to Google Cloud Storage", e)
            Result.failure(e)
        }
    }
    
    suspend fun deletePhoto(fileName: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Deleting photo from Google Cloud Storage: $fileName")
            
            // Extract the file path from the URL if it's a full URL
            val filePath = if (fileName.startsWith("https://storage.googleapis.com/${BUCKET_NAME}/")) {
                fileName.substringAfter("https://storage.googleapis.com/${BUCKET_NAME}/")
            } else {
                fileName
            }
            
            // Get access token
            val accessToken = getAccessToken()
            
            // Create delete request
            val request = Request.Builder()
                .url("$GCS_API_URL/${java.net.URLEncoder.encode(filePath, "UTF-8")}")
                .delete()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
            
            // Execute request
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful || response.code == 404) {
                Log.i(TAG, "Photo deleted successfully: $filePath")
                Result.success(true)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Delete failed with code ${response.code}: $errorBody")
                Result.failure(Exception("Delete failed: ${response.code} - $errorBody"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete photo from Google Cloud Storage", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPhotoUrl(fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting photo URL from Google Cloud Storage: $fileName")
            
            // If it's already a full URL, return it
            if (fileName.startsWith("https://storage.googleapis.com/")) {
                return@withContext Result.success(fileName)
            }
            
            // Generate public URL
            val publicUrl = "https://storage.googleapis.com/${BUCKET_NAME}/${fileName}"
            
            Log.d(TAG, "Generated photo URL: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get photo URL", e)
            Result.failure(e)
        }
    }
    
    private fun readPhotoData(photoUri: Uri): ByteArray {
        return try {
            when (photoUri.scheme) {
                "content" -> {
                    context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                        inputStream.readBytes()
                    } ?: byteArrayOf()
                }
                "file" -> {
                    val file = java.io.File(photoUri.path ?: "")
                    if (file.exists() && file.canRead()) {
                        FileInputStream(file).use { it.readBytes() }
                    } else {
                        Log.e(TAG, "File does not exist or cannot be read: ${file.absolutePath}")
                        byteArrayOf()
                    }
                }
                else -> {
                    Log.e(TAG, "Unsupported URI scheme: ${photoUri.scheme}")
                    byteArrayOf()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading photo data from URI: $photoUri", e)
            byteArrayOf()
        }
    }
} 