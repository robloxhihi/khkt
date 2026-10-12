package com.example.service.sos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R

private const val TAG = "SmsBridgeReceiver"
private const val CHANNEL_ID = "vespera_sms_bridge_channel"

/**
 * BroadcastReceiver nhận tin nhắn SMS dự phòng từ thiết bị người thân khi mất mạng / ngoài 50m,
 * chuyển hóa SMS thành tin nhắn chat hoặc cảnh báo SOS hiển thị trực tiếp vào giao diện app.
 */
class SmsBridgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (sms in messages) {
                    val senderNumber = sms.originatingAddress ?: continue
                    val messageBody = sms.messageBody ?: continue

                    Log.d(TAG, "Nhận SMS từ $senderNumber: $messageBody")

                    if (messageBody.contains("[VESPERA_CHAT]") || messageBody.contains("[SOS]") || messageBody.contains("GIA ĐÌNH VIỆT")) {
                        showSmsBridgeNotification(context, senderNumber, messageBody)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xử lý SMS đến: ${e.message}", e)
            }
        }
    }

    private fun showSmsBridgeNotification(context: Context, sender: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cầu nối SMS & Khẩn cấp Vespera",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nhận thông điệp chat và SOS qua SMS dự phòng khi mất kết nối"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("💬 Tin nhắn cầu nối SMS từ gia đình")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
