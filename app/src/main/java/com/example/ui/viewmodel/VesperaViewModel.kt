package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.AppCategoryUsage
import com.example.data.model.EmotionTag
import com.example.data.model.ExtraTimeRequest
import com.example.data.model.FamilyMessage
import com.example.data.model.LocationData
import com.example.data.model.Pairing
import com.example.data.model.PairingRequest
import com.example.data.model.PairingStatus
import com.example.data.model.PrivacyRequest
import com.example.data.model.RequestStatus
import com.example.data.model.UsageSchedule
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.VesperaRepository
import com.example.data.model.DetailedAppUsage
import com.example.service.TelemetryAndUsageHelper
import com.example.service.VesperaTelemetryService
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "VesperaViewModel"

data class VesperaUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isGuestDemoMode: Boolean = false,
    val isIosPreviewMode: Boolean = false,
    val isBackgroundServiceRunning: Boolean = false,
    val focusSessionMinutesRemaining: Int = 0,
    val isFocusSessionActive: Boolean = false,
    val authUserId: String? = null,
    val userProfile: UserProfile? = null,
    val activePairing: Pairing? = null,
    val pendingRequests: List<PairingRequest> = emptyList(),
    val partnerLocation: LocationData? = null,
    val usageSchedule: UsageSchedule? = null,
    val usageCategories: List<AppCategoryUsage> = emptyList(),
    val detailedAppUsage: List<DetailedAppUsage> = emptyList(),
    val hasUsagePermission: Boolean = false,
    val extraTimeRequests: List<ExtraTimeRequest> = emptyList(),
    val privacyRequests: List<PrivacyRequest> = emptyList(),
    val messages: List<FamilyMessage> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedEmotion: EmotionTag? = null,
    val smartAdvice: String = "Chào bạn! Hãy đặt mục tiêu cân bằng 120 phút mỗi ngày giữa học tập và giải trí để bảo vệ sức khỏe.",
    val lastBumpCount: Int = 0,
    val weeklyUsage: List<Pair<String, Int>> = listOf(
        "T2" to 85, "T3" to 110, "T4" to 95, "T5" to 125, "T6" to 90, "T7" to 140, "CN" to 105
    )
)

