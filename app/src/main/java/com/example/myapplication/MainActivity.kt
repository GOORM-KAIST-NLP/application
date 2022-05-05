package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ocrButton = findViewById<View>(R.id.ocrButton)
        ocrButton.setOnClickListener{
            val intent = Intent(this, OCRActivity::class.java)
            startActivity(intent)
        }

        val sttButton = findViewById<View>(R.id.sttButton)
        sttButton.setOnClickListener{
            val intent = Intent(this, STTActivity::class.java)
            startActivity(intent)
        }
    }
}