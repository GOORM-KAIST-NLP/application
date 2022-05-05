package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.*

fun getCurrentTimeText(): String {
    return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
}