class VesperaViewModel(
    private val repository: VesperaRepository,
    private val telemetryHelper: TelemetryAndUsageHelper
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(VesperaUiState())
    val uiState: StateFlow<VesperaUiState> = _uiState.asStateFlow()

    private var profileJob: Job? = null
    private var pairingJob: Job? = null
    private var requestsJob: Job? = null
    private var locationJob: Job? = null
    private var scheduleJob: Job? = null
    private var extraTimeJob: Job? = null
    private var privacyJob: Job? = null
    private var messagesJob: Job? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    isGuestDemoMode = false,
                    authUserId = user.uid,
                    isLoading = false
                )
                startObservingUserData(user.uid)
            } else if (!_uiState.value.isGuestDemoMode) {
                stopAllObservers()
                _uiState.value = VesperaUiState(isAuthenticated = false)
            }
        }
    }

    private fun stopAllObservers() {
        profileJob?.cancel()
        pairingJob?.cancel()
        requestsJob?.cancel()
        locationJob?.cancel()
        scheduleJob?.cancel()
        extraTimeJob?.cancel()
        privacyJob?.cancel()
        messagesJob?.cancel()
    }

    /**
     * Start Guest / Demo Mode without requiring login
     */
    fun enterGuestDemoMode(initialRole: UserRole = UserRole.PARENT) {
        stopAllObservers()

        val isParent = initialRole == UserRole.PARENT
        val currentUserId = if (isParent) "demo_parent_user" else "demo_child_user"
        val partnerId = if (isParent) "demo_child_user" else "demo_parent_user"

        val demoProfile = UserProfile(
            id = currentUserId,
            email = if (isParent) "parent.demo@vespera.family" else "student.demo@vespera.family",
            displayName = if (isParent) "Bố Minh" else "Bé An",
            role = initialRole,
            studentCode = "HS-8F29K",
            pairedWith = partnerId,
            activePairingId = "demo_pairing_01"
        )

        val demoPairing = Pairing(
            id = "demo_pairing_01",
            parentId = "demo_parent_user",
            childId = "demo_child_user",
            studentCode = "HS-8F29K",
            parentName = "Bố Minh",
            childName = "Bé An",
            status = PairingStatus.PAIRED,
            createdAt = System.currentTimeMillis() - 86400000L,
            updatedAt = System.currentTimeMillis()
        )

        val demoPartnerLocation = LocationData(
            userId = partnerId,
            pairingId = "demo_pairing_01",
            userName = if (isParent) "Bé An" else "Bố Minh",
            latitude = 21.0378, // Ho Tay / Chu Van An area
            longitude = 105.8325,
            accuracy = 8.5f,
            speed = 12.0f,
            batteryLevel = 84,
            isCharging = true,
            isPrivacyActive = false,
            updatedAt = System.currentTimeMillis()
        )

        val demoSchedule = UsageSchedule(
            id = "demo_sched",
            pairingId = "demo_pairing_01",
            title = "Lịch học & Sinh hoạt",
            dailyTargetMinutes = 120,
            usedMinutes = 65,
            allowedStartHour = 7,
            allowedStartMinute = 0,
            allowedEndHour = 21,
            allowedEndMinute = 30,
            daysOfWeek = "T2, T3, T4, T5, T6, T7, CN",
            isReminderOnly = true,
            isActive = true
        )

        val demoExtraTime = listOf(
            ExtraTimeRequest(
                id = "demo_extra_1",
                pairingId = "demo_pairing_01",
                childId = "demo_child_user",
                childName = "Bé An",
                minutesRequested = 30,
                reason = "Làm bài tập nhóm môn Tiếng Anh trên Duolingo",
                status = RequestStatus.PENDING,
                createdAt = System.currentTimeMillis() - 180000L
            )
        )

        val demoPrivacy = listOf(
            PrivacyRequest(
                id = "demo_privacy_1",
                pairingId = "demo_pairing_01",
                childId = "demo_child_user",
                childName = "Bé An",
                startHour = 14,
                startMinute = 0,
                endHour = 16,
                endMinute = 0,
                reason = "Gặp bạn bè thảo luận chuyên đề",
                status = RequestStatus.PENDING,
                createdAt = System.currentTimeMillis() - 60000L
            )
        )

        val demoMessages = listOf(
            FamilyMessage(
                id = "m1",
                pairingId = "demo_pairing_01",
                senderId = "demo_child_user",
                senderName = "Bé An",
                senderRole = UserRole.CHILD,
                text = "Con đã đến trường an toàn rồi ạ! Cảm ơn bố mẹ ❤️",
                emotion = EmotionTag.HAPPY.name,
                isStarred = true,
                createdAt = System.currentTimeMillis() - 3600000L
            ),
            FamilyMessage(
                id = "m2",
                pairingId = "demo_pairing_01",
                senderId = "demo_parent_user",
                senderName = "Bố Minh",
                senderRole = UserRole.PARENT,
                text = "Tốt lắm con yêu. Buổi chiều nhớ uống đủ nước nhé!",
                emotion = EmotionTag.NORMAL.name,
                isStarred = false,
                createdAt = System.currentTimeMillis() - 3000000L
            ),
            FamilyMessage(
                id = "m3",
                pairingId = "demo_pairing_01",
                senderId = "demo_child_user",
                senderName = "Bé An",
                senderRole = UserRole.CHILD,
                text = "Hôm nay bài kiểm tra môn Toán hơi nhiều bài khó 📚",
                emotion = EmotionTag.STUDY_STRESS.name,
                isStarred = true,
                createdAt = System.currentTimeMillis() - 600000L
            )
        )

        val defaultCategories = listOf(
            AppCategoryUsage("Học tập", 40, "study", 0.45f),
            AppCategoryUsage("Mạng xã hội", 15, "social", 0.25f),
            AppCategoryUsage("Trò chơi", 10, "game", 0.15f),
            AppCategoryUsage("Khác", 10, "other", 0.15f)
        )

        val defaultDetailedApps = listOf(
            DetailedAppUsage("com.google.android.youtube", "YouTube", 45, "Mạng xã hội"),
            DetailedAppUsage("com.zhiliaoapp.musically", "TikTok", 32, "Mạng xã hội"),
            DetailedAppUsage("com.duolingo", "Duolingo", 28, "Học tập"),
            DetailedAppUsage("com.zing.zalo", "Zalo", 20, "Mạng xã hội"),
            DetailedAppUsage("us.zoom.videomeetings", "Zoom Meetings", 15, "Học tập"),
            DetailedAppUsage("com.roblox.client", "Roblox", 12, "Trò chơi"),
            DetailedAppUsage("com.android.chrome", "Google Chrome", 10, "Tiện ích & Khác")
        )

        val realCats = if (telemetryHelper.hasUsageStatsPermission()) {
            telemetryHelper.getTodayUsageCategories()
        } else defaultCategories

        val realDetailed = if (telemetryHelper.hasUsageStatsPermission()) {
            telemetryHelper.getDetailedAppUsageList().ifEmpty { defaultDetailedApps }
        } else defaultDetailedApps

        _uiState.value = VesperaUiState(
            isLoading = false,
            isAuthenticated = true,
            isGuestDemoMode = true,
            authUserId = currentUserId,
            userProfile = demoProfile,
            activePairing = demoPairing,
            partnerLocation = demoPartnerLocation,
            usageSchedule = demoSchedule,
            usageCategories = realCats,
            detailedAppUsage = realDetailed,
            hasUsagePermission = telemetryHelper.hasUsageStatsPermission(),
            extraTimeRequests = demoExtraTime,
            privacyRequests = demoPrivacy,
            messages = demoMessages,
            selectedEmotion = if (isParent) EmotionTag.NORMAL else EmotionTag.STUDY_STRESS,
            smartAdvice = if (isParent)
                "🤖 Cố vấn Vespera: Bé An đang cảm thấy 'Áp lực học tập' 📚. Gợi ý: Gửi lời khen ngợi hoặc cho phép con giải lao 15 phút uống sữa ấm nhé!"
            else
                "💡 Lời khuyên Vespera: Con đã dùng 40 phút học tập rất chăm chỉ! Hãy nghỉ mắt 5 phút trước khi tiếp tục."
        )
    }

    /**
     * Switch role inside Demo Mode to test both sides instantly
     */
    fun switchDemoRole(targetRole: UserRole) {
        if (!_uiState.value.isGuestDemoMode) return
        enterGuestDemoMode(targetRole)
    }

    /**
     * Exit demo mode back to SignIn
     */
    fun exitDemoMode() {
        stopAllObservers()
        _uiState.value = VesperaUiState(isAuthenticated = false, isGuestDemoMode = false)
    }

    /**
     * Toggle iOS UI Preview Mode (Apple Dynamic Island & Bento Glass)
     */
    fun toggleIosPreviewMode() {
        val next = !_uiState.value.isIosPreviewMode
        _uiState.value = _uiState.value.copy(
            isIosPreviewMode = next,
            successMessage = if (next)
                "🍏 Đã bật Chế độ xem trước Giao diện iOS (Apple Dynamic Island & Bento Glass)!"
            else
                "Đã chuyển về giao diện Android chuẩn."
        )
    }

    /**
     * Start Pomodoro Focus Mode Study Session
     */
    fun startFocusSession(minutes: Int = 25) {
        _uiState.value = _uiState.value.copy(
            isFocusSessionActive = true,
            focusSessionMinutesRemaining = minutes,
            successMessage = "🎯 Đã kích hoạt Chế độ Học tập Tập trung ($minutes phút)! Vespera sẽ phát cảnh báo nếu mở mạng xã hội."
        )
    }

    fun stopFocusSession() {
        _uiState.value = _uiState.value.copy(
            isFocusSessionActive = false,
            focusSessionMinutesRemaining = 0,
            successMessage = "Đã kết thúc phiên học tập tập trung."
        )
    }

    /**
     * Interactive Zenly "Bump / Chạm tay" feature with tactile vibration
     */
    fun sendZenlyBump(context: Context? = null) {
        val currentCount = _uiState.value.lastBumpCount + 1
        context?.let { ctx ->
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val mgr = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    mgr?.defaultVibrator?.vibrate(
                        android.os.VibrationEffect.createWaveform(longArrayOf(0, 100, 70, 150), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator?.vibrate(
                            android.os.VibrationEffect.createWaveform(longArrayOf(0, 100, 70, 150), -1)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(200)
                    }
                }
            } catch (_: Exception) {}
        }
        _uiState.value = _uiState.value.copy(
            lastBumpCount = currentCount,
            successMessage = "⚡ BUMP! Bạn vừa chạm tay tương tác Zenly. Tín hiệu rung haptic đã kích hoạt! 👋❤️"
        )
    }

    private fun startObservingUserData(userId: String) {
        stopAllObservers()

        profileJob = viewModelScope.launch {
            repository.observeUserProfile(userId).collect { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(userProfile = profile)
                    if (profile.activePairingId.isNotEmpty()) {
                        observeActivePairing(profile.activePairingId, profile)
                    }
                    if (profile.role == UserRole.CHILD) {
                        observeIncomingRequests(profile.id)
                    }
                }
            }
        }

        refreshUsageStats()
        broadcastSelfTelemetry()
    }

    private fun observeActivePairing(pairingId: String, currentProfile: UserProfile) {
        pairingJob?.cancel()
        pairingJob = viewModelScope.launch {
            repository.observePairing(pairingId).collect { pairing ->
                _uiState.value = _uiState.value.copy(activePairing = pairing)
                if (pairing != null && pairing.status == PairingStatus.PAIRED) {
                    val partnerId = if (currentProfile.role == UserRole.PARENT) pairing.childId else pairing.parentId
                    observePartnerLocation(partnerId)
                    observeSchedule(pairingId)
                    observeExtraTime(pairingId)
                    observePrivacy(pairingId)
                    observeChatMessages(pairingId)
                }
            }
        }
    }

    private fun observeIncomingRequests(childId: String) {
        requestsJob?.cancel()
        requestsJob = viewModelScope.launch {
            repository.observePendingRequestsForChild(childId).collect { list ->
                _uiState.value = _uiState.value.copy(pendingRequests = list)
            }
        }
    }

    private fun observePartnerLocation(partnerId: String) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            repository.observeLocation(partnerId).collect { loc ->
                _uiState.value = _uiState.value.copy(partnerLocation = loc)
            }
        }
    }

    private fun observeSchedule(pairingId: String) {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            repository.observeSchedule(pairingId).collect { sched ->
                _uiState.value = _uiState.value.copy(usageSchedule = sched)
            }
        }
    }

    private fun observeExtraTime(pairingId: String) {
        extraTimeJob?.cancel()
        extraTimeJob = viewModelScope.launch {
            repository.observeExtraTimeRequests(pairingId).collect { reqs ->
                _uiState.value = _uiState.value.copy(extraTimeRequests = reqs)
            }
        }
    }

    private fun observePrivacy(pairingId: String) {
        privacyJob?.cancel()
        privacyJob = viewModelScope.launch {
            repository.observePrivacyRequests(pairingId).collect { reqs ->
                _uiState.value = _uiState.value.copy(privacyRequests = reqs)
            }
        }
    }

    private fun observeChatMessages(pairingId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.observeMessages(pairingId).collect { msgs ->
                _uiState.value = _uiState.value.copy(messages = msgs)
            }
        }
    }

    /**
     * Sign in with Google via Credential Manager
     */
    fun signInWithGoogle(context: Context) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                val serverClientId = context.getString(R.string.default_web_client_id)
                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context as Activity
                )

                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val firebaseCred = GoogleAuthProvider.getCredential(credential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCred).await()
                    val user = authResult.user

                    if (user != null) {
                        val existing = repository.getUserProfile(user.uid)
                        if (existing == null) {
                            val newProfile = UserProfile(
                                id = user.uid,
                                email = user.email.orEmpty(),
                                displayName = user.displayName.orEmpty().ifEmpty { "Người dùng" },
                                photoUrl = user.photoUrl?.toString().orEmpty(),
                                role = UserRole.PARENT
                            )
                            repository.saveUserProfile(newProfile)
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(isLoading = false, isGuestDemoMode = false)
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-in was cancelled", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-in failed", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Đăng nhập thất bại: ${e.localizedMessage ?: "Vui lòng thử lại"}"
                )
            }
        }
    }

    fun selectRole(role: UserRole) {
        val current = _uiState.value.userProfile ?: return
        if (_uiState.value.isGuestDemoMode) {
            switchDemoRole(role)
            return
        }
        viewModelScope.launch {
            try {
                val studentCode = if (role == UserRole.CHILD && current.studentCode.isEmpty()) {
                    repository.generateStudentCode()
                } else {
                    current.studentCode
                }
                val updated = current.copy(role = role, studentCode = studentCode)
                repository.saveUserProfile(updated)
                _uiState.value = _uiState.value.copy(userProfile = updated)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun sendPairingRequest(studentCode: String) {
        val parent = _uiState.value.userProfile ?: return
        if (studentCode.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập mã học sinh")
            return
        }

        if (_uiState.value.isGuestDemoMode) {
            _uiState.value = _uiState.value.copy(
                successMessage = "Demo: Đã gửi lời mời ghép nối đến mã $studentCode thành công!"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                repository.createPairingRequest(studentCode.trim().uppercase(), parent)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Đã gửi yêu cầu ghép nối đến mã $studentCode. Vui lòng chờ học sinh xác nhận."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Không thể gửi yêu cầu ghép nối"
                )
            }
        }
    }

    fun acceptPairing(request: PairingRequest) {
        val child = _uiState.value.userProfile ?: return
        if (_uiState.value.isGuestDemoMode) {
            _uiState.value = _uiState.value.copy(
                successMessage = "Demo: Đã chấp nhận ghép nối!"
            )
            return
        }
        viewModelScope.launch {
            try {
                repository.acceptPairingRequest(request, child)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Ghép nối thành công với ${request.parentName}!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun rejectPairing(requestId: String) {
        if (_uiState.value.isGuestDemoMode) {
            _uiState.value = _uiState.value.copy(
                pendingRequests = _uiState.value.pendingRequests.filterNot { it.id == requestId }
            )
            return
        }
        viewModelScope.launch {
            try {
                repository.rejectPairingRequest(requestId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun unpairConnection() {
        val pairing = _uiState.value.activePairing ?: return
        if (_uiState.value.isGuestDemoMode) {
            _uiState.value = _uiState.value.copy(
                activePairing = null,
                partnerLocation = null,
                successMessage = "Demo: Đã hủy liên kết thiết bị thành công."
            )
            return
        }
        viewModelScope.launch {
            try {
                repository.unpair(pairing.id, pairing.parentId, pairing.childId)
                _uiState.value = _uiState.value.copy(
                    activePairing = null,
                    partnerLocation = null,
                    successMessage = "Đã hủy liên kết gia đình thành công."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun refreshUsageStats() {
        val hasPerm = telemetryHelper.hasUsageStatsPermission()
        val categories = telemetryHelper.getTodayUsageCategories()
        val detailedList = telemetryHelper.getDetailedAppUsageList()

        val fallbackDetailed = if (_uiState.value.detailedAppUsage.isNotEmpty()) {
            _uiState.value.detailedAppUsage
        } else {
            listOf(
                DetailedAppUsage("com.google.android.youtube", "YouTube", 45, "Mạng xã hội"),
                DetailedAppUsage("com.zhiliaoapp.musically", "TikTok", 32, "Mạng xã hội"),
                DetailedAppUsage("com.duolingo", "Duolingo", 28, "Học tập"),
                DetailedAppUsage("com.zing.zalo", "Zalo", 20, "Mạng xã hội"),
                DetailedAppUsage("us.zoom.videomeetings", "Zoom Meetings", 15, "Học tập"),
                DetailedAppUsage("com.roblox.client", "Roblox", 12, "Trò chơi"),
                DetailedAppUsage("com.android.chrome", "Google Chrome", 10, "Tiện ích & Khác")
            )
        }

        _uiState.value = _uiState.value.copy(
            hasUsagePermission = hasPerm,
            usageCategories = if (categories.isNotEmpty()) categories else _uiState.value.usageCategories,
            detailedAppUsage = if (detailedList.isNotEmpty()) detailedList else fallbackDetailed
        )
    }

    fun toggleBackgroundService(context: Context) {
        val newState = !_uiState.value.isBackgroundServiceRunning
        if (newState) {
            VesperaTelemetryService.start(context)
            _uiState.value = _uiState.value.copy(
                isBackgroundServiceRunning = true,
                successMessage = "🟢 Đã bật Dịch vụ Chạy Ngầm! Vespera đang liên tục cập nhật GPS và giám sát thời gian sử dụng."
            )
        } else {
            VesperaTelemetryService.stop(context)
            _uiState.value = _uiState.value.copy(
                isBackgroundServiceRunning = false,
                successMessage = "Đã dừng dịch vụ chạy ngầm."
            )
        }
    }

    fun broadcastSelfTelemetry() {
        val user = _uiState.value.userProfile ?: return
        val pairing = _uiState.value.activePairing
        val (batteryLevel, isCharging) = telemetryHelper.getBatteryInfo()

        if (_uiState.value.isGuestDemoMode) {
            _uiState.value = _uiState.value.copy(
                partnerLocation = _uiState.value.partnerLocation?.copy(
                    updatedAt = System.currentTimeMillis()
                ),
                successMessage = "Đã cập nhật tọa độ vệ tinh GPS mới nhất."
            )
            return
        }

        telemetryHelper.fetchCurrentLocation { loc ->
            val lat = loc?.latitude ?: 21.0285
            val lng = loc?.longitude ?: 105.8542
            val acc = loc?.accuracy ?: 15.0f
            val spd = loc?.speed ?: 0.0f
            val detailed = telemetryHelper.getDetailedAppUsageList()
            val topApp = detailed.firstOrNull()?.appName ?: "Đang chờ dữ liệu"
            val topMins = detailed.firstOrNull()?.minutesUsed ?: 0
            val totalMins = detailed.sumOf { it.minutesUsed }

            val safeZone = when {
                spd > 8f -> "Đang di chuyển (${(spd * 3.6f).toInt()} km/h)"
                Math.abs(lat - 21.0378) < 0.005 && Math.abs(lng - 105.8325) < 0.005 -> "Trường học"
                else -> "Nhà riêng / Điểm an toàn"
            }

            val locationData = LocationData(
                userId = user.id,
                pairingId = pairing?.id.orEmpty(),
                userName = user.displayName,
                latitude = lat,
                longitude = lng,
                accuracy = acc,
                speed = spd,
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                isPrivacyActive = false,
                currentSafeZone = safeZone,
                topActiveApp = topApp,
                topActiveAppMinutes = topMins,
                totalUsedMinutes = totalMins,
                updatedAt = System.currentTimeMillis()
            )

            viewModelScope.launch {
                try {
                    repository.updateLocation(locationData)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update location", e)
                }
            }
        }
    }

    fun requestExtraTime(minutes: Int, reason: String) {
        val pairing = _uiState.value.activePairing ?: return
        val child = _uiState.value.userProfile ?: return

        if (_uiState.value.isGuestDemoMode) {
            val newReq = ExtraTimeRequest(
                id = "demo_extra_${System.currentTimeMillis()}",
                pairingId = pairing.id,
                childId = child.id,
                childName = child.displayName,
                minutesRequested = minutes,
                reason = reason.ifEmpty { "Làm bài tập thêm" },
                status = RequestStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
            _uiState.value = _uiState.value.copy(
                extraTimeRequests = listOf(newReq) + _uiState.value.extraTimeRequests,
                successMessage = "Demo: Đã gửi yêu cầu xin thêm $minutes phút đến phụ huynh!"
            )
            return
        }

        viewModelScope.launch {
            try {
                val req = ExtraTimeRequest(
                    pairingId = pairing.id,
                    childId = child.id,
                    childName = child.displayName,
                    minutesRequested = minutes,
                    reason = reason,
                    status = RequestStatus.PENDING
                )
                repository.requestExtraTime(req)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Đã gửi yêu cầu xin thêm $minutes phút đến phụ huynh!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun respondToExtraTime(requestId: String, approved: Boolean, minutesToAdd: Int) {
        val pairing = _uiState.value.activePairing ?: return

        if (_uiState.value.isGuestDemoMode) {
            val updatedReqs = _uiState.value.extraTimeRequests.map {
                if (it.id == requestId) it.copy(status = if (approved) RequestStatus.APPROVED else RequestStatus.REJECTED) else it
            }
            val currentSched = _uiState.value.usageSchedule
            val updatedSched = if (approved && currentSched != null) {
                currentSched.copy(dailyTargetMinutes = currentSched.dailyTargetMinutes + minutesToAdd)
            } else currentSched

            _uiState.value = _uiState.value.copy(
                extraTimeRequests = updatedReqs,
                usageSchedule = updatedSched,
                successMessage = if (approved) "Demo: Đã duyệt cộng +$minutesToAdd phút!" else "Demo: Đã từ chối yêu cầu."
            )
            return
        }

        viewModelScope.launch {
            try {
                repository.respondToExtraTime(pairing.id, requestId, approved, minutesToAdd)
                _uiState.value = _uiState.value.copy(
                    successMessage = if (approved) "Đã duyệt cộng $minutesToAdd phút sử dụng." else "Đã từ chối yêu cầu."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun requestPrivacyHours(startHour: Int, endHour: Int, reason: String) {
        val pairing = _uiState.value.activePairing ?: return
        val child = _uiState.value.userProfile ?: return

        if (_uiState.value.isGuestDemoMode) {
            val newReq = PrivacyRequest(
                id = "demo_privacy_${System.currentTimeMillis()}",
                pairingId = pairing.id,
                childId = child.id,
                childName = child.displayName,
                startHour = startHour,
                startMinute = 0,
                endHour = endHour,
                endMinute = 0,
                reason = reason.ifEmpty { "Học nhóm riêng tư" },
                status = RequestStatus.PENDING,
                createdAt = System.currentTimeMillis()
            )
            _uiState.value = _uiState.value.copy(
                privacyRequests = listOf(newReq) + _uiState.value.privacyRequests,
                successMessage = "Demo: Đã gửi yêu cầu khung giờ riêng tư ($startHour:00 - $endHour:00)!"
            )
            return
        }

        viewModelScope.launch {
            try {
                val req = PrivacyRequest(
                    pairingId = pairing.id,
                    childId = child.id,
                    childName = child.displayName,
                    startHour = startHour,
                    startMinute = 0,
                    endHour = endHour,
                    endMinute = 0,
                    reason = reason,
                    status = RequestStatus.PENDING
                )
                repository.requestPrivacyHours(req)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Đã gửi yêu cầu khung giờ riêng tư ($startHour:00 - $endHour:00)!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun respondToPrivacyRequest(requestId: String, childId: String, approved: Boolean) {
        val pairing = _uiState.value.activePairing ?: return

        if (_uiState.value.isGuestDemoMode) {
            val updatedReqs = _uiState.value.privacyRequests.map {
                if (it.id == requestId) it.copy(status = if (approved) RequestStatus.APPROVED else RequestStatus.REJECTED) else it
            }
            val currentLoc = _uiState.value.partnerLocation
            val updatedLoc = if (approved && currentLoc != null) currentLoc.copy(isPrivacyActive = true) else currentLoc

            _uiState.value = _uiState.value.copy(
                privacyRequests = updatedReqs,
                partnerLocation = updatedLoc,
                successMessage = if (approved) "Demo: Đã duyệt khung giờ riêng tư 🛡️" else "Demo: Đã từ chối riêng tư."
            )
            return
        }

        viewModelScope.launch {
            try {
                repository.respondToPrivacyRequest(pairing.id, requestId, childId, approved)
                _uiState.value = _uiState.value.copy(
                    successMessage = if (approved) "Đã duyệt khung giờ riêng tư cho học sinh." else "Đã từ chối yêu cầu riêng tư."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun setSelectedEmotion(emotion: EmotionTag?) {
        _uiState.value = _uiState.value.copy(selectedEmotion = emotion)
        updateSmartAdvice(emotion)
    }

    private fun updateSmartAdvice(emotion: EmotionTag?) {
        val advice = when (emotion) {
            EmotionTag.STUDY_STRESS -> "🤖 Cố vấn Vespera: Học sinh đang chịu áp lực học tập 📚. Lời khuyên: Phụ huynh nên gửi tin nhắn động viên và nhắc con nghỉ mắt 15 phút."
            EmotionTag.NEED_SUPPORT -> "🆘 Cảnh báo Vespera: Học sinh đang cần hỗ trợ khẩn cấp! Hãy gọi điện hoặc liên hệ ngay với con."
            EmotionTag.HAPPY -> "🌟 Vui vẻ: Tâm trạng con đang rất tích cực! Duy trì sự khích lệ và khen ngợi con nhé."
            EmotionTag.NORMAL -> "💡 Cố vấn Vespera: Buổi chiều nay tiến độ học tập ổn định. Cân nhắc dành 30 phút buổi tối cho hoạt động thể thao gia đình."
            null -> _uiState.value.smartAdvice
        }
        _uiState.value = _uiState.value.copy(smartAdvice = advice)
    }

    fun sendMessage(text: String) {
        val pairing = _uiState.value.activePairing ?: return
        val profile = _uiState.value.userProfile ?: return
        val emotion = _uiState.value.selectedEmotion

        if (text.isBlank() && emotion == null) return

        if (_uiState.value.isGuestDemoMode) {
            val newMsg = FamilyMessage(
                id = "demo_msg_${System.currentTimeMillis()}",
                pairingId = pairing.id,
                senderId = profile.id,
                senderName = profile.displayName,
                senderRole = profile.role,
                text = text.trim(),
                emotion = emotion?.name,
                isStarred = false,
                createdAt = System.currentTimeMillis()
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + newMsg,
                selectedEmotion = null
            )
            return
        }

        viewModelScope.launch {
            try {
                val msg = FamilyMessage(
                    pairingId = pairing.id,
                    senderId = profile.id,
                    senderName = profile.displayName,
                    senderRole = profile.role,
                    text = text.trim(),
                    emotion = emotion?.name,
                    isStarred = false,
                    createdAt = System.currentTimeMillis()
                )
                repository.sendMessage(msg)
                _uiState.value = _uiState.value.copy(selectedEmotion = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun toggleMessageStar(messageId: String, isStarred: Boolean) {
        val pairing = _uiState.value.activePairing ?: return

        if (_uiState.value.isGuestDemoMode) {
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages.map {
                    if (it.id == messageId) it.copy(isStarred = isStarred) else it
                }
            )
            return
        }

        viewModelScope.launch {
            try {
                repository.toggleMessageStar(pairing.id, messageId, isStarred)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun signOut() {
        if (_uiState.value.isGuestDemoMode) {
            exitDemoMode()
            return
        }
        auth.signOut()
    }
}
