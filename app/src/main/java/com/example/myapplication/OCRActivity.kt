package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivityOcrBinding
import com.example.myapplication.databinding.ItemTransBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class OCRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrBinding
    private val adapter = OCRAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerview.adapter = adapter

        binding.upButton.setOnClickListener {
            dispatchTakePictureIntent()
        }
        binding.geButton.setOnClickListener {
            openGalleryForImage()
        }

        var permission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        var permission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_DENIED || permission2 == PackageManager.PERMISSION_DENIED) {
            // 권한이 없으면 요청
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ), 1
            )
        }
    }

    // 카메라 열기
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            if (takePictureIntent.resolveActivity(this.packageManager) == null) {
                Log.d("TAG", "사용 가능한 카메라 앱이 없습니다")
                Toast.makeText(this, "사용 가능한 카메라 앱이 없습니다", Toast.LENGTH_SHORT).show()
                return
            }

            // 찍은 사진을 그림파일로 만들기
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }

            if (photoFile == null) {
                Log.d("TAG", "카메라 앱을 실행할 수 없습니다")
                Toast.makeText(this, "카메라 앱을 실행할 수 없습니다", Toast.LENGTH_SHORT).show()
                return
            }

            // 그림파일을 성공적으로 만들었다면 onActivityForResult로 보내기
            // 안드로이드 버전에 따라서
            if (Build.VERSION.SDK_INT < 24) {
                val photoURI = Uri.fromFile(photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } else {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this, "com.example.myapplication.fileprovider", photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    // 카메라로 촬영한 이미지를 파일로 저장해준다
    private var currentPhotoPath = ""

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = getCurrentTimeText()
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // OnActivityResult에서 Data(사진)받기
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                if (resultCode == RESULT_OK) {
                    // 카메라로부터 받은 데이터가 있을경우에만
                    val file = File(currentPhotoPath)

                    if (Build.VERSION.SDK_INT < 28) {
                        val bitmap = MediaStore.Images.Media
                            .getBitmap(contentResolver, Uri.fromFile(file))  //Deprecated
                        displayImage(bitmap)
                    } else {
                        val decode = ImageDecoder.createSource(
                            this.contentResolver,
                            Uri.fromFile(file)
                        )
                        val bitmap = ImageDecoder.decodeBitmap(decode)
                        displayImage(bitmap)
                    }
                    uploadImage(file)
                }
            }
            REQUEST_GALLERY_TAKE -> {
                if (resultCode == RESULT_OK && requestCode == REQUEST_GALLERY_TAKE) {
                    val currentImageUri = data?.data
                    try {
                        currentImageUri?.let {
                            if (Build.VERSION.SDK_INT < 28) {
                                val bitmap = MediaStore.Images.Media.getBitmap(
                                            this.contentResolver,
                                        currentImageUri
                                    )
                                displayImage(bitmap)
                            } else {
                            val source =
                                ImageDecoder.createSource(this.contentResolver, currentImageUri)
                                val bitmap = ImageDecoder.decodeBitmap(source)
                                displayImage(bitmap)
                            }
                        }

//                        data?.data?.let { uri ->
//                            val cursor = contentResolver.query(
//                                uri,
//                                arrayOf(MediaStore.Images.Media.DATA),
//                                null,
//                                null,
//                                null
//                            )
//                            if (cursor?.moveToFirst() == true) {
//                                val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
//                                val filePath = cursor.getString(index)
//                                val file = File(filePath)
//                                uploadImage(file) // fixme android 10+
//                            }
//                            cursor?.close()
//                        }

                        data?.data?.let { contentUri ->
                            // Preparing Temp file name
                            val fileExtension = getFileExtension(this, contentUri)
                            val fileName =
                                getCurrentTimeText() + if (fileExtension != null) ".$fileExtension" else ""

                            val tempFile = File(cacheDir, fileName)
                            tempFile.createNewFile()

                            try {
                                val outputStream = tempFile.outputStream()
                                contentResolver.openInputStream(contentUri)?.use {
                                    it.copyTo(outputStream)
                                    outputStream.flush()
                                    uploadImage(tempFile)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        val fileType: String? = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
    }

    //갤러리 열기
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }

    private fun uploadImage(file: File) {
        lifecycleScope.launch {
            try {
                binding.run {
                    progressBar.isVisible = true
                    geButton.isEnabled = false
                    upButton.isEnabled = false
                }

                val fileBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, fileBody)
                val response = NetworkManager.ocrApi.uploadImages(
                    listOf(part)
                )

                displayImage(NetworkManager.IMAGE_PATH + response.url)
                binding.run {
                    adapter.submitList(response.texts)
                    progressBar.isVisible = false
                    geButton.isEnabled = true
                    upButton.isEnabled = true
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@OCRActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                binding.run {
                    progressBar.isVisible = false
                    geButton.isEnabled = true
                    upButton.isEnabled = true
                }
            }
        }
    }

    private fun displayImage(bitmap: Bitmap) {
        binding.cameraIconView.isVisible = false
        binding.imageView.setImageBitmap(bitmap)
    }

    private fun displayImage(url: String?) {
        binding.cameraIconView.isVisible = false
        Picasso.get()
            .load(url)
            .into(binding.imageView, object : Callback {
                override fun onSuccess() {
                    Log.e("TAG", "image load success...")
                }

                override fun onError(e: Exception?) {
                    Log.e("TAG", "image load error...")
                }
            })
    }

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 1001
        const val REQUEST_GALLERY_TAKE = 1002
    }
}

class OCRAdapter : ListAdapter<STTResponse, OCRViewHolder>(object : DiffUtil.ItemCallback<STTResponse>() {
    override fun areItemsTheSame(oldItem: STTResponse, newItem: STTResponse): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: STTResponse, newItem: STTResponse): Boolean {
        return oldItem == newItem
    }
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OCRViewHolder {
        val binding = ItemTransBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OCRViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OCRViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class OCRViewHolder(private val binding: ItemTransBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: STTResponse?) {
        binding.originalTextView.text = item?.originalText
        binding.tsTextView.text = item?.transText
    }
}