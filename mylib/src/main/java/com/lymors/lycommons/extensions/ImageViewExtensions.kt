package com.lymors.lycommons.extensions


import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.textfield.TextInputLayout
import com.lymors.lycommons.R
import com.lymors.lycommons.utils.FirebaseUploadWorker
import com.lymors.lycommons.utils.MyExtensions.logT
import com.lymors.lycommons.utils.MyExtensions.toBitmap
import com.lymors.lycommons.utils.Utils.saveImageToInternalStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.math.floor
import kotlin.math.sqrt

object ImageViewExtensions {


    fun uploadImageUsingWorkManager(context: Context, uri: String, path:String = "") {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val data = Data.Builder()
            .putString("uri", uri.toUri().saveImageToInternalStorage(context).toString())
            .putString("path", path)
            .build()

        val uploadWorkRequest = OneTimeWorkRequestBuilder<FirebaseUploadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        var workManager = WorkManager.getInstance(context)
        workManager.enqueue(uploadWorkRequest)

        val statusLiveData: LiveData<WorkInfo> = workManager.
        getWorkInfoByIdLiveData(uploadWorkRequest.id)
//        //        You can also use the id for cancellation:
//        workManager.cancelWorkById(workRequest.id);

        statusLiveData.observe(context as LifecycleOwner) { workInfo ->
            if (workInfo != null) {
                val status = workInfo.state
                val outputData = workInfo.outputData
               status.name.logT("status")
                outputData.logT("outputData")
            }
        }



    }


    fun TextInputLayout.applyError(message: String = "This field is required") {
        val errorColor = ContextCompat.getColorStateList(context, R.color.red)
        this.apply {
            error = message
            isErrorEnabled = true
            boxStrokeErrorColor = errorColor // Set error color
        }
    }



    fun EditText.scrollByY(scrollView: ScrollView, byY: Int = 400) {
        this.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                scrollView.scrollBy(0, byY)
            }
        }
    }

    fun ScrollView.scrollLittle(list: List<EditText>, byY: Int = 100) {
        list.forEach {
            it.scrollByY(this, byY)
        }
    }


    fun Bitmap?.orEmpty(
        defaultValue: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    ): Bitmap = this ?: defaultValue


    // imageview
    fun ImageView.loadImageFromUrl(
        url: String,
        placeHolder: Int = R.drawable.ic_launcher_background,
        error: Int = R.drawable.ic_launcher_background
    ) {
        Glide.with(this.context)
            .load(url)
            .placeholder(placeHolder)
            .error(error)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(this)

    }


    fun ImageView.loadImageFromResource(resourceId: Int) {
        Glide.with(this.context)
            .load(resourceId)
            .into(this)
    }


    fun ImageView.makeCircular() {
        Glide.with(this.context)
            .load(this.drawable)
            .apply(RequestOptions.circleCropTransform())
            .into(this)
    }

    // Extension function to load a drawable resource into an ImageView using Glide
    fun ImageView.loadDrawable(@DrawableRes resId: Int) {
        Glide.with(this.context)
            .load(resId)
            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
            .into(this)
    }

    fun ImageView.setRoundedCorner(cornerRadius: Float) {
        Glide.with(this.context)
            .load(this.drawable)
            .apply(RequestOptions().transform(RoundedCorners(cornerRadius.toInt())))
            .into(this)
    }


    fun ImageView.setTintColor(color: Int) {
        setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    fun ImageView.setGrayscale() {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(matrix)
        colorFilter = filter
    }


    fun ImageView.loadThumbnail(videoUrl: String, frame: Long = 2000) {

        Glide.with(context).setDefaultRequestOptions(RequestOptions().frame(frame)).load(videoUrl)
            .into(this)
    }




    // Extension function to load a circular image into an ImageView using Glide
    fun ImageView.loadCircularImage(url: String, placeholderResId: Int) {
        Glide.with(context)
            .load(url)
            .apply(RequestOptions.circleCropTransform())
            .placeholder(placeholderResId)
            .into(this)
    }


    fun ImageView.loadResizedImage(url: String, width: Int, height: Int) {
        Glide.with(context)
            .load(url)
            .override(width, height)
            .into(this)
    }


    @SuppressLint("Range")
    fun Uri.getFileName(contentResolver: ContentResolver): String {
        var result: String? = null
        if (scheme == "content") {
            val cursor: Cursor? = contentResolver.query(this, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } catch (e: IOException) {
                e.printStackTrace()
                cursor?.close()
            }
        }
        if (result == null) {
            result = path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }


    fun Bitmap.scaleBitmap(maxBytes: Long = 2097152.toLong()): Bitmap {
        val currentWidth = this.width
        val currentHeight = this.height
        val currentPixels = currentWidth * currentHeight
        val maxPixels = maxBytes / 4
        if (currentPixels <= maxPixels) {
            return this
        }
        val scaleFactor = sqrt(maxPixels / currentPixels.toDouble())
        val newWidthPx = floor(currentWidth * scaleFactor).toInt()
        val newHeightPx = floor(currentHeight * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(this, newWidthPx, newHeightPx, true)
    }

    enum class ImageFormat {
        PNG,
        JPEG
    }

    fun Drawable.saveAsImageFile(context: Context, fileName: String, format: ImageFormat): File? {
        val bitmap = this.toBitmap()
        var outputStream: OutputStream? = null
        var file: File? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        "$fileName.${format.name.toLowerCase()}"
                    )
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/${format.name.toLowerCase()}")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/myicons"
                    )
                }
                val uri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = resolver.openOutputStream(uri)
                    file = File(uri.path)
                }
            } else {
                val storageDir =
                    File(Environment.getExternalStorageDirectory().toString() + "/myicons")
                if (!storageDir.exists()) {
                    storageDir.mkdirs()
                }
                file = File(storageDir, "$fileName.${format.name.toLowerCase()}")
                outputStream = FileOutputStream(file)
            }

            outputStream?.use { out ->
                when (format) {
                    ImageFormat.PNG -> {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    ImageFormat.JPEG -> {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            file?.delete()
            return null
        } finally {
            outputStream?.close()
        }

        return file
    }
}