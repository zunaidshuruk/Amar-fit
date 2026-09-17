package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.remote.GoogleDriveClient
import com.example.data.remote.RetrofitClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

data class DriveBackupPayload(
    val backupDate: String,
    val profile: UserProfile?,
    val dailyMetrics: List<DailyMetric>,
    val foodLogs: List<FoodLog>,
    val savedDietCharts: List<SavedDietChart>,
    val savedWorkouts: List<SavedWorkout>,
    val savedChats: List<SavedChat>,
    val medicalRecords: List<MedicalRecord>
)

object GoogleDriveManager {
    private const val TAG = "GoogleDriveManager"
    private const val BACKUP_FILE_NAME = "amarfit_backup.json"
    private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    fun requestAuthorization(context: Context): Task<AuthorizationResult> {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_FILE_SCOPE)))
            .build()
        return Identity.getAuthorizationClient(context).authorize(request)
    }

    suspend fun backupToDrive(accessToken: String, payload: DriveBackupPayload): Boolean {
        return try {
            val jsonAdapter = RetrofitClient.moshi.adapter(DriveBackupPayload::class.java)
            val jsonString = jsonAdapter.toJson(payload)

            val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) {
                accessToken
            } else {
                "Bearer $accessToken"
            }

            val searchQuery = "name='$BACKUP_FILE_NAME' and trashed=false"
            val searchResponse = GoogleDriveClient.service.searchFiles(
                token = authHeader,
                query = searchQuery
            )

            val existingFileId = searchResponse.files?.firstOrNull()?.id

            val metadataJson = """{"name":"$BACKUP_FILE_NAME","mimeType":"application/json"}"""
            val metadataBody = metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
            val metadataPart = MultipartBody.Part.create(metadataBody)

            val fileBody = jsonString.toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.create(fileBody)

            if (!existingFileId.isNullOrBlank()) {
                GoogleDriveClient.service.updateFile(
                    token = authHeader,
                    fileId = existingFileId,
                    metadata = metadataPart,
                    file = filePart
                )
            } else {
                GoogleDriveClient.service.createFile(
                    token = authHeader,
                    metadata = metadataPart,
                    file = filePart
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup data to Google Drive", e)
            false
        }
    }

    suspend fun restoreFromDrive(accessToken: String): DriveBackupPayload? {
        return try {
            val authHeader = if (accessToken.startsWith("Bearer ", ignoreCase = true)) {
                accessToken
            } else {
                "Bearer $accessToken"
            }

            val searchQuery = "name='$BACKUP_FILE_NAME' and trashed=false"
            val searchResponse = GoogleDriveClient.service.searchFiles(
                token = authHeader,
                query = searchQuery
            )

            val existingFileId = searchResponse.files?.firstOrNull()?.id
            if (existingFileId.isNullOrBlank()) {
                Log.d(TAG, "No backup file found in Google Drive")
                return null
            }

            val responseBody = GoogleDriveClient.service.downloadFile(
                token = authHeader,
                fileId = existingFileId,
                alt = "media"
            )

            val jsonString = responseBody.string()
            val jsonAdapter = RetrofitClient.moshi.adapter(DriveBackupPayload::class.java)
            jsonAdapter.fromJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore data from Google Drive", e)
            null
        }
    }
}
