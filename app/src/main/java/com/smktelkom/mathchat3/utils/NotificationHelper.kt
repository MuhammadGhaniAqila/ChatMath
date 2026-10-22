package com.smktelkom.mathchat3.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.smktelkom.mathchat3.ChatActivity
import com.smktelkom.mathchat3.R
import com.smktelkom.mathchat3.models.User

object NotificationHelper {

    private const val CHANNEL_ID = "chatmath_messages_channel"
    private const val CHANNEL_NAME = "Notifikasi Pesan ChatMath"
    private const val CHANNEL_DESC = "Notifikasi pesan rahasia terenkripsi masuk"

    /**
     * Membuat NotificationChannel dengan tingkat kepentingan tinggi (Heads-Up / Pop-Up)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.parseColor("#881337") // Maroon
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Tampilkan notifikasi pop-up melayang (Heads-Up Notification) saat pesan baru masuk dari HP lain
     */
    fun showNewMessageNotification(
        context: Context,
        senderId: String,
        senderName: String,
        plaintext: String,
        ciphertext: String,
        senderUser: User
    ) {
        createNotificationChannel(context)

        // PendingIntent mengarah langsung ke ChatActivity dengan user pengirim
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("user", senderUser)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderId.hashCode(),
            intent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_send)
            .setContentTitle("Pesan Rahasia dari $senderName")
            .setContentText("$plaintext (Cipher: $ciphertext)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$plaintext\nCipher: $ciphertext")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Menggunakan ID numerik dari senderId agar notifikasi dari pengirim yang sama diperbarui
        val notificationId = senderId.hashCode()
        notificationManager.notify(notificationId, builder.build())
    }
}
