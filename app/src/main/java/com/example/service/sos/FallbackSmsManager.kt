package com.example.service.sos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log

private const val TAG = "FallbackSmsManager"
private const val PREFS_NAME = "vespera_emergency_prefs"
private const val KEY_EMERGENCY_PHONE = "emergency_phone_number"
private const val KEY_RESCUE_HOTLINE = "rescue_hotline_number"

/**
 * Quản lý gửi tin nhắn khẩn cấp qua SMS dự phòng khi mất kết nối Internet
 * và ngoài vùng phủ sóng Mesh (>50m, hoàn toàn cô lập).
 */
class FallbackSmsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Lấy số điện thoại người thân khẩn cấp đã lưu
     */
    fun getEmergencyPhoneNumber(): String {
        return prefs.getString(KEY_EMERGENCY_PHONE, "") ?: ""
    }

    /**
     * Lưu số điện thoại người thân khẩn cấp
     */
    fun saveEmergencyPhoneNumber(phoneNumber: String) {
        prefs.edit().putString(KEY_EMERGENCY_PHONE, phoneNumber.trim()).apply()
    }

    /**
     * Danh sách đầu số cứu hộ khẩn cấp quốc gia
     */
    val emergencyHotlines = listOf(
        HotlineInfo("112", "Cứu nạn, Cứu hộ Quốc gia (Tìm kiếm cứu nạn đa năng)", "🆘"),
        HotlineInfo("115", "Cấp cứu Y tế (Khẩn cấp y khoa, tai nạn)", "🚑"),
        HotlineInfo("114", "Cứu hỏa, Phòng cháy & Sự cố sập đổ", "🚒"),
        HotlineInfo("113", "Công an / An ninh trật tự khẩn cấp", "👮")
    )

    data class HotlineInfo(
        val number: String,
        val label: String,
        val icon: String
    )

    /**
     * [Lựa chọn 1] Gửi tin nhắn SMS khẩn cấp tới số điện thoại người thân.
     * Tự động đính kèm liên kết Google Maps và tọa độ GPS thực tế.
     */
    fun sendEmergencySmsToFamily(
        latitude: Double,
        longitude: Double,
        senderName: String,
        customMessage: String? = null
    ): SmsSendResult {
        val targetPhone = getEmergencyPhoneNumber()
        if (targetPhone.isBlank()) {
            return SmsSendResult(
                isSuccess = false,
                errorMessage = "Chưa cài đặt số điện thoại người thân khẩn cấp trong Cài đặt."
            )
        }

        val mapsLink = "https://maps.google.com/?q=%.6f,%.6f".format(latitude, longitude)
        val smsBody = buildString {
            append("[GIA ĐÌNH VIỆT - BÁO ĐỘNG SOS KHẨN CẤP]\n")
            append("Người gửi: ${senderName.ifEmpty { "Thành viên gia đình" }}\n")
            append("Tình trạng: CẦN TRỢ GIÚP GẤP!\n")
            if (!customMessage.isNullOrBlank()) {
                append("Nội dung: $customMessage\n")
            }
            append("Vị trí GPS: $mapsLink\n")
            append("Tọa độ: %.5f, %.5f".format(latitude, longitude))
        }

        return dispatchSms(targetPhone, smsBody)
    }

    /**
     * Gửi tin nhắn chat qua SMS dự phòng khi mất kết nối Internet và ngoài 50m
     */
    fun sendChatViaSms(senderName: String, messageText: String): SmsSendResult {
        val targetPhone = getEmergencyPhoneNumber()
        if (targetPhone.isBlank()) {
            return SmsSendResult(
                isSuccess = false,
                errorMessage = "Chưa cài đặt số điện thoại người thân khẩn cấp để gửi SMS chat."
            )
        }
        val smsBody = "[VESPERA_CHAT] $senderName: $messageText"
        return dispatchSms(targetPhone, smsBody)
    }

    /**
     * [Lựa chọn 2] Gửi tin nhắn SOS khẩn cấp tới đường dây nóng cứu hộ (hoặc chuẩn bị cú pháp gửi cứu hộ).
     */
    fun sendEmergencySmsToRescue(
        hotline: String,
        latitude: Double,
        longitude: Double,
        senderName: String,
        details: String = "Cần cứu hộ khẩn cấp tại vị trí này!"
    ): SmsSendResult {
        val mapsLink = "https://maps.google.com/?q=%.6f,%.6f".format(latitude, longitude)
        val body = "[SOS CUU HO $hotline] $senderName can cuu ho! Toa do: $mapsLink - $details"
        return dispatchSms(hotline, body)
    }

    /**
     * Mở ứng dụng gọi điện trực tiếp tới Hotline cứu hộ
     */
    fun makeDirectEmergencyCall(hotline: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$hotline")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Không thể mở trình gọi điện thoại: ${e.message}")
        }
    }

    private fun dispatchSms(destinationAddress: String, text: String): SmsSendResult {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val parts = smsManager.divideMessage(text)
            smsManager.sendMultipartTextMessage(
                destinationAddress,
                null,
                parts,
                null,
                null
            )
            Log.d(TAG, "Đã gửi SMS thành công tới $destinationAddress")
            SmsSendResult(isSuccess = true, targetPhone = destinationAddress)
        } catch (e: SecurityException) {
            Log.e(TAG, "Lỗi bảo mật khi gửi SMS (chưa cấp quyền SEND_SMS)", e)
            SmsSendResult(
                isSuccess = false,
                errorMessage = "Ứng dụng chưa được cấp quyền 'Gửi tin nhắn SMS' trong hệ thống."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi SMS: ${e.message}", e)
            SmsSendResult(
                isSuccess = false,
                errorMessage = "Không thể gửi SMS: ${e.localizedMessage ?: "Lỗi sóng điện thoại"}"
            )
        }
    }

    data class SmsSendResult(
        val isSuccess: Boolean,
        val targetPhone: String = "",
        val errorMessage: String? = null
    )
}
