package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmotionTag
import com.example.data.model.ExtraTimeRequest
import com.example.data.model.PairingRequest
import com.example.data.model.PairingStatus
import com.example.data.model.PrivacyRequest
import com.example.data.model.RequestStatus
import com.example.data.model.UserRole
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoProgressBar
import com.example.ui.components.BentoRadarCard
import com.example.ui.components.BentoStatCard
import com.example.ui.components.EmotionSelectorRow
import com.example.ui.components.IosDynamicIslandPill
import com.example.ui.components.LiveBackgroundTelemetryCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceCardElevated
import com.example.ui.theme.SpaceCardSurface
import com.example.ui.theme.SpaceDarkBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.VesperaUiState

@Composable
fun DashboardScreen(
    uiState: VesperaUiState,
    onSendPairingRequest: (String) -> Unit,
    onAcceptPairing: (PairingRequest) -> Unit,
    onRejectPairing: (String) -> Unit,
    onApproveExtraTime: (String, Int) -> Unit,
    onRejectExtraTime: (String) -> Unit,
    onApprovePrivacy: (String, String) -> Unit,
    onRejectPrivacy: (String) -> Unit,
    onRequestExtraTimeClick: () -> Unit,
    onRequestPrivacyClick: () -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onSelectEmotion: (EmotionTag) -> Unit,
    onSwitchDemoRole: (UserRole) -> Unit = {},
    onSendZenlyBump: () -> Unit
) {
    val scrollState = rememberScrollState()
    var inputStudentCode by remember { mutableStateOf("") }

    val userProfile = uiState.userProfile
    val isChild = userProfile?.role == UserRole.CHILD
    val activePairing = uiState.activePairing
    val isPaired = activePairing != null && activePairing.status == PairingStatus.PAIRED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        if (uiState.isIosPreviewMode) {
            val partnerLoc = uiState.partnerLocation
            IosDynamicIslandPill(
                partnerName = if (isChild) uiState.activePairing?.parentName.orEmpty().ifEmpty { "Phụ huynh" }
                              else uiState.activePairing?.childName.orEmpty().ifEmpty { "Bé An" },
                safeZone = partnerLoc?.currentSafeZone?.ifEmpty { "Trường học" } ?: "Trường học",
                batteryPct = partnerLoc?.batteryLevel ?: 84,
                isCharging = partnerLoc?.isCharging ?: true,
                activeApp = partnerLoc?.topActiveApp?.ifEmpty { "YouTube" } ?: "YouTube",
                appMins = partnerLoc?.topActiveAppMinutes ?: 45
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonViolet, NeonCyan)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfile?.displayName?.take(1)?.uppercase() ?: "V",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = userProfile?.displayName?.ifEmpty { "Gia Đình Việt" } ?: "Gia Đình Việt",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isChild) "Học sinh • ${userProfile?.studentCode}" else "Phụ huynh quản lý",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isChild) NeonCyan else NeonViolet
                    )
                }
            }

            // Connection Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPaired) NeonEmerald.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (isPaired) NeonEmerald else NeonAmber,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isPaired) "Đã kết nối ⚡" else "Chưa ghép nối",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPaired) NeonEmerald else NeonAmber,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // UNPAIRED STATE: Setup / Invitation Cards
        if (!isPaired) {
            if (isChild) {
                // Child: Display Permanent Student Code and Incoming Requests
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "child_student_code_card"
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MÃ ĐỊNH DANH HỌC SINH LÂU DÀI",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Đưa mã này cho phụ huynh nhập vào máy của họ để thiết lập liên kết gia đình bảo mật.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SpaceCardElevated)
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile?.studentCode?.ifEmpty { "HS-8F29K" } ?: "HS-8F29K",
                                style = MaterialTheme.typography.displayMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pending requests for student
                if (uiState.pendingRequests.isNotEmpty()) {
                    Text(
                        text = "YÊU CẦU GHÉP NỐI ĐANG CHỜ (${uiState.pendingRequests.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    uiState.pendingRequests.forEach { req ->
                        BentoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            borderColor = NeonAmber
                        ) {
                            Column {
                                Text(
                                    text = "Phụ huynh: ${req.parentName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Email: ${req.parentEmail}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { onRejectPairing(req.id) },
                                        modifier = Modifier.weight(1f).testTag("reject_pair_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SpaceCardElevated,
                                            contentColor = NeonRose
                                        )
                                    ) {
                                        Text("Từ chối")
                                    }
                                    Button(
                                        onClick = { onAcceptPairing(req) },
                                        modifier = Modifier.weight(1f).testTag("accept_pair_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonEmerald,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text("Đồng ý kết nối", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Parent: Enter Student Code
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "parent_enter_code_card"
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                tint = NeonViolet,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KẾT NỐI VỚI THIẾT BỊ CỦA CON",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonViolet,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Nhập mã định danh học sinh (hiển thị trên màn hình ứng dụng Vespera của con) để gửi yêu cầu ghép nối.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = inputStudentCode,
                            onValueChange = { inputStudentCode = it.uppercase() },
                            label = { Text("Mã học sinh (vd: HS-8F29K)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_code_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = SpaceCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonViolet
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onSendPairingRequest(inputStudentCode)
                                inputStudentCode = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_pair_request_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gửi yêu cầu ghép nối", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // PAIRED STATE: Full Bento-Grid Active Companion Dashboard
        if (isPaired) {
            val partnerLoc = uiState.partnerLocation
            val partnerName = if (isChild) activePairing.parentName else activePairing.childName
            val isPrivacy = partnerLoc?.isPrivacyActive == true

            // AI Family Advisor Card
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonCyan.copy(alpha = 0.5f),
                testTag = "ai_advisor_card"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🤖", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI CỐ VẤN GIA ĐÌNH THÔNG MINH",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "LIVE", style = MaterialTheme.typography.labelSmall, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = uiState.smartAdvice,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSendZenlyBump,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpaceCardElevated,
                                contentColor = NeonAmber
                            )
                        ) {
                            Text("👋 Chạm tay Bump (${uiState.lastBumpCount})", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onNavigateToTab(3) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpaceCardElevated,
                                contentColor = NeonCyan
                            )
                        ) {
                            Text("💬 Nhắn tin gia đình", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Bento Radar Card (Zenly inspired)
            BentoRadarCard(
                partnerName = partnerName,
                distanceKm = "Vị trí chuẩn",
                batteryPct = partnerLoc?.batteryLevel ?: 88,
                isCharging = partnerLoc?.isCharging ?: false,
                speedKmh = partnerLoc?.speed ?: 0.0f,
                isPrivacyActive = isPrivacy,
                lastUpdateText = "Vừa cập nhật",
                onTap = { onNavigateToTab(1) } // Navigate to Map tab
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Background Foreground Service & Real App Usage Tracking Card
            LiveBackgroundTelemetryCard(
                partnerName = partnerName,
                safeZone = partnerLoc?.currentSafeZone?.ifEmpty { "Trường THPT Chu Văn An" } ?: "Trường THPT Chu Văn An",
                topApp = partnerLoc?.topActiveApp?.ifEmpty { "YouTube" } ?: "YouTube",
                topAppMins = partnerLoc?.topActiveAppMinutes ?: 45,
                totalMins = partnerLoc?.totalUsedMinutes ?: 65,
                isBackgroundActive = uiState.isBackgroundServiceRunning
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Bento Stats Row (Battery & Daily Screen Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val sched = uiState.usageSchedule
                val target = sched?.dailyTargetMinutes ?: 120
                val used = sched?.usedMinutes ?: 45
                val remaining = (target - used).coerceAtLeast(0)

                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BatteryChargingFull,
                    iconTint = NeonCyan,
                    title = "PIN THIẾT BỊ",
                    value = "${partnerLoc?.batteryLevel ?: 88}%",
                    subtitle = if (partnerLoc?.isCharging == true) "Đang sạc ⚡" else "Bình thường",
                    testTag = "battery_stat_card"
                )

                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.HourglassTop,
                    iconTint = NeonEmerald,
                    title = "CÒN LẠI HÔM NAY",
                    value = "$remaining ph",
                    subtitle = "Mục tiêu: $target ph",
                    testTag = "remaining_stat_card"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Bento Screen Time Progress & Quick Action
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                testTag = "screen_time_bento_card"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LỊCH DÙNG ĐIỆN THOẠI",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonAmber,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "Xem chi tiết →",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            modifier = Modifier.clickable { onNavigateToTab(2) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val sched = uiState.usageSchedule
                    BentoProgressBar(
                        usedMinutes = sched?.usedMinutes ?: 45,
                        targetMinutes = sched?.dailyTargetMinutes ?: 120
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isChild) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onRequestExtraTimeClick,
                                modifier = Modifier.weight(1f).testTag("ask_extra_time_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpaceCardElevated,
                                    contentColor = NeonAmber
                                )
                            ) {
                                Text("⏱️ Xin thêm giờ")
                            }

                            Button(
                                onClick = onRequestPrivacyClick,
                                modifier = Modifier.weight(1f).testTag("ask_privacy_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpaceCardElevated,
                                    contentColor = NeonCyan
                                )
                            ) {
                                Text("🛡️ Xin riêng tư")
                            }
                        }
                    } else {
                        Text(
                            text = "Khung giờ cho phép: ${sched?.allowedStartHour ?: 7}:00 - ${sched?.allowedEndHour ?: 21}:30 (${sched?.daysOfWeek ?: "T2 - CN"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Pending Extra Time Requests (for Parent)
            val pendingExtra = uiState.extraTimeRequests.filter { it.status == RequestStatus.PENDING }
            if (!isChild && pendingExtra.isNotEmpty()) {
                Text(
                    text = "YÊU CẦU XIN THÊM GIỜ CHỜ DUYỆT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonAmber,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                pendingExtra.forEach { req ->
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        borderColor = NeonAmber
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${req.childName} xin thêm +${req.minutesRequested} phút",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Đang chờ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lý do: ${req.reason.ifEmpty { "Không nêu lý do" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRejectExtraTime(req.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SpaceCardElevated,
                                        contentColor = NeonRose
                                    )
                                ) {
                                    Text("Từ chối")
                                }
                                Button(
                                    onClick = { onApproveExtraTime(req.id, req.minutesRequested) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonAmber,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Duyệt +${req.minutesRequested} ph", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5. Pending Privacy Hours Requests (for Parent)
            val pendingPrivacy = uiState.privacyRequests.filter { it.status == RequestStatus.PENDING }
            if (!isChild && pendingPrivacy.isNotEmpty()) {
                Text(
                    text = "YÊU CẦU KHUNG GIỜ RIÊNG TƯ CHỜ DUYỆT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                pendingPrivacy.forEach { req ->
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        borderColor = NeonCyan
                    ) {
                        Column {
                            Text(
                                text = "${req.childName} xin khung giờ: ${req.startHour}:00 - ${req.endHour}:00",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lý do: ${req.reason.ifEmpty { "Gặp gỡ bạn bè / riêng tư" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRejectPrivacy(req.id) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SpaceCardElevated,
                                        contentColor = NeonRose
                                    )
                                ) {
                                    Text("Từ chối")
                                }
                                Button(
                                    onClick = { onApprovePrivacy(req.id, req.childId) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text("Duyệt riêng tư 🛡️", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 6. Family Emotion Sharing Bar
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                testTag = "emotion_bento_card"
            ) {
                Column {
                    Text(
                        text = "CẢM XÚC HÔM NAY CỦA BẠN",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonViolet,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    EmotionSelectorRow(
                        selectedEmotion = uiState.selectedEmotion,
                        onSelectEmotion = onSelectEmotion
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Quick Chat Card
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigateToTab(3) },
                testTag = "chat_shortcut_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Trò chuyện gia đình realtime",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val latestMsg = uiState.messages.lastOrNull()
                        Text(
                            text = latestMsg?.let { "${it.senderName}: ${it.text}" } ?: "Chưa có tin nhắn nào. Nhấn để gửi tin nhắn!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "Mở chat 💬",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
