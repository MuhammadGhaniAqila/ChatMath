package com.smktelkom.mathchat3.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Base64
import java.io.ByteArrayOutputStream

object AvatarGenerator {

    /**
     * Mengubah Bitmap menjadi String Base64.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * Mengubah String Base64 kembali menjadi Bitmap.
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Membuat Bitmap Avatar bulat otomatis berukuran 200x200px dengan inisial nama.
     */
    fun generateAvatarBitmap(name: String): Bitmap {
        val width = 200
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Latar belakang Maroon
        val bgPaint = Paint().apply {
            color = Color.parseColor("#881337") // Maroon
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, bgPaint)

        // Inisial Nama (misal: "Andi Wijaya" -> "AW", "Budi" -> "B")
        val initials = name.trim().split("\\s+".toRegex())
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .take(2)
            .joinToString("")
            .ifEmpty { "C" }

        // Teks Inisial
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 72f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val yPos = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initials, width / 2f, yPos, textPaint)

        return bitmap
    }

    /**
     * Membuat String Base64 Avatar otomatis dari nama.
     */
    fun generateAvatarBase64(name: String): String {
        val bitmap = generateAvatarBitmap(name)
        return bitmapToBase64(bitmap)
    }
}
