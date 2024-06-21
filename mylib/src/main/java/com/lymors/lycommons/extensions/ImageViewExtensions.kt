package com.lymors.lycommons.extensions


import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
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
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.textfield.TextInputLayout
import com.lymors.lycommons.R
import com.lymors.lycommons.extensions.ScreenExtensions.pickedImageUri
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



    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var onImagePickedCallback: ((Uri?) -> Unit)? = null

    fun FragmentActivity.registerLauncherForImageResult() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            pickedImageUri = uri
            onImagePickedCallback?.invoke(uri)
        }
    }

    fun AppCompatActivity.registerLauncherForImageResult() {
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            pickedImageUri = uri
            onImagePickedCallback?.invoke(uri)
        }
    }

    fun View.pickImageInDialog(onImagePicked: (Uri?) -> Unit) {
        this.setOnClickListener {
            onImagePickedCallback = onImagePicked
            pickImageLauncher.launch("image/*")
        }

    }


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


    fun View.pickImageCroppedByCameraAndGallery(
        activity: AppCompatActivity,
        onMediaPicked: (Uri?) -> Unit
    ) {
        val startForProfileImageResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    activity.pickedImageUri = data?.data!!
                    onMediaPicked.invoke(data.data)
                } else if (resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(activity, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }

        this.setOnClickListener {
            ImagePicker.with(activity)
                .compress(1024)
                .crop()//Final image size will be less than 1 MB(Optional)
                .maxResultSize(
                    1080,
                    1080
                )  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }
    }


    fun View.pickImageCropped(activity: AppCompatActivity, onMediaPicked: (Uri?) -> Unit) {
        val startForProfileImageResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    activity.pickedImageUri = data?.data!!
                    onMediaPicked.invoke(data.data)
                } else if (resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(activity, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }

        this.setOnClickListener {
            ImagePicker.with(activity)
                .galleryOnly()
                .crop()
                .compress(1024)         //Final image size will be less than 1 MB(Optional)
                .maxResultSize(
                    1080,
                    1080
                )  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }
    }


    fun View.pickImageByCameraCropped(activity: AppCompatActivity, onMediaPicked: (Uri?) -> Unit) {
        val startForProfileImageResult =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    activity.pickedImageUri = data?.data!!
                    onMediaPicked.invoke(data.data)
                } else if (resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(activity, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }

        this.setOnClickListener {
            ImagePicker.with(activity)
                .cameraOnly()
                .crop()
                .compress(1024)         //Final image size will be less than 1 MB(Optional)
                .maxResultSize(
                    1080,
                    1080
                )  //Final image resolution will be less than 1080 x 1080(Optional)
                .createIntent { intent ->
                    startForProfileImageResult.launch(intent)
                }
        }
    }


//    fun View.pickImage(activity: AppCompatActivity, onMediaPicked: (Uri?) -> Unit) {
//        val pickImageLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            activity.pickedImageUri = uri
//            onMediaPicked.invoke(uri)
//        }
//        this.setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
//    }


    fun View.pickImageByCamera(activity: AppCompatActivity, onMediaPicked: (Uri?) -> Unit) {
        // Create a Uri for the captured image
        val capturedImageUri: Uri? = getCapturedImageUri(activity)
        val takePictureLauncher: ActivityResultLauncher<Uri> =
            activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    activity.pickedImageUri = capturedImageUri
                    onMediaPicked.invoke(capturedImageUri)
                } else {
                    onMediaPicked.invoke(null) // Handle failed capture (optional)
                }
            }

        this.setOnClickListener {
            // Check camera permission before launching intent
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                takePictureLauncher.launch(capturedImageUri)
            } else {
                // Request camera permission if not granted
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_REQUEST_CODE
                )
            }
        }
    }


    private fun getCapturedImageUri(activity: AppCompatActivity): Uri? {
        val storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            val imageFile =
                File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", storageDir)
            FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


//    private fun getCapturedImageUri(activity: AppCompatActivity): Uri? {
//    val tempFile = File(activity.cacheDir, "temp_sticker.webp")
//    var byteArrayOutputStream = ByteArrayOutputStream()
//
//    FileOutputStream(tempFile).use { fos ->
//        fos.write(byteArrayOutputStream.toByteArray())
//    }
//
//    return Uri.fromFile(tempFile)
//    }

    private const val CAMERA_REQUEST_CODE = 102 // Define a uni


    fun View.pickMultipleImages(activity: AppCompatActivity, onMediaPicked: (List<Uri>?) -> Unit) {
        val pickImageLauncher =
            activity.registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uri ->
                onMediaPicked.invoke(uri)
            }
        this.setOnClickListener {
            pickImageLauncher.launch("image/*")
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

    fun View.pickImage(activity: AppCompatActivity, onMediaPicked: (Uri?) -> Unit) {

        val pickImageLauncher =
            activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                activity.pickedImageUri = uri
                onMediaPicked.invoke(uri)
            }
        this.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    fun AppCompatActivity.pickImage(onMediaPicked: (Uri?) -> Unit) {
        val pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                pickedImageUri = uri
                onMediaPicked.invoke(uri)
            }

        // Launch the image picker when needed (e.g., in a button click listener)
        pickImageLauncher.launch("image/*")
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