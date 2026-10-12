package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ExtraTimeRequest
import com.example.data.model.FamilyMessage
import com.example.data.model.LocationData
import com.example.data.model.LocationHistoryEntry
import com.example.data.model.Pairing
import com.example.data.model.PairingRequest
import com.example.data.model.PairingStatus
import com.example.data.model.PrivacyRequest
import com.example.data.model.RequestStatus
import com.example.data.model.UsageSchedule
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "VesperaRepository"

class VesperaRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth, private val context: Context? = null) {

    constructor(context: Context) : this(
        try {
            val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            if (resId != 0) {
                val dbId = context.getString(resId)
                if (dbId.isNotBlank()) FirebaseFirestore.getInstance(dbId) else FirebaseFirestore.getInstance()
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        },
        FirebaseAuth.getInstance(),
        context
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
    }

    /**
     * Observe or get UserProfile safely without throwing
     */
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observeUserProfile error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java)
                trySend(profile)
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
        } catch (e: Exception) {
            Log.w(TAG, "getUserProfile error", e)
            null
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = profile.id.ifEmpty { auth.currentUser?.uid ?: "user_${System.currentTimeMillis()}" }
        try {
            db.collection("users").document(uid).set(profile.copy(id = uid)).await()
        } catch (e: Exception) {
            Log.w(TAG, "saveUserProfile error", e)
        }
    }

    /**
     * Student Code generator (HS-XXXXX: Unique 5-char alphanumeric)
     */
    fun generateStudentCode(): String {
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val randomPart = (1..5)
            .map { allowedChars.random() }
            .joinToString("")
        return "HS-$randomPart"
    }

    /**
     * Unique persistent student code per device (mỗi người một mã khác nhau)
     */
    fun getOrGenerateDeviceStudentCode(): String {
        val prefs = context?.getSharedPreferences("vespera_device_prefs", android.content.Context.MODE_PRIVATE)
        var code = prefs?.getString("unique_student_code", "") ?: ""
        if (code.isBlank() || code == "HS-8F29K") {
            code = generateStudentCode()
            prefs?.edit()?.putString("unique_student_code", code)?.apply()
        }
        return code
    }

    fun regenerateDeviceStudentCode(): String {
        val prefs = context?.getSharedPreferences("vespera_device_prefs", android.content.Context.MODE_PRIVATE)
        val code = generateStudentCode()
        prefs?.edit()?.putString("unique_student_code", code)?.apply()
        return code
    }

    fun getOrGenerateVerificationPin(): String {
        val prefs = context?.getSharedPreferences("vespera_device_prefs", android.content.Context.MODE_PRIVATE)
        var pin = prefs?.getString("unique_verification_pin", "") ?: ""
        if (pin.isBlank() || pin == "8888") {
            pin = (1000..9999).random().toString()
            prefs?.edit()?.putString("unique_verification_pin", pin)?.apply()
        }
        return pin
    }

    fun regenerateVerificationPin(): String {
        val prefs = context?.getSharedPreferences("vespera_device_prefs", android.content.Context.MODE_PRIVATE)
        val pin = (1000..9999).random().toString()
        prefs?.edit()?.putString("unique_verification_pin", pin)?.apply()
        return pin
    }

    /**
     * Find student user by student code
     */
    suspend fun findStudentByCode(studentCode: String): UserProfile? {
        return try {
            val query = db.collection("users")
                .whereEqualTo("studentCode", studentCode.trim().uppercase())
                .whereEqualTo("role", UserRole.CHILD.name)
                .limit(1)
                .get()
                .await()
            query.documents.firstOrNull()?.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "findStudentByCode error", e)
            null
        }
    }

    /**
     * Send pairing request from Parent to Child
     */
    suspend fun createPairingRequest(studentCode: String, parentProfile: UserProfile): PairingRequest {
        val student = findStudentByCode(studentCode)
            ?: throw IllegalArgumentException("Không tìm thấy học sinh với mã $studentCode")

        val requestId = UUID.randomUUID().toString()
        val request = PairingRequest(
            id = requestId,
            studentCode = studentCode.trim().uppercase(),
            parentId = parentProfile.id,
            parentName = parentProfile.displayName.ifEmpty { "Phụ huynh" },
            parentEmail = parentProfile.email,
            childId = student.id,
            status = RequestStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        db.collection("pairing_requests").document(requestId).set(request).await()
        return request
    }

    /**
     * Observe incoming pairing requests for a child
     */
    fun observePendingRequestsForChild(childId: String): Flow<List<PairingRequest>> = callbackFlow {
        if (childId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val query = db.collection("pairing_requests")
            .whereEqualTo("childId", childId)
            .whereEqualTo("status", RequestStatus.PENDING.name)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "observePendingRequestsForChild error", error)
                return@addSnapshotListener
            }
            val requests = snapshot?.toObjects(PairingRequest::class.java) ?: emptyList()
            trySend(requests)
        }
        awaitClose { listener.remove() }
    }

    /**
     * Accept pairing request and create permanent Pairing
     */
    suspend fun acceptPairingRequest(request: PairingRequest, childProfile: UserProfile) {
        val pairingId = "pair_${request.parentId}_${request.childId}"
        val pairing = Pairing(
            id = pairingId,
            parentId = request.parentId,
            childId = request.childId,
            studentCode = request.studentCode,
            parentName = request.parentName,
            childName = childProfile.displayName.ifEmpty { "Học sinh" },
            status = PairingStatus.PAIRED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        try {
            db.collection("pairings").document(pairingId).set(pairing).await()
            db.collection("pairing_requests").document(request.id).update(
                "status", RequestStatus.APPROVED.name
            ).await()
            db.collection("users").document(request.childId).update(
                mapOf(
                    "pairedWith" to request.parentId,
                    "activePairingId" to pairingId
                )
            ).await()
            db.collection("users").document(request.parentId).update(
                mapOf(
                    "pairedWith" to request.childId,
                    "activePairingId" to pairingId
                )
            ).await()

            val defaultSchedule = UsageSchedule(
                id = "default_sched",
                pairingId = pairingId,
                title = "Lịch sinh hoạt chuẩn",
                dailyTargetMinutes = 120,
                usedMinutes = 45,
                allowedStartHour = 7,
                allowedStartMinute = 0,
                allowedEndHour = 21,
                allowedEndMinute = 30,
                daysOfWeek = "T2, T3, T4, T5, T6, T7, CN",
                isReminderOnly = true,
                isActive = true
            )
            db.collection("pairings").document(pairingId)
                .collection("schedules").document("default_sched").set(defaultSchedule).await()
        } catch (e: Exception) {
            Log.w(TAG, "acceptPairingRequest error", e)
        }
    }

    suspend fun rejectPairingRequest(requestId: String) {
        try {
            db.collection("pairing_requests").document(requestId).update(
                "status", RequestStatus.REJECTED.name
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "rejectPairingRequest error", e)
        }
    }

    suspend fun unpair(pairingId: String, parentId: String, childId: String) {
        try {
            db.collection("pairings").document(pairingId).update(
                "status", PairingStatus.UNPAIRED.name,
                "updatedAt", System.currentTimeMillis()
            ).await()
            db.collection("users").document(parentId).update(
                "activePairingId", "",
                "pairedWith", ""
            ).await()
            db.collection("users").document(childId).update(
                "activePairingId", "",
                "pairedWith", ""
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "unpair error", e)
        }
    }

    fun observePairing(pairingId: String): Flow<Pairing?> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observePairing error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(Pairing::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    fun isWifiOrInternetConnected(): Boolean {
        val ctx = context ?: return true
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            true
        }
    }

    suspend fun updateLocation(location: LocationData) {
        try {
            if (!isWifiOrInternetConnected() || auth.currentUser == null) {
                saveOfflineLocationLocally(location)
                Log.d(TAG, "Offline or unauthenticated: Saved GPS location locally.")
                return
            }

            flushOfflineLocations()
            db.collection("locations").document(location.userId).set(location).await()
        } catch (e: Exception) {
            Log.d(TAG, "updateLocation offline/permission fallback: ${e.message}")
            saveOfflineLocationLocally(location)
        }
    }

    suspend fun sendSosAlert(location: LocationData) {
        try {
            val alert = location.copy(
                isSos = true,
                updatedAt = System.currentTimeMillis()
            )
            if (auth.currentUser != null && isWifiOrInternetConnected()) {
                db.collection("sos_alerts").document("${alert.userId}_${alert.updatedAt}").set(alert).await()
                db.collection("locations").document(alert.userId).set(alert).await()
            } else {
                saveOfflineLocationLocally(alert)
                Log.d(TAG, "SOS alert saved locally (Offline/Auth pending)")
            }
        } catch (e: Exception) {
            Log.d(TAG, "sendSosAlert fallback: ${e.message}")
            saveOfflineLocationLocally(location)
        }
    }

    private fun saveOfflineLocationLocally(location: LocationData) {
        val ctx = context ?: return
        try {
            val prefs = ctx.getSharedPreferences("vespera_offline_gps", Context.MODE_PRIVATE)
            val jsonList = prefs.getString("queue", "[]") ?: "[]"
            val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, LocationData::class.java)
            val adapter = moshi.adapter<List<LocationData>>(type)
            val list = try { adapter.fromJson(jsonList) ?: emptyList() } catch (e: Exception) { emptyList() }

            val updated = list + location
            prefs.edit().putString("queue", adapter.toJson(updated)).apply()
        } catch (e: Exception) {
            Log.w(TAG, "saveOfflineLocationLocally error", e)
        }
    }

    suspend fun flushOfflineLocations() {
        val ctx = context ?: return
        try {
            if (!isWifiOrInternetConnected()) return
            val prefs = ctx.getSharedPreferences("vespera_offline_gps", Context.MODE_PRIVATE)
            val jsonList = prefs.getString("queue", "[]") ?: "[]"
            if (jsonList == "[]" || jsonList.isBlank()) return

            val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, LocationData::class.java)
            val adapter = moshi.adapter<List<LocationData>>(type)
            val list = try { adapter.fromJson(jsonList) ?: emptyList() } catch (e: Exception) { emptyList() }

            if (list.isNotEmpty()) {
                for (loc in list) {
                    try {
                        db.collection("locations").document(loc.userId).set(loc).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to flush offline location", e)
                    }
                }
                prefs.edit().remove("queue").apply()
                Log.d(TAG, "Đã có WiFi: Trả lại ${list.size} vị trí GPS đã lưu offline lên máy chủ thành công.")
            }
        } catch (e: Exception) {
            Log.w(TAG, "flushOfflineLocations error", e)
        }
    }

    fun observeLocation(userId: String): Flow<LocationData?> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        val listener = db.collection("locations").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeLocation error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(LocationData::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveLocationHistory(entry: LocationHistoryEntry) {
        try {
            if (auth.currentUser == null || !isWifiOrInternetConnected()) return
            val docId = entry.id.ifEmpty { "${entry.userId}_${entry.timestamp}" }
            val item = entry.copy(id = docId)
            db.collection("location_history").document(docId).set(item).await()
            if (entry.pairingId.isNotEmpty()) {
                db.collection("pairings").document(entry.pairingId)
                    .collection("location_history").document(docId).set(item).await()
            }
        } catch (e: Exception) {
            Log.d(TAG, "saveLocationHistory offline/permission bypass: ${e.message}")
        }
    }

    fun observeLocationHistory(pairingId: String): Flow<List<LocationHistoryEntry>> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("location_history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeLocationHistory error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(LocationHistoryEntry::class.java) }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeLocationHistoryForUser(userId: String): Flow<List<LocationHistoryEntry>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = db.collection("location_history")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeLocationHistoryForUser error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(LocationHistoryEntry::class.java) }
                    trySend(list)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeSchedule(pairingId: String): Flow<UsageSchedule?> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("schedules").document("default_sched")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeSchedule error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(UsageSchedule::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveSchedule(schedule: UsageSchedule) {
        try {
            db.collection("pairings").document(schedule.pairingId)
                .collection("schedules").document(schedule.id.ifEmpty { "default_sched" })
                .set(schedule).await()
        } catch (e: Exception) {
            Log.w(TAG, "saveSchedule error", e)
        }
    }

    suspend fun requestExtraTime(request: ExtraTimeRequest) {
        try {
            val reqId = request.id.ifEmpty { UUID.randomUUID().toString() }
            db.collection("pairings").document(request.pairingId)
                .collection("extra_time").document(reqId)
                .set(request.copy(id = reqId)).await()
        } catch (e: Exception) {
            Log.w(TAG, "requestExtraTime error", e)
        }
    }

    fun observeExtraTimeRequests(pairingId: String): Flow<List<ExtraTimeRequest>> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("extra_time")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeExtraTimeRequests error", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(ExtraTimeRequest::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun respondToExtraTime(pairingId: String, requestId: String, approved: Boolean, minutesToAdd: Int) {
        try {
            db.collection("pairings").document(pairingId)
                .collection("extra_time").document(requestId)
                .update(
                    mapOf(
                        "status" to if (approved) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name,
                        "respondedAt" to System.currentTimeMillis()
                    )
                ).await()

            if (approved) {
                val schedRef = db.collection("pairings").document(pairingId)
                    .collection("schedules").document("default_sched")
                val snap = schedRef.get().await()
                if (snap.exists()) {
                    val currentTarget = snap.getLong("dailyTargetMinutes") ?: 120L
                    schedRef.update("dailyTargetMinutes", currentTarget + minutesToAdd).await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "respondToExtraTime error", e)
        }
    }

    suspend fun requestPrivacyHours(request: PrivacyRequest) {
        try {
            val reqId = request.id.ifEmpty { UUID.randomUUID().toString() }
            db.collection("pairings").document(request.pairingId)
                .collection("privacy_requests").document(reqId)
                .set(request.copy(id = reqId)).await()
        } catch (e: Exception) {
            Log.w(TAG, "requestPrivacyHours error", e)
        }
    }

    fun observePrivacyRequests(pairingId: String): Flow<List<PrivacyRequest>> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("privacy_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observePrivacyRequests error", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(PrivacyRequest::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun respondToPrivacyRequest(pairingId: String, requestId: String, childId: String, approved: Boolean) {
        try {
            db.collection("pairings").document(pairingId)
                .collection("privacy_requests").document(requestId)
                .update(
                    mapOf(
                        "status" to if (approved) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name,
                        "respondedAt" to System.currentTimeMillis()
                    )
                ).await()

            if (approved) {
                db.collection("locations").document(childId).update(
                    "isPrivacyActive", true,
                    "updatedAt", System.currentTimeMillis()
                ).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "respondToPrivacyRequest error", e)
        }
    }

    suspend fun sendMessage(message: FamilyMessage) {
        try {
            val msgId = message.id.ifEmpty { UUID.randomUUID().toString() }
            db.collection("pairings").document(message.pairingId)
                .collection("messages").document(msgId)
                .set(message.copy(id = msgId)).await()
        } catch (e: Exception) {
            Log.w(TAG, "sendMessage error", e)
        }
    }

    suspend fun toggleMessageStar(pairingId: String, messageId: String, isStarred: Boolean) {
        try {
            db.collection("pairings").document(pairingId)
                .collection("messages").document(messageId)
                .update("isStarred", isStarred).await()
        } catch (e: Exception) {
            Log.w(TAG, "toggleMessageStar error", e)
        }
    }

    fun observeMessages(pairingId: String): Flow<List<FamilyMessage>> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeMessages error", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(FamilyMessage::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
}
