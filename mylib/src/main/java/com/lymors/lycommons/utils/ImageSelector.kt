package com.lymors.lycommons.utils

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageSelector(
    private val context: AppCompatActivity,
    private val imageView: View,
    private var sourceType: SourceType = SourceType.BOTH,
    private val callback: (Uri?) -> Unit
) {


    enum class SourceType {
        BOTH, GALLERY_ONLY, CAMERA_ONLY
    }

    private var currentPhotoUri: Uri? = null
    private lateinit var getContentLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    init {
            registerActivityResultLaunchers(context)
    }

    private fun registerActivityResultLaunchers(activity: FragmentActivity) {
        getContentLauncher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            callback(uri)
        }

        takePictureLauncher = activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback(currentPhotoUri)
                }
            } else {
                callback(null)
            }
        }
    }

    fun start() {
        imageView.setOnClickListener {
            when (sourceType) {
                SourceType.BOTH -> showOptionsDialog()
                SourceType.GALLERY_ONLY -> launchGallery()
                SourceType.CAMERA_ONLY -> launchCamera()
            }
        }
    }


    private fun showOptionsDialog() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(context)
            .setTitle("Select From")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchGallery()
                    1 -> launchCamera()
                }
            }
            .show()
    }

    private fun launchGallery() {
        getContentLauncher.launch("image/*")
    }


    private fun launchCamera() {
        val photoFile = createImageFile(context)
        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            if (currentPhotoUri != null){
                takePictureLauncher.launch(currentPhotoUri!!)
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(context: Context): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    class Builder(private val context: AppCompatActivity, private val imageView: View) {
        private var sourceType: SourceType = SourceType.BOTH
        private var callback: (Uri?) -> Unit = {}

        fun galleryOnly(): Builder {
            sourceType = SourceType.GALLERY_ONLY
            return this
        }

        fun cameraOnly(): Builder {
            sourceType = SourceType.CAMERA_ONLY
            return this
        }

        fun onPicked(callback: (Uri?) -> Unit) {
            this.callback = callback
            ImageSelector(context, imageView, sourceType, callback).start()
        }
    }
}

fun View.pickNewImage(context: AppCompatActivity): ImageSelector.Builder {
    return ImageSelector.Builder(context, this)
}