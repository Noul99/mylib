package com.lymors.lycommons.utils

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.lymors.lycommons.databinding.PickImageDialogBinding

object MyImagePicker {
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var pickMultipleImageLauncher: ActivityResultLauncher<String>


    private var onImagePicked: ((Uri?) -> Unit)? = null
    private var onMultipleImagePicked: ((List<Uri>?) -> Unit)? = null
    private var imageUri: Uri? = null


    fun registerActivityForImageLauncher(activity: FragmentActivity) {
        if (this::pickImageLauncher.isInitialized) {
            return
        }
        pickImageLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    val uri = result.data?.data ?: imageUri
                    onImagePicked?.invoke(uri)
                } else {
                    onImagePicked?.invoke(null)
                }
            }
    }

    fun registerActivityForMultipleImagesLauncher(activity: FragmentActivity) {
        if (this::pickMultipleImageLauncher.isInitialized) {
            return
        }
        pickMultipleImageLauncher =
            activity.registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uriList ->
                onMultipleImagePicked?.invoke(uriList)
            }
    }


    fun View.pickImageByGallery(activity: FragmentActivity,onImagePicked: (Uri?) -> Unit) {
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        setOnClickListener {
            this@MyImagePicker.onImagePicked = onImagePicked
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }
    }

    fun View.pickImageByCamera(activity: FragmentActivity,onImagePicked: (Uri?) -> Unit) {
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        setOnClickListener {
            this@MyImagePicker.onImagePicked = onImagePicked
            imageUri = createImageUri(context)
            imageUri?.let {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, it)
                pickImageLauncher.launch(intent)
            } ?: this@MyImagePicker.onImagePicked?.invoke(null)
        }
    }


    fun View.pickImageByGalleryCropped(activity: FragmentActivity, onImagePicked: (Uri?) -> Unit) {
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        this.setOnClickListener {
            this@MyImagePicker.onImagePicked = onImagePicked
            ImagePicker.with(activity)
                .galleryOnly()
                .crop()
                .createIntent { intent ->
                    pickImageLauncher.launch(intent)
                }
        }
    }

    fun View.pickImageByCameraCropped(activity: FragmentActivity, onImagePicked: (Uri?) -> Unit) {
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        this.setOnClickListener {
            this@MyImagePicker.onImagePicked = onImagePicked
            ImagePicker.with(activity)
                .cameraOnly()
                .crop()
                .createIntent { intent ->
                    pickImageLauncher.launch(intent)
                }
        }
    }


    fun View.pickImageMultiple(onMultipleImage: (List<Uri>?) -> Unit) {
        if (this.context is FragmentActivity) {
            registerActivityForMultipleImagesLauncher(this.context as FragmentActivity)
        }
        setOnClickListener {
            onMultipleImagePicked = onMultipleImage
            pickMultipleImageLauncher.launch("image/*")
        }
    }

    fun View.pickVideo(callback: (Uri?) -> Unit) {
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        onImagePicked = callback
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        pickImageLauncher.launch(intent)


    }

    fun View.pickDocument(callback: (Uri?) -> Unit) {
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        onImagePicked = callback
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "/"
        pickImageLauncher.launch(intent)
    }

    private fun createImageUri(context: Context): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        return context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
            )
        }

    fun View.pickImageByBothCropped(activity:FragmentActivity,onImagePicked: (Uri?) -> Unit = {}){
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        this.setOnClickListener {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val binding = PickImageDialogBinding.inflate(activity.layoutInflater)
            dialog.setContentView(binding.root)


            binding.pickFromGallery.pickImageByGalleryCropped(activity) {
                onImagePicked(it)
                dialog.dismiss()
            }

            binding.useCamera.pickImageByCameraCropped(activity) {
                onImagePicked(it)
                dialog.dismiss()
            }


            // Calculate 90% of the screen width
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val dialogWidth = (screenWidth * 0.92).toInt()



            val windowParams = WindowManager.LayoutParams().apply {
                copyFrom(dialog.window!!.attributes)
                width = dialogWidth
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
            }

            dialog.window!!.attributes = windowParams
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.attributes.windowAnimations = com.lymors.lycommons.R.style.DialogAnimation
            dialog.show()
            }
        }



    fun View.pickImageByBoth(activity:FragmentActivity,onImagePicked: (Uri?) -> Unit = {}){
        if (this.context is FragmentActivity) {
            registerActivityForImageLauncher(this.context as FragmentActivity)
        }
        this.setOnClickListener {
            val dialog = Dialog(activity)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val binding = PickImageDialogBinding.inflate(activity.layoutInflater)
            dialog.setContentView(binding.root)


            binding.pickFromGallery.pickImageByGallery(activity) {
                onImagePicked(it)
                dialog.dismiss()
            }

            binding.useCamera.pickImageByCamera(activity) {
                onImagePicked(it)
                dialog.dismiss()
            }

            // Calculate 90% of the screen width
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            val screenWidth = displayMetrics.widthPixels
            val dialogWidth = (screenWidth * 0.92).toInt()



            val windowParams = WindowManager.LayoutParams().apply {
                copyFrom(dialog.window!!.attributes)
                width = dialogWidth
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM
            }

            dialog.window!!.attributes = windowParams
            dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window!!.attributes.windowAnimations = com.lymors.lycommons.R.style.DialogAnimation
            dialog.show()
            }
        }



}
