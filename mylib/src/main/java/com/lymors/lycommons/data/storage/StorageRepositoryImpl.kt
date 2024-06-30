package com.lymors.lycommons.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.google.firebase.storage.FirebaseStorage
import com.lymors.lycommons.utils.MyResult
import com.lymors.lycommons.utils.Utils.saveImageToInternalStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class StorageRepositoryImpl @Inject constructor(
    private val context: Context,
    private val storage: FirebaseStorage
) : StorageRepository {

    override suspend fun uploadImageToFirebaseStorageWithUri(uri: Uri): MyResult<String> {
        return enqueueUploadWorker(uri, "images")
    }

    override suspend fun uploadDocumentToFirebaseStorage(uri: Uri): MyResult<String> {
        return enqueueUploadWorker(uri, "documents")
    }

    override suspend fun uploadImageToFirebaseStorageWithBitmap(bitmap: Bitmap): MyResult<String> {
        val uri = bitmapToUri(bitmap)
        return enqueueUploadWorker(uri, "images")
    }

    override suspend fun uploadImageToFirebaseStorageWithBitmap(bitArray: ByteArray): MyResult<String> {
        val uri = bitArray.toUri(context , System.currentTimeMillis().toString())
        return enqueueUploadWorker(uri, "images")
    }

    override suspend fun deleteImageToFirebaseStorage(url: String): MyResult<String> {
        return try {
            val storageRef = storage.getReferenceFromUrl(url)
            val deleteTask = storageRef.delete()
            deleteTask.await()
            MyResult.Success("Image deleted successfully")
        } catch (e: Exception) {
            MyResult.Error("Failed to delete image: ${e.message}")
        }
    }

    override suspend fun uploadVideoToFirebaseStorage(videoUri: Uri, progressCallBack: (Int) -> Unit): MyResult<String> {
        return enqueueUploadWorker(videoUri, "videos")
    }

    private suspend fun enqueueUploadWorker(uri: Uri, path: String): MyResult<String> {
        val inputData = workDataOf(
            "uri" to uri.saveImageToInternalStorage(context).toString(),
            "path" to path
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<MediaUploadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(uploadWorkRequest)

        val workInfo = WorkManager.getInstance(context).getWorkInfoById(uploadWorkRequest.id).await()
        return when (workInfo.state) {
            WorkInfo.State.SUCCEEDED -> {
                val downloadUrl = workInfo.outputData.getString("downloadUrl")
                MyResult.Success(downloadUrl ?: "")
            }
            WorkInfo.State.FAILED -> MyResult.Error("Upload failed ")
            WorkInfo.State.CANCELLED -> MyResult.Error("Upload cancelled ")
            else -> MyResult.Error("Unknown error occurred")
        }
    }

    private fun bitmapToUri(bitmap: Bitmap): Uri {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val path = MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    fun ByteArray.toUri(context: Context, fileName: String): Uri {
        val tempFile = File.createTempFile(fileName, null, context.cacheDir)
        if (tempFile.createNewFile()) {
            tempFile.outputStream().use { os ->
                os.write(this)
            }
            return tempFile.toUri()
        }
        return Uri.EMPTY
    }

}





//class StorageRepositoryImpl @Inject constructor(private val storageReference: StorageReference,private val storage:FirebaseStorage):
//    StorageRepository {
//
//    override suspend fun uploadImageToFirebaseStorageWithUri(
//        uri: Uri,
//    ): MyResult<String> {
//        return try {
//            val filename =  (uri.lastPathSegment) ?: System.currentTimeMillis()
//            val imageRef = storageReference.child("images/${filename}")
//            val uploadTask = imageRef.putFile(uri)
//            val result: UploadTask.TaskSnapshot = uploadTask.await()
//            val downloadUrl = result.storage.downloadUrl.await()
//            MyResult.Success(downloadUrl.toString())
//        } catch (e: Exception) {
//            MyResult.Error(e.message ?: "Unknown error occurred")
//        }
//    }
//
//    override suspend fun uploadDocumentToFirebaseStorage(uri: Uri): MyResult<String> {
//       return uploadImageToFirebaseStorageWithUri(uri)
//    }
//
//
//
//    override suspend fun uploadImageToFirebaseStorageWithBitmap(bitmap: Bitmap ): MyResult<String> {
//        return try {
//            var n = "images/${System.currentTimeMillis()}"
//            val imageRef = storageReference.child("images/${n}")
//            val byteArray = bitmapToByteArray(bitmap)
//            val uploadTask = imageRef.putBytes(byteArray)
//            val result= uploadTask.await()
//            val downloadUrl = result.storage.downloadUrl.await()
//            MyResult.Success(downloadUrl.toString())
//        } catch (e: Exception) {
//            MyResult.Error(e.message ?: "Unknown error occurred")
//        }
//    }
//
//    override suspend fun uploadImageToFirebaseStorageWithBitmap(bitArray: ByteArray): MyResult<String> {
//        return try {
//            var n = "images/${System.currentTimeMillis()}"
//            val imageRef = storageReference.child("images/${n}")
//            val uploadTask = imageRef.putBytes(bitArray)
//            val result= uploadTask.await()
//            val downloadUrl = result.storage.downloadUrl.await()
//            MyResult.Success(downloadUrl.toString())
//        } catch (e: Exception) {
//            MyResult.Error(e.message ?: "Unknown error occurred")
//        }
//    }
//
//    override suspend fun deleteImageToFirebaseStorage(url: String): MyResult<String> {
//        return try {
//            val storageRef = storage.getReferenceFromUrl(url)
//            val deleteTask: Task<Void> = storageRef.delete()
//            Tasks.await(deleteTask)
//            MyResult.Success("Image deleted successfully")
//        } catch (e: Exception) {
//            e.printStackTrace()
//            MyResult.Error("Failed to delete image: ${e.message}")
//        }
//    }
//
//
//
//    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
//        return byteArrayOutputStream.toByteArray()
//    }
//
//
//
//    override suspend fun uploadVideoToFirebaseStorage(
//        videoUri: Uri,
//        progressCallBack: (Int) -> Unit
//    ): MyResult<String> {
//        return try {
//            val storageReference = Firebase.storage.reference
//            val uploadTask = storageReference.child("${System.currentTimeMillis()}.mp4").putFile(videoUri)
//            uploadTask.addOnProgressListener { taskSnapshot ->
//                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
//                progressCallBack(progress)
//            }
//            val result = uploadTask.await()
//            val downloadUrl = result.storage.downloadUrl.await()
//            MyResult.Success(downloadUrl.toString())
//        } catch (e: Exception) {
//            MyResult.Error(e.message ?: "Unknown error occurred")
//        }
//    }
//
//}