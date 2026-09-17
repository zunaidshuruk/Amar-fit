package com.example.data.remote

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class DriveFileItem(
    val id: String? = null,
    val name: String? = null
)

data class DriveFileListResponse(
    val files: List<DriveFileItem>? = null
)

interface GoogleDriveApiService {
    @GET("drive/v3/files")
    suspend fun searchFiles(
        @Header("Authorization") token: String,
        @Query("q") query: String,
        @Query("fields") fields: String = "files(id,name)"
    ): DriveFileListResponse

    @Multipart
    @POST("upload/drive/v3/files?uploadType=multipart")
    suspend fun createFile(
        @Header("Authorization") token: String,
        @Part metadata: MultipartBody.Part,
        @Part file: MultipartBody.Part
    ): DriveFileItem

    @Multipart
    @PATCH("upload/drive/v3/files/{fileId}?uploadType=multipart")
    suspend fun updateFile(
        @Header("Authorization") token: String,
        @Path("fileId") fileId: String,
        @Part metadata: MultipartBody.Part,
        @Part file: MultipartBody.Part
    ): DriveFileItem
}
