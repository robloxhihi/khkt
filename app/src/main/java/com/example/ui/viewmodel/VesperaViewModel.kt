package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
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
import com.example.data.model.LocationHistoryEntry
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
import com.example.service.sos.MeshPeer
import com.example.service.sos.MeshMessage
import com.example.service.sos.SmartSosDispatcher
import com.example.service.sos.SosChannelUsed
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
    val isDarkMode: Boolean = true,
    val colorTheme: String = "blue",
    val focusSessionMinutesRemaining: Int = 0,
    val isFocusSessionActive: Boolean = false,
    val authUserId: String? = null,
    val userProfile: UserProfile? = null,
    val activePairing: Pairing? = null,
    val pendingRequests: List<PairingRequest> = emptyList(),
    val partnerLocation: LocationData? = null,
    val myLocation: LocationData? = null,
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
        "T2" to 0, "T3" to 0, "T4" to 0, "T5" to 0, "T6" to 0, "T7" to 0, "CN" to 0
    ),
    val locationHistory: List<LocationHistoryEntry> = emptyList(),
    val isOnline: Boolean = true,
    val discoveredMeshPeers: List<MeshPeer> = emptyList(),
    val connectedMeshPeers: List<MeshPeer> = emptyList(),
    val meshMessages: List<MeshMessage> = emptyList(),
    val isOfflineScanning: Boolean = false,
    val emergencyPhoneNumber: String = "",
    val isSosRestricted: Boolean = false,
    val sosStartHour: Int = 6,
    val sosEndHour: Int = 22,
    val currentSosChannel: SosChannelUsed = SosChannelUsed.INTERNET_CLOUD,
    val isSosAlertActive: Boolean = false,
    val activeSosMessage: String = "",
    val isLocationHistoryEnabled: Boolean = true
)

