package com.lymors.lycommons.data.storage

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.lymors.lycommons.utils.MyResult
import kotlinx.coroutines.tasks.await

class MediaUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val storageReference: StorageReference = FirebaseStorage.getInstance().reference

    override suspend fun doWork(): Result {
        val uriString = inputData.getString("uri") ?: return Result.failure()
        val path = inputData.getString("path") ?: return Result.failure()
        val uri = Uri.parse(uriString)

        return try {
            val result = uploadToFirebaseStorage(uri, path)
            if (result is MyResult.Success) {
                val outputData = Data.Builder().putString("downloadUrl", result.data).build()
                Result.success(outputData)
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun uploadToFirebaseStorage(uri: Uri, path: String): MyResult<String> {
        return try {
            val filename = uri.lastPathSegment ?: System.currentTimeMillis().toString()
            val fileRef = storageReference.child("$path/$filename")
            val uploadTask = fileRef.putFile(uri)
            val result = uploadTask.await()
            val downloadUrl = result.storage.downloadUrl.await()
            MyResult.Success(downloadUrl.toString())
        } catch (e: Exception) {
            MyResult.Error(e.message ?: "Unknown error occurred")
        }
    }
}
