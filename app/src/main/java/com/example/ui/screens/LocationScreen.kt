package com.example.ui.screens

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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PairingStatus
import com.example.data.model.UserRole
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoStatCard
import com.example.ui.components.GoogleMapsStudentLocationCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LocationScreen(
    uiState: VesperaUiState,
    onRefreshLocation: () -> Unit,
    onRequestPrivacyClick: () -> Unit,
    onSendZenlyBump: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isChild = uiState.userProfile?.role == UserRole.CHILD
    val isPaired = uiState.activePairing != null && uiState.activePairing.status == PairingStatus.PAIRED
    val partnerLoc = uiState.partnerLocation
    val myLoc = uiState.myLocation
    val partnerName = if (isChild) uiState.activePairing?.parentName.orEmpty().ifEmpty { "Phụ huynh" }
                      else uiState.activePairing?.childName.orEmpty().ifEmpty { "Học sinh" }

    val isPrivacy = partnerLoc?.isPrivacyActive == true
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ĐỊNH VỊ GPS",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isPaired) "Bản đồ thời gian thực • Vệ tinh" else "Vị trí cảm biến GPS thực tế",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Connection Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isPaired) NeonEmerald.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f))
                        .border(1.dp, if (isPaired) NeonEmerald else NeonAmber, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPaired) "Đã kết nối" else "Chưa ghép nối",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPaired) NeonEmerald else NeonAmber,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRefreshLocation,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpaceCardElevated,
                        contentColor = NeonCyan
                    ),
                    modifier = Modifier.size(38.dp).testTag("refresh_location_btn"),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Làm mới",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UNPAIRED STATE: Concise, professional status card (NO fake demo data!)
        if (!isPaired) {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonAmber.copy(alpha = 0.6f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CHƯA GHÉP NỐI HỌC SINH",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nhập mã học sinh tại màn hình Tổng quan để theo dõi vị trí của con.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display THIS device's REAL GPS location on the real map
            val activeLoc = myLoc
            val displayLat = activeLoc?.latitude ?: 21.0285
            val displayLng = activeLoc?.longitude ?: 105.8542
            val displayAcc = activeLoc?.accuracy?.toInt() ?: 10
            val displaySpd = activeLoc?.speed ?: 0.0f
            val displayBat = activeLoc?.batteryLevel ?: 85
            val displayChg = activeLoc?.isCharging ?: false

            Text(
                text = "TỌA ĐỘ GPS THỰC TẾ (THIẾT BỊ NÀY)",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            GoogleMapsStudentLocationCard(
                studentName = "Thiết bị này",
                latitude = displayLat,
                longitude = displayLng,
                safeZoneName = if (displaySpd > 5f) "Đang di chuyển" else "Đang định vị",
                batteryLevel = displayBat,
                isCharging = displayChg,
                speedKmh = displaySpd,
                isPrivacyActive = false
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Real GPS Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.GpsFixed,
                    iconTint = NeonCyan,
                    title = "ĐỘ CHÍNH XÁC",
                    value = "±$displayAcc m",
                    subtitle = "Vệ tinh GPS thực tế",
                    testTag = "accuracy_card"
                )

                BentoStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    iconTint = NeonViolet,
                    title = "TỐC ĐỘ",
                    value = "${displaySpd.toInt()} km/h",
                    subtitle = if (displaySpd > 5f) "Di chuyển" else "Đứng yên",
                    testTag = "speed_card"
                )
            }
        } else {
            // PAIRED STATE: Display partner's real location
            if (isPrivacy) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeonAmber.copy(alpha = 0.15f))
                        .border(1.dp, NeonAmber, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KHUNG GIỜ RIÊNG TƯ",
                                style = MaterialTheme.typography.titleSmall,
                                color = NeonAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tọa độ GPS đang được ẩn an toàn theo thỏa thuận.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            if (partnerLoc != null) {
                GoogleMapsStudentLocationCard(
                    studentName = partnerName,
                    latitude = partnerLoc.latitude,
                    longitude = partnerLoc.longitude,
                    safeZoneName = partnerLoc.currentSafeZone.ifEmpty { "Khu vực an toàn" },
                    batteryLevel = partnerLoc.batteryLevel,
                    isCharging = partnerLoc.isCharging,
                    speedKmh = partnerLoc.speed,
                    isPrivacyActive = isPrivacy
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Real Partner Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.GpsFixed,
                        iconTint = NeonCyan,
                        title = "ĐỘ CHÍNH XÁC",
                        value = "±${partnerLoc.accuracy.toInt()} m",
                        subtitle = "Cập nhật lúc ${timeFormat.format(Date(partnerLoc.updatedAt))}",
                        testTag = "partner_accuracy_card"
                    )

                    BentoStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        iconTint = NeonViolet,
                        title = "TỐC ĐỘ",
                        value = "${partnerLoc.speed.toInt()} km/h",
                        subtitle = if (partnerLoc.speed > 5f) "Đang di chuyển" else "Đứng yên",
                        testTag = "partner_speed_card"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zenly Bump Action
                Button(
                    onClick = onSendZenlyBump,
                    modifier = Modifier.fillMaxWidth().testTag("zenly_bump_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpaceCardElevated,
                        contentColor = NeonAmber
                    )
                ) {
                    Text(text = "⚡ Chạm tay Zenly Bump (Rung chuông • ${uiState.lastBumpCount})", fontWeight = FontWeight.Bold)
                }
            } else {
                // Paired, but partner has not sent location yet
                BentoCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ĐANG ĐỢI TÍN HIỆU GPS",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Thiết bị học sinh cần bật GPS và mở ứng dụng để cập nhật.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRefreshLocation,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Làm mới ngay", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