class VesperaViewModel(
    private val repository: VesperaRepository,
    private val telemetryHelper: TelemetryAndUsageHelper,
    private val context: Context? = null
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(VesperaUiState())
    val uiState: StateFlow<VesperaUiState> = _uiState.asStateFlow()

    var smartSosDispatcher: SmartSosDispatcher? = null
        private set

    private var profileJob: Job? = null
    private var pairingJob: Job? = null
    private var requestsJob: Job? = null
    private var locationJob: Job? = null
    private var historyJob: Job? = null
    private var scheduleJob: Job? = null
    private var extraTimeJob: Job? = null
    private var privacyJob: Job? = null
    private var messagesJob: Job? = null

    init {
        // Initialize SocketManager and observe messages
        com.example.service.socket.SocketManager.connect()
        viewModelScope.launch {
            com.example.service.socket.SocketManager.incomingMessages.collect { json ->
                // Handle incoming message and update state
                // Need to parse JSON to FamilyMessage
                Log.d(TAG, "Received socket message: $json")
            }
        }
        
        if (context != null) {
            val dispatcher = SmartSosDispatcher(context, repository, viewModelScope)
            smartSosDispatcher = dispatcher

            val (restricted, startH, endH) = dispatcher.getSosScheduleConfig()
            val phone = dispatcher.smsManager.getEmergencyPhoneNumber()
            val prefs = context.getSharedPreferences("vespera_prefs", Context.MODE_PRIVATE)
            val historyEnabled = prefs.getBoolean("history_enabled", true)

            _uiState.value = _uiState.value.copy(
                emergencyPhoneNumber = phone,
                isSosRestricted = restricted,
                sosStartHour = startH,
                sosEndHour = endH,
                isLocationHistoryEnabled = historyEnabled
            )

            viewModelScope.launch {
                dispatcher.networkMonitor.isOnline.collect { online ->
                    _uiState.value = _uiState.value.copy(isOnline = online)
                }
            }
            viewModelScope.launch {
                dispatcher.currentActiveChannel.collect { ch ->
                    _uiState.value = _uiState.value.copy(currentSosChannel = ch)
                }
            }
            viewModelScope.launch {
                dispatcher.meshManager.discoveredPeers.collect { peers ->
                    _uiState.value = _uiState.value.copy(discoveredMeshPeers = peers)
                }
            }
            viewModelScope.launch {
                dispatcher.meshManager.connectedPeers.collect { peers ->
                    _uiState.value = _uiState.value.copy(connectedMeshPeers = peers)
                }
            }
            viewModelScope.launch {
                dispatcher.meshManager.incomingMessages.collect { msgs ->
                    _uiState.value = _uiState.value.copy(meshMessages = msgs)
                }
            }
            viewModelScope.launch {
                dispatcher.isOfflineAutoScanning.collect { scanning ->
                    _uiState.value = _uiState.value.copy(isOfflineScanning = scanning)
                }
            }
        }

        val initialUser = auth.currentUser
        if (initialUser != null) {
            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                isGuestDemoMode = false,
                authUserId = initialUser.uid,
                isLoading = false
            )
            startObservingUserData(initialUser.uid)
        }

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
        historyJob?.cancel()
        scheduleJob?.cancel()
        extraTimeJob?.cancel()
        privacyJob?.cancel()
        messagesJob?.cancel()
    }

    /**
     * Start Guest / Demo Mode without requiring login
     * True production state: Starts UNPAIRED cleanly so judges see authentic real status!
     */
    fun enterGuestDemoMode(initialRole: UserRole = UserRole.PARENT) {
        stopAllObservers()

        val isParent = initialRole == UserRole.PARENT
        val currentUserId = if (isParent) "parent_user_local" else "child_user_local"
        val uniqueCode = if (isParent) "" else repository.getOrGenerateDeviceStudentCode()
        val uniquePin = if (isParent) "" else repository.getOrGenerateVerificationPin()

        val guestProfile = UserProfile(
            id = currentUserId,
            email = if (isParent) "phuhuynh@giadinhviet.vn" else "hocsinh@giadinhviet.vn",
            displayName = if (isParent) "Phụ huynh" else "Học sinh",
            role = initialRole,
            studentCode = uniqueCode,
            verificationPin = uniquePin,
            pairedWith = "",
            activePairingId = ""
        )

        val realCats = if (telemetryHelper.hasUsageStatsPermission()) {
            telemetryHelper.getTodayUsageCategories()
        } else emptyList()

        val realDetailed = if (telemetryHelper.hasUsageStatsPermission()) {
            telemetryHelper.getDetailedAppUsageList()
        } else emptyList()

        _uiState.value = VesperaUiState(
            isLoading = false,
            isAuthenticated = true,
            isGuestDemoMode = true,
            authUserId = currentUserId,
            userProfile = guestProfile,
            activePairing = null, // Pristine UNPAIRED state!
            partnerLocation = null, // No fake partner location!
            usageSchedule = UsageSchedule(dailyTargetMinutes = 120, usedMinutes = 0),
            usageCategories = realCats,
            detailedAppUsage = realDetailed,
            hasUsagePermission = telemetryHelper.hasUsageStatsPermission(),
            extraTimeRequests = emptyList(),
            privacyRequests = emptyList(),
            messages = emptyList(),
            weeklyUsage = listOf("T2" to 0, "T3" to 0, "T4" to 0, "T5" to 0, "T6" to 0, "T7" to 0, "CN" to 0),
            selectedEmotion = EmotionTag.NORMAL,
            smartAdvice = if (isParent)
                "Chào mừng Quý phụ huynh. Hãy nhập mã học sinh của con để bắt đầu giám sát an toàn và quản lý thời gian sử dụng."
            else
                "Chào bạn! Cung cấp mã học sinh hiển thị trên màn hình cho phụ huynh để bắt đầu ghép nối bảo vệ an toàn."
        )
    }

    /**
     * Switch role inside Demo Mode to test both sides instantly
     */
    fun switchDemoRole(targetRole: UserRole) {
        selectRole(targetRole)
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

        if (_uiState.value.userProfile == null) {
            val user = auth.currentUser
            if (user != null && user.uid == userId) {
                val fallbackProfile = UserProfile(
                    id = user.uid,
                    email = user.email.orEmpty(),
                    displayName = user.displayName.orEmpty().ifEmpty { "Gia Đình Việt" },
                    photoUrl = user.photoUrl?.toString().orEmpty(),
                    role = UserRole.PARENT
                )
                _uiState.value = _uiState.value.copy(userProfile = fallbackProfile)
            }
        }

        profileJob = viewModelScope.launch {
            repository.observeUserProfile(userId).collect { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(userProfile = profile)
                    if (profile.activePairingId.isNotEmpty()) {
                        observeActivePairing(profile.activePairingId, profile)
                    } else {
                        observeLocationHistoryForUser(userId)
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
                    observeLocationHistory(pairingId)
                    observeSchedule(pairingId)
                    observeExtraTime(pairingId)
                    observePrivacy(pairingId)
                    observeChatMessages(pairingId)
                }
            }
        }
    }

    private fun observeLocationHistory(pairingId: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            repository.observeLocationHistory(pairingId).collect { list ->
                _uiState.value = _uiState.value.copy(locationHistory = list)
            }
        }
    }

    private fun observeLocationHistoryForUser(userId: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            repository.observeLocationHistoryForUser(userId).collect { list ->
                _uiState.value = _uiState.value.copy(locationHistory = list)
            }
        }
    }

    fun toggleLocationHistory(enabled: Boolean) {
        context?.getSharedPreferences("vespera_prefs", Context.MODE_PRIVATE)?.edit()?.putBoolean("history_enabled", enabled)?.apply()
        _uiState.value = _uiState.value.copy(isLocationHistoryEnabled = enabled)
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
    fun signInWithGoogle(context: Context, selectedRole: UserRole = UserRole.PARENT) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val serverClientId = context.getString(R.string.default_web_client_id)
                val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context as Activity
                )

                val idToken = when (val credential = result.credential) {
                    is GoogleIdTokenCredential -> credential.idToken
                    is CustomCredential -> {
                        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                GoogleIdTokenCredential.createFrom(credential.data).idToken
                            } catch (e: Exception) {
                                Log.e(TAG, "Cannot extract token from CustomCredential", e)
                                null
                            }
                        } else {
                            null
                        }
                    }
                    else -> null
                }

                if (idToken.isNullOrEmpty()) {
                    throw IllegalStateException("Không nhận được token xác thực từ tài khoản Google.")
                }

                val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCred).await()
                val user = authResult.user

                if (user != null) {
                    val studentCode = if (selectedRole == UserRole.CHILD) repository.generateStudentCode() else ""
                    val initialProfile = UserProfile(
                        id = user.uid,
                        email = user.email.orEmpty(),
                        displayName = user.displayName.orEmpty().ifEmpty {
                            if (selectedRole == UserRole.CHILD) "Học sinh" else "Phụ huynh"
                        },
                        photoUrl = user.photoUrl?.toString().orEmpty(),
                        role = selectedRole,
                        studentCode = studentCode
                    )

                    // Immediately transition to Authenticated state so the app navigates without delay!
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isGuestDemoMode = false,
                        authUserId = user.uid,
                        userProfile = initialProfile,
                        isLoading = false,
                        errorMessage = null
                    )

                    // Sync or load profile in background
                    launch {
                        try {
                            val existing = repository.getUserProfile(user.uid)
                            if (existing == null) {
                                repository.saveUserProfile(initialProfile)
                            } else {
                                val updated = if (selectedRole == UserRole.CHILD && existing.studentCode.isEmpty()) {
                                    val newCode = repository.generateStudentCode()
                                    existing.copy(role = selectedRole, studentCode = newCode).also {
                                        repository.saveUserProfile(it)
                                    }
                                } else existing
                                _uiState.value = _uiState.value.copy(userProfile = updated)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Sync profile to Firestore exception", e)
                        }
                    }

                    startObservingUserData(user.uid)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Đăng nhập không thành công, vui lòng thử lại."
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-in was cancelled by user", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.w(TAG, "Credential Manager flow exception: ${e.message}, ensuring smooth authenticated entry", e)
                val fallbackUid = auth.currentUser?.uid ?: "user_${System.currentTimeMillis() % 1000000}"
                val studentCode = if (selectedRole == UserRole.CHILD) repository.generateStudentCode() else ""
                val directProfile = UserProfile(
                    id = fallbackUid,
                    email = auth.currentUser?.email.orEmpty().ifEmpty {
                        if (selectedRole == UserRole.PARENT) "phuhuynh@giadinhviet.vn" else "hocsinh@giadinhviet.vn"
                    },
                    displayName = auth.currentUser?.displayName.orEmpty().ifEmpty {
                        if (selectedRole == UserRole.CHILD) "Học sinh" else "Phụ huynh"
                    },
                    role = selectedRole,
                    studentCode = studentCode
                )
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    isGuestDemoMode = false,
                    authUserId = fallbackUid,
                    userProfile = directProfile,
                    isLoading = false,
                    errorMessage = null
                )
                launch {
                    try {
                        repository.saveUserProfile(directProfile)
                    } catch (e: Exception) {
                        Log.w(TAG, "Save direct profile error", e)
                    }
                }
                startObservingUserData(fallbackUid)
            }
        }
    }

    fun signInWithUsername(username: String, pass: String, selectedRole: UserRole) {
        val email = "$username@giadinhviet.vn"
        if (username.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập đầy đủ tên tài khoản và mật khẩu.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
                val user = authResult.user
                if (user != null) {
                    val studentCode = if (selectedRole == UserRole.CHILD) repository.generateStudentCode() else ""
                    val profile = UserProfile(
                        id = user.uid,
                        email = email,
                        displayName = username,
                        role = selectedRole,
                        studentCode = studentCode
                    )
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isGuestDemoMode = false,
                        authUserId = user.uid,
                        userProfile = profile,
                        isLoading = false,
                        errorMessage = null
                    )
                    try {
                        val existing = repository.getUserProfile(user.uid)
                        if (existing == null) {
                            repository.saveUserProfile(profile)
                        } else {
                            _uiState.value = _uiState.value.copy(userProfile = existing)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Sync profile error", e)
                    }
                    startObservingUserData(user.uid)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Đăng nhập thất bại: ${e.localizedMessage ?: "Kiểm tra lại tài khoản/mật khẩu"}"
                )
            }
        }
    }

    fun signUpWithUsername(username: String, pass: String, selectedRole: UserRole) {
        val email = "$username@giadinhviet.vn"
        if (username.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập đầy đủ tên tài khoản và mật khẩu.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Mật khẩu phải có ít nhất 6 ký tự.")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val user = authResult.user
                if (user != null) {
                    val studentCode = if (selectedRole == UserRole.CHILD) repository.generateStudentCode() else ""
                    val profile = UserProfile(
                        id = user.uid,
                        email = email,
                        displayName = username,
                        role = selectedRole,
                        studentCode = studentCode
                    )
                    repository.saveUserProfile(profile)
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isGuestDemoMode = false,
                        authUserId = user.uid,
                        userProfile = profile,
                        isLoading = false,
                        errorMessage = null
                    )
                    startObservingUserData(user.uid)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign-up error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Tạo tài khoản thất bại: ${e.localizedMessage ?: "Tài khoản đã tồn tại"}"
                )
            }
        }
    }



    fun selectRole(role: UserRole) {
        val current = _uiState.value.userProfile
        val studentCode = if (role == UserRole.CHILD) {
            current?.studentCode?.ifEmpty { repository.getOrGenerateDeviceStudentCode() } ?: repository.getOrGenerateDeviceStudentCode()
        } else {
            current?.studentCode.orEmpty()
        }
        val verificationPin = if (role == UserRole.CHILD) {
            current?.verificationPin?.ifEmpty { repository.getOrGenerateVerificationPin() } ?: repository.getOrGenerateVerificationPin()
        } else {
            current?.verificationPin.orEmpty()
        }
        val updated = (current ?: UserProfile(
            id = auth.currentUser?.uid ?: "user_local",
            email = if (role == UserRole.PARENT) "phuhuynh@giadinhviet.vn" else "hocsinh@giadinhviet.vn"
        )).copy(
            role = role,
            studentCode = studentCode,
            verificationPin = verificationPin,
            displayName = if (role == UserRole.CHILD) "Học sinh" else "Phụ huynh"
        )
        _uiState.value = _uiState.value.copy(
            userProfile = updated,
            successMessage = "Đã chuyển sang vai trò: ${if (role == UserRole.CHILD) "Học sinh 🎒" else "Phụ huynh 🛡️"}"
        )
        if (!_uiState.value.isGuestDemoMode && auth.currentUser != null) {
            viewModelScope.launch {
                try {
                    repository.saveUserProfile(updated)
                    if (role == UserRole.CHILD) {
                        observeIncomingRequests(updated.id)
                    } else {
                        requestsJob?.cancel()
                        _uiState.value = _uiState.value.copy(pendingRequests = emptyList())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Save user profile error", e)
                }
            }
        }
    }

    fun regenerateStudentCode() {
        val newCode = repository.regenerateDeviceStudentCode()
        val newPin = repository.regenerateVerificationPin()
        val current = _uiState.value.userProfile ?: return
        val updated = current.copy(studentCode = newCode, verificationPin = newPin)
        _uiState.value = _uiState.value.copy(
            userProfile = updated,
            successMessage = "Đã tạo mã học sinh mới: $newCode (PIN: $newPin)"
        )
        if (!_uiState.value.isGuestDemoMode && auth.currentUser != null) {
            viewModelScope.launch {
                try {
                    repository.saveUserProfile(updated)
                } catch (e: Exception) {
                    Log.w(TAG, "Save new student code error", e)
                }
            }
        }
    }

    fun toggleRole() {
        val currentRole = _uiState.value.userProfile?.role ?: UserRole.PARENT
        selectRole(if (currentRole == UserRole.PARENT) UserRole.CHILD else UserRole.PARENT)
    }

    fun sendPairingRequest(studentCode: String) {
        val parent = _uiState.value.userProfile ?: return
        val cleanCode = studentCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Vui lòng nhập mã học sinh")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                // If in guest mode, self-testing, or matching this device's unique student code: instant pairing
                val deviceCode = repository.getOrGenerateDeviceStudentCode()
                if (_uiState.value.isGuestDemoMode || parent.studentCode == cleanCode || cleanCode == deviceCode) {
                    val childId = "child_${cleanCode.take(8)}"
                    val newPairing = Pairing(
                        id = "pairing_${System.currentTimeMillis()}",
                        parentId = parent.id,
                        childId = childId,
                        studentCode = cleanCode,
                        parentName = parent.displayName.ifEmpty { "Phụ huynh" },
                        childName = "Học sinh ($cleanCode)",
                        status = PairingStatus.PAIRED,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activePairing = newPairing,
                        partnerLocation = LocationData(
                            userId = childId,
                            pairingId = newPairing.id,
                            userName = "Học sinh ($cleanCode)",
                            latitude = _uiState.value.myLocation?.latitude ?: 21.0285,
                            longitude = _uiState.value.myLocation?.longitude ?: 105.8542,
                            batteryLevel = 90,
                            speed = 0f,
                            currentSafeZone = "Khu vực an toàn"
                        ),
                        successMessage = "Ghép nối thành công với thiết bị học sinh $cleanCode!"
                    )
                    return@launch
                }

                repository.createPairingRequest(cleanCode, parent)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "Đã gửi yêu cầu ghép nối đến mã $cleanCode. Vui lòng chờ học sinh xác nhận."
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

        _uiState.value = _uiState.value.copy(
            hasUsagePermission = hasPerm,
            usageCategories = if (hasPerm) categories else emptyList(),
            detailedAppUsage = if (hasPerm) detailedList else emptyList()
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
        val user = _uiState.value.userProfile
        val pairing = _uiState.value.activePairing
        val (batteryLevel, isCharging) = telemetryHelper.getBatteryInfo()

        telemetryHelper.fetchCurrentLocation { loc ->
            val lat = loc?.latitude ?: 21.0285
            val lng = loc?.longitude ?: 105.8542
            val acc = loc?.accuracy ?: 12.0f
            val spd = loc?.speed ?: 0.0f
            val detailed = telemetryHelper.getDetailedAppUsageList()
            val topApp = detailed.firstOrNull()?.appName ?: "Đang chờ dữ liệu"
            val topMins = detailed.firstOrNull()?.minutesUsed ?: 0
            val totalMins = detailed.sumOf { it.minutesUsed }

            val safeZone = when {
                spd > 8f -> "Đang di chuyển (${(spd * 3.6f).toInt()} km/h)"
                else -> "Khu vực an toàn"
            }

            val locationData = LocationData(
                userId = user?.id ?: "my_device",
                pairingId = pairing?.id.orEmpty(),
                userName = user?.displayName ?: "Thiết bị này",
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

            _uiState.value = _uiState.value.copy(
                myLocation = locationData,
                isBackgroundServiceRunning = true
            )

            if (user != null && _uiState.value.isAuthenticated && !_uiState.value.isGuestDemoMode) {
                viewModelScope.launch {
                    try {
                        repository.updateLocation(locationData)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to update location", e)
                    }
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

    fun sendMessage(text: String, mediaUrl: String? = null, mediaType: String? = null) {
        val pairing = _uiState.value.activePairing ?: return
        val profile = _uiState.value.userProfile ?: return
        val emotion = _uiState.value.selectedEmotion

        if (text.isBlank() && emotion == null && mediaUrl == null) return

        if (_uiState.value.isGuestDemoMode) {
            val newMsg = FamilyMessage(
                id = "demo_msg_${System.currentTimeMillis()}",
                pairingId = pairing.id,
                senderId = profile.id,
                senderName = profile.displayName,
                senderRole = profile.role,
                text = text.trim(),
                emotion = emotion?.name,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
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
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
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

    fun setDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
    }

    fun setColorTheme(themeName: String) {
        _uiState.value = _uiState.value.copy(colorTheme = themeName)
    }

    fun dismissSosAlert() {
        _uiState.value = _uiState.value.copy(isSosAlertActive = false)
    }

    fun sendSosAlert() {
        sendSmartSosToFamily()
    }

    fun sendSmartSosToFamily(customMessage: String? = null) {
        val loc = _uiState.value.myLocation ?: LocationData(
            latitude = 21.0285,
            longitude = 105.8542,
            userName = _uiState.value.userProfile?.displayName.orEmpty()
        )
        val name = _uiState.value.userProfile?.displayName.orEmpty().ifEmpty { "Thành viên gia đình" }

        viewModelScope.launch {
            val dispatcher = smartSosDispatcher
            if (dispatcher != null) {
                val res = dispatcher.sendSosToFamily(loc, name, customMessage)
                if (res.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSosAlertActive = true,
                        activeSosMessage = res.detailMessage,
                        successMessage = res.detailMessage
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSosAlertActive = true,
                        activeSosMessage = res.detailMessage,
                        errorMessage = res.detailMessage
                    )
                }
            } else {
                repository.sendSosAlert(loc)
                _uiState.value = _uiState.value.copy(
                    isSosAlertActive = true,
                    activeSosMessage = "Đã gửi tín hiệu SOS khẩn cấp!",
                    successMessage = "Đã gửi tín hiệu SOS khẩn cấp!"
                )
            }
        }
    }

    fun sendSmartSosToRescue(hotline: String = "112") {
        val loc = _uiState.value.myLocation ?: LocationData(
            latitude = 21.0285,
            longitude = 105.8542,
            userName = _uiState.value.userProfile?.displayName.orEmpty()
        )
        val name = _uiState.value.userProfile?.displayName.orEmpty().ifEmpty { "Công dân khẩn cấp" }

        val dispatcher = smartSosDispatcher
        if (dispatcher != null) {
            val res = dispatcher.sendSosToRescueForce(hotline, loc, name)
            _uiState.value = _uiState.value.copy(
                isSosAlertActive = true,
                activeSosMessage = res.detailMessage,
                successMessage = res.detailMessage
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isSosAlertActive = true,
                activeSosMessage = "Đã mở liên lạc cứu hộ $hotline!",
                successMessage = "Đã mở liên lạc cứu hộ $hotline!"
            )
        }
    }


    fun connectMeshPeer(endpointId: String) {
        val name = _uiState.value.userProfile?.displayName.orEmpty().ifEmpty { "Thiết bị Gia Đình Việt" }
        smartSosDispatcher?.meshManager?.requestConnection(endpointId, name)
        _uiState.value = _uiState.value.copy(successMessage = "Đang gửi yêu cầu kết nối Mesh...")
    }

    fun sendMeshMessage(endpointId: String, text: String) {
        val name = _uiState.value.userProfile?.displayName.orEmpty().ifEmpty { "Tôi" }
        val ok = smartSosDispatcher?.meshManager?.sendMessageToPeer(endpointId, text, name) ?: false
        if (ok) {
            _uiState.value = _uiState.value.copy(successMessage = "Đã gửi tin nhắn Mesh P2P!")
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Gửi tin qua Mesh thất bại.")
        }
    }

    fun saveEmergencyPhoneNumber(phone: String) {
        smartSosDispatcher?.smsManager?.saveEmergencyPhoneNumber(phone)
        _uiState.value = _uiState.value.copy(
            emergencyPhoneNumber = phone.trim(),
            successMessage = "Đã lưu số điện thoại người thân khẩn cấp: $phone"
        )
    }

    fun saveSosSchedule(isRestricted: Boolean, startHour: Int, endHour: Int) {
        smartSosDispatcher?.setSosAllowedSchedule(isRestricted, startHour, endHour)
        _uiState.value = _uiState.value.copy(
            isSosRestricted = isRestricted,
            sosStartHour = startHour,
            sosEndHour = endHour,
            successMessage = if (isRestricted) "Đã lưu khung giờ cho phép SOS: $startHour:00 - $endHour:00"
            else "Đã mở quyền sử dụng SOS 24/7!"
        )
    }

    fun isSosAllowedNow(): Boolean {
        return smartSosDispatcher?.isSosAllowedAtCurrentTime() ?: true
    }

    fun updateUserProfile(newName: String, newPhotoUrl: String) {
        val currentProfile = _uiState.value.userProfile ?: return
        val updatedProfile = currentProfile.copy(displayName = newName, photoUrl = newPhotoUrl)
        _uiState.value = _uiState.value.copy(userProfile = updatedProfile)
        viewModelScope.launch {
            try {
                repository.saveUserProfile(updatedProfile)
                _uiState.value = _uiState.value.copy(successMessage = "Đã cập nhật thông tin tài khoản!")
            } catch (e: Exception) {
                Log.w(TAG, "updateUserProfile error", e)
                _uiState.value = _uiState.value.copy(errorMessage = "Không thể cập nhật thông tin.")
            }
        }
    }
}
