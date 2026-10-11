package com.example.data.repository

import android.content.Context
import com.example.R
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VesperaRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    /**
     * Observe or get UserProfile
     */
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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
        val snapshot = db.collection("users").document(userId).get().await()
        return if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        db.collection("users").document(uid).set(profile.copy(id = uid)).await()
    }

    /**
     * Student Code generator (HS-XXXXX)
     */
    fun generateStudentCode(): String {
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val randomPart = (1..5)
            .map { allowedChars.random() }
            .joinToString("")
        return "HS-$randomPart"
    }

    /**
     * Find student user by student code
     */
    suspend fun findStudentByCode(studentCode: String): UserProfile? {
        val query = db.collection("users")
            .whereEqualTo("studentCode", studentCode.trim().uppercase())
            .whereEqualTo("role", UserRole.CHILD.name)
            .limit(1)
            .get()
            .await()
        return query.documents.firstOrNull()?.toObject(UserProfile::class.java)
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
        val query = db.collection("pairing_requests")
            .whereEqualTo("childId", childId)
            .whereEqualTo("status", RequestStatus.PENDING.name)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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

        // Run batch / sequence
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

        // Initialize default schedule if none exists
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
    }

    /**
     * Reject pairing request
     */
    suspend fun rejectPairingRequest(requestId: String) {
        db.collection("pairing_requests").document(requestId).update(
            "status", RequestStatus.REJECTED.name
        ).await()
    }

    /**
     * Unpair and revoke access immediately
     */
    suspend fun unpair(pairingId: String, parentId: String, childId: String) {
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
    }

    /**
     * Observe Pairing object
     */
    fun observePairing(pairingId: String): Flow<Pairing?> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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

    /**
     * Realtime location telemetry
     */
    suspend fun updateLocation(location: LocationData) {
        db.collection("locations").document(location.userId).set(location).await()
    }

    fun observeLocation(userId: String): Flow<LocationData?> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("locations").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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

    /**
     * Schedules
     */
    fun observeSchedule(pairingId: String): Flow<UsageSchedule?> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("schedules").document("default_sched")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
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
        db.collection("pairings").document(schedule.pairingId)
            .collection("schedules").document(schedule.id.ifEmpty { "default_sched" })
            .set(schedule).await()
    }

    /**
     * Extra Time requests
     */
    suspend fun requestExtraTime(request: ExtraTimeRequest) {
        val reqId = request.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("pairings").document(request.pairingId)
            .collection("extra_time").document(reqId)
            .set(request.copy(id = reqId)).await()
    }

    fun observeExtraTimeRequests(pairingId: String): Flow<List<ExtraTimeRequest>> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("extra_time")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(ExtraTimeRequest::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun respondToExtraTime(pairingId: String, requestId: String, approved: Boolean, minutesToAdd: Int) {
        db.collection("pairings").document(pairingId)
            .collection("extra_time").document(requestId)
            .update(
                mapOf(
                    "status" to if (approved) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name,
                    "respondedAt" to System.currentTimeMillis()
                )
            ).await()

        if (approved) {
            // Bump schedule limit
            val schedRef = db.collection("pairings").document(pairingId)
                .collection("schedules").document("default_sched")
            val snap = schedRef.get().await()
            if (snap.exists()) {
                val currentTarget = snap.getLong("dailyTargetMinutes") ?: 120L
                schedRef.update("dailyTargetMinutes", currentTarget + minutesToAdd).await()
            }
        }
    }

    /**
     * Privacy Shield requests
     */
    suspend fun requestPrivacyHours(request: PrivacyRequest) {
        val reqId = request.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("pairings").document(request.pairingId)
            .collection("privacy_requests").document(reqId)
            .set(request.copy(id = reqId)).await()
    }

    fun observePrivacyRequests(pairingId: String): Flow<List<PrivacyRequest>> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("privacy_requests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(PrivacyRequest::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun respondToPrivacyRequest(pairingId: String, requestId: String, childId: String, approved: Boolean) {
        db.collection("pairings").document(pairingId)
            .collection("privacy_requests").document(requestId)
            .update(
                mapOf(
                    "status" to if (approved) RequestStatus.APPROVED.name else RequestStatus.REJECTED.name,
                    "respondedAt" to System.currentTimeMillis()
                )
            ).await()

        if (approved) {
            // Mask location
            db.collection("locations").document(childId).update(
                "isPrivacyActive", true,
                "updatedAt", System.currentTimeMillis()
            ).await()
        }
    }

    /**
     * Chat Messages
     */
    suspend fun sendMessage(message: FamilyMessage) {
        val msgId = message.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("pairings").document(message.pairingId)
            .collection("messages").document(msgId)
            .set(message.copy(id = msgId)).await()
    }

    suspend fun toggleMessageStar(pairingId: String, messageId: String, isStarred: Boolean) {
        db.collection("pairings").document(pairingId)
            .collection("messages").document(messageId)
            .update("isStarred", isStarred).await()
    }

    fun observeMessages(pairingId: String): Flow<List<FamilyMessage>> = callbackFlow {
        if (pairingId.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("pairings").document(pairingId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(FamilyMessage::class.java) ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }
}
