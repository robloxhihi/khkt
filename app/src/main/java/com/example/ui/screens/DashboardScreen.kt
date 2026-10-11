package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmotionTag
import com.example.data.model.PairingRequest
import com.example.data.model.PairingStatus
import com.example.data.model.UserRole
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoProgressBar
import com.example.ui.components.BentoStatCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceCardElevated
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var inputStudentCode by remember { mutableStateOf("") }

    val userProfile = uiState.userProfile
    val isChild = userProfile?.role == UserRole.CHILD
    val activePairing = uiState.activePairing
    val isPaired = activePairing != null && activePairing.status == PairingStatus.PAIRED
    val studentCode = userProfile?.studentCode.orEmpty().ifEmpty { "HS-CONNECT" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // User Profile Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonViolet, Color(0xFF1877F2))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfile?.displayName?.take(1)?.uppercase() ?: if (isChild) "H" else "P",
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
                        text = if (isChild) "Học sinh • $studentCode" else "Phụ huynh",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPaired) Icons.Default.Link else Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = if (isPaired) NeonEmerald else NeonAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPaired) "Đã kết nối" else "Chưa ghép nối",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaired) NeonEmerald else NeonAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UNPAIRED STATE: Concise pairing cards (No mock demo data!)
        if (!isPaired) {
            if (isChild) {
                // Child: Display Real Student Code
                BentoCard(
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "child_student_code_card"
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MÃ KẾT NỐI HỌC SINH",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Mã học sinh", studentCode)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, "Đã sao chép: $studentCode", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SpaceCardElevated,
                                    contentColor = NeonCyan
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sao chép", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpaceCardElevated)
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = studentCode,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Đưa mã này cho phụ huynh nhập để hoàn tất ghép nối.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Pending requests from parents
                if (uiState.pendingRequests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "YÊU CẦU GHÉP NỐI (${uiState.pendingRequests.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.pendingRequests.forEach { req ->
                        BentoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            borderColor = NeonAmber
                        ) {
                            Column {
                                Text(
                                    text = "Phụ huynh: ${req.parentName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = req.parentEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onRejectPairing(req.id) },
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
                                        onClick = { onAcceptPairing(req) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonEmerald,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text("Đồng ý", fontWeight = FontWeight.Bold)
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GHÉP NỐI THIẾT BỊ HỌC SINH",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonViolet,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputStudentCode,
                            onValueChange = { inputStudentCode = it.uppercase() },
                            label = { Text("Nhập mã học sinh (vd: HS-8F29K)") },
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
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (inputStudentCode.isNotBlank()) {
                                    onSendPairingRequest(inputStudentCode)
                                    inputStudentCode = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("send_pair_request_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gửi yêu cầu ghép nối", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real Telemetry of THIS device
            val myLoc = uiState.myLocation
            val totalTodayMins = uiState.detailedAppUsage.sumOf { it.minutesUsed }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.GpsFixed,
                    iconTint = NeonCyan,
                    title = "GPS THIẾT BỊ",
                    value = if (myLoc != null) "Hoạt động" else "Đang dò",
                    subtitle = if (myLoc != null) "±${myLoc.accuracy.toInt()}m" else "Vệ tinh",
                    testTag = "device_gps_stat"
                )

                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    iconTint = NeonEmerald,
                    title = "GIỜ DÙNG HÔM NAY",
                    value = "$totalTodayMins phút",
                    subtitle = "Thiết bị này",
                    testTag = "device_usage_stat"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Map Shortcut Card
            Button(
                onClick = { onNavigateToTab(1) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpaceCardElevated,
                    contentColor = NeonCyan
                )
            ) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xem bản đồ GPS thực tế thiết bị", fontWeight = FontWeight.Bold)
            }
        } else {
            // PAIRED STATE: Real Active Connection Bento-Grid
            val partnerLoc = uiState.partnerLocation
            val partnerName = if (isChild) activePairing.parentName else activePairing.childName

            // Partner Summary Card
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = partnerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = partnerLoc?.currentSafeZone?.ifEmpty { "Khu vực an toàn" } ?: "Đang trực tuyến",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${partnerLoc?.batteryLevel ?: 85}%",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

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
                            Text("⚡ Bump (${uiState.lastBumpCount})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            Text("💬 Nhắn tin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onNavigateToTab(1) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet,
                                contentColor = Color.White
                            )
                        ) {
                            Text("📍 Bản đồ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val sched = uiState.usageSchedule
                val target = sched?.dailyTargetMinutes ?: 120
                val used = sched?.usedMinutes ?: (partnerLoc?.totalUsedMinutes ?: 45)
                val remaining = (target - used).coerceAtLeast(0)

                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.HourglassTop,
                    iconTint = NeonEmerald,
                    title = "CÒN LẠI HÔM NAY",
                    value = "$remaining ph",
                    subtitle = "Mục tiêu: $target ph",
                    testTag = "remaining_stat_card"
                )

                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Schedule,
                    iconTint = NeonCyan,
                    title = "ĐÃ DÙNG",
                    value = "$used ph",
                    subtitle = "Trong ngày",
                    testTag = "used_stat_card"
                )
            }
        }
    }
}
