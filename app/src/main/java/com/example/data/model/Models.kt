package com.example.data.model

enum class UserRole {
    PARENT,
    CHILD
}

enum class PairingStatus {
    UNPAIRED,
    PAIRING_PENDING,
    PAIRED,
    UNPAIRING
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class EmotionTag(val label: String, val emoji: String) {
    HAPPY("Vui vẻ", "😊"),
    NORMAL("Bình thường", "😐"),
    STUDY_STRESS("Áp lực học tập", "📚"),
    NEED_SUPPORT("Cần hỗ trợ", "🆘")
}

data class UserProfile(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val role: UserRole = UserRole.PARENT,
    val studentCode: String = "",
    val verificationPin: String = "",
    val pairedWith: String = "",
    val activePairingId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Pairing(
    val id: String = "",
    val parentId: String = "",
    val childId: String = "",
    val studentCode: String = "",
    val parentName: String = "",
    val childName: String = "",
    val status: PairingStatus = PairingStatus.UNPAIRED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class PairingRequest(
    val id: String = "",
    val studentCode: String = "",
    val parentId: String = "",
    val parentName: String = "",
    val parentEmail: String = "",
    val childId: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

data class LocationData(
    val userId: String = "",
    val pairingId: String = "",
    val userName: String = "",
    val latitude: Double = 21.0285, // Default Hanoi center coord if not set yet
    val longitude: Double = 105.8542,
    val accuracy: Float = 12.0f,
    val speed: Float = 0.0f,
    val batteryLevel: Int = 85,
    val isCharging: Boolean = false,
    val isPrivacyActive: Boolean = false,
    val currentSafeZone: String = "Trường học",
    val topActiveApp: String = "YouTube",
    val topActiveAppMinutes: Int = 45,
    val totalUsedMinutes: Int = 65,
    val isSos: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class UsageSchedule(
    val id: String = "",
    val pairingId: String = "",
    val title: String = "Lịch ngày thường",
    val dailyTargetMinutes: Int = 120,
    val usedMinutes: Int = 45,
    val allowedStartHour: Int = 7,
    val allowedStartMinute: Int = 0,
    val allowedEndHour: Int = 21,
    val allowedEndMinute: Int = 30,
    val daysOfWeek: String = "T2, T3, T4, T5, T6, T7, CN",
    val isReminderOnly: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExtraTimeRequest(
    val id: String = "",
    val pairingId: String = "",
    val childId: String = "",
    val childName: String = "",
    val minutesRequested: Int = 30,
    val reason: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val respondedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class PrivacyRequest(
    val id: String = "",
    val pairingId: String = "",
    val childId: String = "",
    val childName: String = "",
    val startHour: Int = 14,
    val startMinute: Int = 0,
    val endHour: Int = 16,
    val endMinute: Int = 0,
    val reason: String = "",
    val status: RequestStatus = RequestStatus.PENDING,
    val respondedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class FamilyMessage(
    val id: String = "",
    val pairingId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: UserRole = UserRole.PARENT,
    val text: String = "",
    val emotion: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val isStarred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AppCategoryUsage(
    val categoryName: String,
    val minutes: Int,
    val iconName: String,
    val percentage: Float
)

data class DetailedAppUsage(
    val packageName: String,
    val appName: String,
    val minutesUsed: Int,
    val category: String,
    val lastTimeUsed: Long = System.currentTimeMillis()
)

data class LocationHistoryEntry(
    val id: String = "",
    val pairingId: String = "",
    val userId: String = "",
    val userName: String = "",
    val latitude: Double = 21.0285,
    val longitude: Double = 105.8542,
    val placeName: String = "Điểm dừng chân",
    val speed: Float = 0.0f,
    val batteryLevel: Int = 85,
    val isCharging: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val dateStr: String = "",
    val timeStr: String = "",
    val year: Int = 2026,
    val month: Int = 10,
    val day: Int = 12,
    val hour: Int = 0,
    val minute: Int = 0,
    val second: Int = 0
)

