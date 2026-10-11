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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoRadarCard
import com.example.ui.components.BentoStatCard
import com.example.ui.components.GoogleMapsStudentLocationCard
import com.example.ui.components.IosDynamicIslandPill
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
    val partnerLoc = uiState.partnerLocation
    val isChild = uiState.userProfile?.role == UserRole.CHILD
    val partnerName = if (isChild) uiState.activePairing?.parentName.orEmpty().ifEmpty { "Phụ huynh" }
                      else uiState.activePairing?.childName.orEmpty().ifEmpty { "Học sinh" }

    val isPrivacy = partnerLoc?.isPrivacyActive == true
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val lastUpdated = partnerLoc?.updatedAt?.let { timeFormat.format(Date(it)) } ?: "Chưa có"

    // Check stale status (> 5 mins old)
    val isStale = partnerLoc != null && (System.currentTimeMillis() - partnerLoc.updatedAt > 300_000)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        if (uiState.isIosPreviewMode) {
            IosDynamicIslandPill(
                partnerName = partnerName,
                safeZone = partnerLoc?.currentSafeZone?.ifEmpty { "Trường học" } ?: "Trường học",
                batteryPct = partnerLoc?.batteryLevel ?: 84,
                isCharging = partnerLoc?.isCharging ?: true,
                activeApp = partnerLoc?.topActiveApp?.ifEmpty { "YouTube" } ?: "YouTube",
                appMins = partnerLoc?.topActiveAppMinutes ?: 45
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Screen Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ĐỊNH VỊ GIA ĐÌNH",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Công nghệ Zenly Radar • Cập nhật vệ tinh chính xác",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onRefreshLocation,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpaceCardElevated,
                    contentColor = NeonCyan
                ),
                modifier = Modifier.testTag("refresh_location_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Làm mới",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy Shield Notice if active
        if (isPrivacy) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(NeonAmber.copy(alpha = 0.15f))
                    .border(1.5.dp, NeonAmber, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonAmber.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "ĐANG BẬT KHUNG GIỜ RIÊNG TƯ",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tọa độ GPS đang được ẩn an toàn nhằm tôn trọng quyền riêng tư đã thống nhất.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Google Maps Reference Style Student Location Card (Matching User Image 1)
        GoogleMapsStudentLocationCard(
            studentName = partnerName,
            latitude = partnerLoc?.latitude ?: 21.0285,
            longitude = partnerLoc?.longitude ?: 105.8542,
            safeZoneName = partnerLoc?.currentSafeZone?.ifEmpty { "Trường THPT Chu Văn An" } ?: "Trường THPT Chu Văn An",
            batteryLevel = partnerLoc?.batteryLevel ?: 85,
            isCharging = partnerLoc?.isCharging ?: false,
            speedKmh = partnerLoc?.speed ?: 0.0f,
            isPrivacyActive = isPrivacy
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Radar Bento Card
        BentoRadarCard(
            partnerName = partnerName,
            distanceKm = if (isPrivacy) "Đang ẩn" else "Đang trực tuyến",
            batteryPct = partnerLoc?.batteryLevel ?: 85,
            isCharging = partnerLoc?.isCharging ?: false,
            speedKmh = partnerLoc?.speed ?: 0.0f,
            isPrivacyActive = isPrivacy,
            lastUpdateText = if (isStale) "STALE ($lastUpdated)" else "Lúc $lastUpdated",
            onTap = onRefreshLocation
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Zenly Bump Action
        Button(
            onClick = onSendZenlyBump,
            modifier = Modifier.fillMaxWidth().testTag("zenly_bump_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpaceCardElevated,
                contentColor = NeonAmber
            )
        ) {
            Text(text = "⚡", fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Chạm tay Zenly Bump (Gửi rung chuông • ${uiState.lastBumpCount})",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Coordinates & Telemetry Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BentoStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.GpsFixed,
                iconTint = NeonCyan,
                title = "ĐỘ CHÍNH XÁC",
                value = "${partnerLoc?.accuracy?.toInt() ?: 12} mét",
                subtitle = "Vệ tinh GPS",
                testTag = "accuracy_card"
            )

            BentoStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                iconTint = NeonViolet,
                title = "TỐC ĐỘ DI CHUYỂN",
                value = "${partnerLoc?.speed?.toInt() ?: 0} km/h",
                subtitle = if ((partnerLoc?.speed ?: 0f) > 5f) "Đang di chuyển" else "Đứng yên",
                testTag = "speed_card"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Coordinates details card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TỌA ĐỘ BẢN ĐỒ VỆ TINH",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isPrivacy) "BẢO VỆ RIÊNG TƯ" else "REALTIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPrivacy) NeonAmber else NeonEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isPrivacy) {
                    Text(
                        text = "Vĩ độ & Kinh độ bị ẩn theo yêu cầu khung giờ riêng tư.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Vĩ độ (Lat):", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "%.5f".format(partnerLoc?.latitude ?: 21.0285),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(text = "Kinh độ (Lng):", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "%.5f".format(partnerLoc?.longitude ?: 105.8542),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isChild) {
                    Button(
                        onClick = onRequestPrivacyClick,
                        modifier = Modifier.fillMaxWidth().testTag("req_privacy_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpaceCardElevated,
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xin cấp Khung giờ riêng tư hôm nay")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safe Zone & Background App Telemetry Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VÙNG AN TOÀN & ỨNG DỤNG ĐANG MỞ",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonEmerald,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (uiState.isBackgroundServiceRunning) "🟢 Chạy ngầm" else "⚪ Tắt ngầm",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.isBackgroundServiceRunning) NeonEmerald else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceCardElevated)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(text = "Vùng Geofence", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = partnerLoc?.currentSafeZone?.ifEmpty { "Trường học" } ?: "Trường học",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceCardElevated)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(text = "Ứng dụng ngầm", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${partnerLoc?.topActiveApp?.ifEmpty { "YouTube" } ?: "YouTube"} (${partnerLoc?.topActiveAppMinutes ?: 45}m)",
                                style = MaterialTheme.typography.titleMedium,
                                color = NeonAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
