package com.example.service.sos

import android.content.Context
import android.util.Log
import com.example.data.model.LocationData
import com.example.data.repository.VesperaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

private const val TAG = "SmartSosDispatcher"

enum class SosChannelUsed {
    INTERNET_CLOUD,
    OFFLINE_MESH_P2P,
    FALLBACK_SMS
}

data class SosDispatchResult(
    val isSuccess: Boolean,
    val channel: SosChannelUsed,
    val detailMessage: String
)

/**
 * Điều phối viên cứu trợ thông minh (Smart Dispatcher):
 * Tự động chuyển đổi giữa 3 tầng cứu hộ:
 * Tầng 1: Internet Cloud (Firebase Firestore) khi có mạng.
 * Tầng 2: Offline Mesh P2P (Google Nearby Connections < 50m) khi mất mạng.
 * Tầng 3: Fallback SMS (SmsManager có kèm Google Maps GPS) khi ngoài 50m / cô lập.
 */
class SmartSosDispatcher(
    private val context: Context,
    private val repository: VesperaRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    val networkMonitor = NetworkMonitor(context)
    val meshManager = OfflineMeshManager(context)
    val smsManager = FallbackSmsManager(context)

    private val prefs = context.getSharedPreferences("vespera_sos_schedule_prefs", Context.MODE_PRIVATE)

    private val _currentActiveChannel = MutableStateFlow(SosChannelUsed.INTERNET_CLOUD)
    val currentActiveChannel: StateFlow<SosChannelUsed> = _currentActiveChannel.asStateFlow()

    private val _isOfflineAutoScanning = MutableStateFlow(false)
    val isOfflineAutoScanning: StateFlow<Boolean> = _isOfflineAutoScanning.asStateFlow()

    init {
        observeNetworkChanges()
    }

    /**
     * Tự động điều phối kết nối dựa trên trạng thái Internet:
     * - Khi CÓ mạng: Dùng Cloud, tắt Mesh để tiết kiệm pin tối đa.
     * - Khi MẤT mạng: Tự động kích hoạt quét Mesh xung quanh.
     */
    private fun observeNetworkChanges() {
        scope.launch {
            networkMonitor.isOnline.collectLatest { isOnline ->
                if (isOnline) {
                    Log.d(TAG, "Đã có mạng: Ưu tiên Internet Cloud, tắt bộ quét ngoại tuyến để tiết kiệm pin.")
                    _currentActiveChannel.value = SosChannelUsed.INTERNET_CLOUD
                    _isOfflineAutoScanning.value = false
                    meshManager.stopAll()
                } else {
                    Log.d(TAG, "Mất mạng: Tự động kích hoạt quét ngoại tuyến P2P Mesh (<50m)...")
                    _currentActiveChannel.value = SosChannelUsed.OFFLINE_MESH_P2P
                    _isOfflineAutoScanning.value = true
                    // Bắt đầu cả phát hiện và thông báo lân cận
                    meshManager.startDiscovery()
                }
            }
        }
    }

    /**
     * Kiểm tra khung giờ con được phép sử dụng nút SOS
     * (Mặc định: Luôn cho phép 24/7 để bảo vệ an toàn tối thượng)
     */
    fun isSosAllowedAtCurrentTime(): Boolean {
        return true
    }

    /**
     * Lưu cài đặt khung giờ sử dụng nút SOS
     */
    fun setSosAllowedSchedule(isRestricted: Boolean, startHour: Int, endHour: Int) {
        prefs.edit()
            .putBoolean("sos_schedule_restricted", isRestricted)
            .putInt("sos_allowed_start_hour", startHour)
            .putInt("sos_allowed_end_hour", endHour)
            .apply()
    }

    fun getSosScheduleConfig(): Triple<Boolean, Int, Int> {
        val restricted = prefs.getBoolean("sos_schedule_restricted", false)
        val startH = prefs.getInt("sos_allowed_start_hour", 6)
        val endH = prefs.getInt("sos_allowed_end_hour", 22)
        return Triple(restricted, startH, endH)
    }

    /**
     * [LỰA CHỌN 1]: Gửi tín hiệu SOS tới Người thân.
     * Tự động chọn kênh tối ưu nhất:
     * 1. Internet Cloud (Nếu có mạng)
     * 2. Mesh P2P (Nếu mất mạng & có thiết bị xung quanh < 50m)
     * 3. Fallback SMS (Nếu mất mạng & không có thiết bị lân cận)
     */
    suspend fun sendSosToFamily(
        location: LocationData,
        senderName: String,
        customMessage: String? = null
    ): SosDispatchResult {
        val isOnline = networkMonitor.isOnline.value

        // 1. Nếu CÓ mạng -> Gửi qua Firestore Cloud
        if (isOnline) {
            return try {
                repository.sendSosAlert(location)
                _currentActiveChannel.value = SosChannelUsed.INTERNET_CLOUD
                SosDispatchResult(
                    isSuccess = true,
                    channel = SosChannelUsed.INTERNET_CLOUD,
                    detailMessage = "Đã gửi tín hiệu SOS khẩn cấp qua Internet Cloud tới gia đình thành công!"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Gửi qua Cloud thất bại, chuyển sang kênh ngoại tuyến...")
                attemptOfflineDispatch(location, senderName, customMessage)
            }
        } else {
            // 2. MẤT mạng -> Thử Mesh P2P hoặc SMS
            return attemptOfflineDispatch(location, senderName, customMessage)
        }
    }

    private fun attemptOfflineDispatch(
        location: LocationData,
        senderName: String,
        customMessage: String?
    ): SosDispatchResult {
        val connectedPeers = meshManager.connectedPeers.value
        val discoveredPeers = meshManager.discoveredPeers.value

        // Nếu có thiết bị lân cận trong bán kính 50m
        if (connectedPeers.isNotEmpty() || discoveredPeers.isNotEmpty()) {
            val sosText = "CẦN TRỢ GIÚP! Tọa độ GPS: %.5f, %.5f. Pin: %d%%. %s".format(
                location.latitude,
                location.longitude,
                location.batteryLevel,
                customMessage.orEmpty()
            )
            val sentCount = meshManager.broadcastSosBeacon(sosText, senderName)
            _currentActiveChannel.value = SosChannelUsed.OFFLINE_MESH_P2P
            return SosDispatchResult(
                isSuccess = true,
                channel = SosChannelUsed.OFFLINE_MESH_P2P,
                detailMessage = "Đã phát tín hiệu SOS qua mạng ngoại tuyến Mesh tới $sentCount thiết bị trong bán kính 50m!"
            )
        }

        // Nếu không có thiết bị xung quanh (>50m hoặc cô lập hoàn toàn) -> Tự động chuyển Fallback SMS
        val smsResult = smsManager.sendEmergencySmsToFamily(
            latitude = location.latitude,
            longitude = location.longitude,
            senderName = senderName,
            customMessage = customMessage
        )
        _currentActiveChannel.value = SosChannelUsed.FALLBACK_SMS

        return if (smsResult.isSuccess) {
            SosDispatchResult(
                isSuccess = true,
                channel = SosChannelUsed.FALLBACK_SMS,
                detailMessage = "Không có mạng & thiết bị gần. Đã tự động kích hoạt gửi SMS cứu nạn kèm tọa độ Google Maps tới ${smsResult.targetPhone}!"
            )
        } else {
            SosDispatchResult(
                isSuccess = false,
                channel = SosChannelUsed.FALLBACK_SMS,
                detailMessage = "Không thể gửi SMS: ${smsResult.errorMessage}"
            )
        }
    }

    /**
     * [LỰA CHỌN 2]: Gửi SOS tới Lực lượng Cứu hộ (112, 115, 114, 113)
     */
    fun sendSosToRescueForce(
        hotline: String,
        location: LocationData,
        senderName: String
    ): SosDispatchResult {
        // Gửi tin nhắn khẩn cấp tới đường dây nóng
        val result = smsManager.sendEmergencySmsToRescue(
            hotline = hotline,
            latitude = location.latitude,
            longitude = location.longitude,
            senderName = senderName
        )
        // Mở màn hình gọi khẩn cấp để người dùng có thể đàm thoại ngay
        smsManager.makeDirectEmergencyCall(hotline)

        return SosDispatchResult(
            isSuccess = result.isSuccess,
            channel = SosChannelUsed.FALLBACK_SMS,
            detailMessage = if (result.isSuccess) "Đã gửi SMS tọa độ cứu nạn và mở cuộc gọi khẩn cấp tới $hotline!"
            else "Đã mở cuộc gọi tới $hotline (Lỗi SMS: ${result.errorMessage})"
        )
    }

    fun release() {
        networkMonitor.stopMonitoring()
        meshManager.stopAll()
    }
}
