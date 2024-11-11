package com.dicoding.asclepius.view

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageUri: Uri? = null
    private lateinit var imageClassifier: ImageClassifierHelper

    @Suppress("DEPRECATION")
    private val cameraResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val capturedImage = result.data?.extras?.get("data") as? Bitmap
            capturedImage?.let {
                binding.previewImageView.setImageBitmap(it)
                imageUri = saveImage(it)
            }
        }
    }

    private val galleryResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                imageUri = uri
                displayImage(uri)
            }
        }
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            showToast("Kamera perlu izin untuk mengambil gambar.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageClassifier = ImageClassifierHelper(context = this)

        binding.cameraButton.setOnClickListener { requestCameraPermission() }
        binding.galleryButton.setOnClickListener { openGallery() }
        binding.analyzeButton.setOnClickListener { analyze() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        galleryResult.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraResult.launch(intent)
    }

    private fun displayImage(uri: Uri) {
        binding.previewImageView.setImageURI(uri)
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun saveImage(bitmap: Bitmap): Uri? {
        val contentResolver = contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Image")
            put(MediaStore.Images.Media.DESCRIPTION, "Image captured from camera")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        imageUri?.let {
            contentResolver.openOutputStream(it)?.let { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }

        return imageUri
    }

    private fun analyze() {
        imageUri?.let { uri ->
            imageClassifier.classifyImage(uri) { result, confidence ->
                val intent = Intent(this, ResultActivity::class.java).apply {
                    putExtra("RESULT", result)
                    putExtra("CONFIDENCE", confidence)
                    putExtra("IMAGE_URI", uri.toString())
                }
                startActivity(intent)
            }
        } ?: showToast("Pilih gambar terlebih dahulu")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
