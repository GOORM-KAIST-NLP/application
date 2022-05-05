package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivitySttBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class STTActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySttBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySttBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sttupButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
            }

            if (intent.resolveActivity(this.packageManager) == null) {
                Log.d("TAG", "사용 가능한 파일 탐색기가 없습니다")
                Toast.makeText(this, "사용 가능한 파일 탐색기가 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivityForResult(
                intent,
                PICKFILE_RESULT_CODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICKFILE_RESULT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
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
                            uploadFile(tempFile)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        val fileType: String? = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
    }

    private fun uploadFile(file: File) {
        lifecycleScope.launch {
            try {
                binding.progressBar.isVisible = true
                binding.sttupButton.isEnabled = false

                val fileBody = file.asRequestBody("audio/wav".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, fileBody)

                val response = NetworkManager.sttApi.uploadAudios(
                    listOf(part)
                )

                binding.originalTextView.text = response.originalText
                binding.tsTextView.text = response.transText
                binding.progressBar.isVisible = false
                binding.sttupButton.isEnabled = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@STTActivity, "${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.isVisible = false
                binding.sttupButton.isEnabled = true
            }
        }
    }

    companion object {
        const val PICKFILE_RESULT_CODE = 1001
    }
}