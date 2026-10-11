package com.example.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TagFaces
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.BentoCard
import com.example.ui.components.BentoProgressBar
import com.example.ui.components.BentoStatCard
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

@Composable
fun ScreenTimeScreen(
    uiState: VesperaUiState,
    onOpenUsageSettings: () -> Unit,
    onRequestExtraTimeClick: () -> Unit,
    onEditScheduleClick: () -> Unit,
    onToggleBackgroundService: () -> Unit,
    onStartFocusSession: () -> Unit = {},
    onStopFocusSession: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isChild = uiState.userProfile?.role == UserRole.CHILD
    val sched = uiState.usageSchedule
    val target = sched?.dailyTargetMinutes ?: 120
    val used = sched?.usedMinutes ?: 45
    val remaining = (target - used).coerceAtLeast(0)

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

        // Header
        Text(
            text = "QUẢN LÝ THỜI GIAN",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Lịch sinh hoạt gia đình & Thống kê ứng dụng",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Progress Overview Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIẾN ĐỘ SỬ DỤNG HÔM NAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonAmber,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Còn $remaining phút",
                        style = MaterialTheme.typography.labelMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                BentoProgressBar(
                    usedMinutes = used,
                    targetMinutes = target
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isChild) {
                        Button(
                            onClick = onRequestExtraTimeClick,
                            modifier = Modifier.fillMaxWidth().testTag("req_extra_time_screentime_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonAmber,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xin thêm thời gian sử dụng", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onEditScheduleClick,
                            modifier = Modifier.fillMaxWidth().testTag("edit_sched_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SpaceCardElevated,
                                contentColor = NeonViolet
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tùy chỉnh lịch & Giới hạn mục tiêu")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pomodoro Focus Study Mode Card
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (uiState.isFocusSessionActive) NeonCyan else SpaceCardBorder
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHẾ ĐỘ HỌC TẬP TẬP TRUNG (POMODORO)",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.isFocusSessionActive) NeonCyan.copy(alpha = 0.2f) else SpaceCardElevated)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (uiState.isFocusSessionActive) "Đang học 📚" else "Chờ bắt đầu",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isFocusSessionActive) NeonCyan else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (uiState.isFocusSessionActive)
                        "Phiên tập trung 25 phút Pomodoro đang diễn ra. Điện thoại sẽ nhắc nhở nếu thoát ra để mở mạng xã hội hoặc game."
                    else
                        "Kích hoạt phiên học tập 25 phút Pomodoro giúp con rèn luyện thói quen tự giác, không bị xao nhãng bởi thông báo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (uiState.isFocusSessionActive) onStopFocusSession() else onStartFocusSession()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("toggle_focus_mode_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isFocusSessionActive) SpaceCardElevated else NeonCyan,
                        contentColor = if (uiState.isFocusSessionActive) NeonRose else Color.Black
                    )
                ) {
                    Text(
                        text = if (uiState.isFocusSessionActive) "Kết thúc phiên học tập" else "Bắt đầu 25 phút Học tập Tập trung 🎯",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly Trend Bento Bar Chart
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BIỂU ĐỒ SỬ DỤNG TRONG TUẦN",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonViolet,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Trung bình 105 ph/ngày",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxMins = 160f
                    uiState.weeklyUsage.forEach { (day, mins) ->
                        val ratio = (mins / maxMins).coerceIn(0.15f, 1f)
                        val isToday = day == "CN"
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "$mins",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) NeonCyan else TextMuted,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height((70 * ratio).dp)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (isToday) NeonCyan else NeonViolet.copy(alpha = 0.5f))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) TextPrimary else TextSecondary,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UsageStats Real Breakdown Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PHÂN LOẠI ỨNG DỤNG (USAGE ACCESS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (!uiState.hasUsagePermission) {
                        Text(
                            text = "Chưa cấp quyền ⚠️",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!uiState.hasUsagePermission) {
                    // Explanatory card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceCardElevated)
                            .border(1.dp, SpaceCardBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Yêu cầu quyền UsageStats từ Android",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Để thống kê chính xác thời gian thực dùng cho Mạng xã hội, Học tập và Trò chơi, bạn cần bật quyền 'Truy cập dữ liệu sử dụng' trong Cài đặt hệ thống.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenUsageSettings,
                                modifier = Modifier.fillMaxWidth().testTag("open_usage_settings_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mở Cài đặt Android để cấp quyền", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Real Categories list
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.usageCategories.forEach { cat ->
                            CategoryUsageItem(
                                title = cat.categoryName,
                                minutes = cat.minutes,
                                percentage = cat.percentage
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Background Foreground Service Status & Control Card
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (uiState.isBackgroundServiceRunning) NeonEmerald else SpaceCardBorder
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (uiState.isBackgroundServiceRunning) "🟢" else "⚪", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CHẠY NGẦM LIÊN TỤC (GPS & GIỜ DÙNG)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isBackgroundServiceRunning) NeonEmerald else TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.isBackgroundServiceRunning) NeonEmerald.copy(alpha = 0.2f) else SpaceCardElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (uiState.isBackgroundServiceRunning) "Đang chạy ngầm" else "Đang tắt",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isBackgroundServiceRunning) NeonEmerald else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Khi bật, Android Foreground Service sẽ tiếp tục cập nhật vị trí GPS vệ tinh và thống kê ứng dụng liên tục ngay cả khi tắt màn hình hoặc khóa máy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onToggleBackgroundService,
                    modifier = Modifier.fillMaxWidth().testTag("toggle_bg_service_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isBackgroundServiceRunning) SpaceCardElevated else NeonEmerald,
                        contentColor = if (uiState.isBackgroundServiceRunning) NeonRose else Color.Black
                    )
                ) {
                    Text(
                        text = if (uiState.isBackgroundServiceRunning) "Dừng dịch vụ chạy ngầm" else "Kích hoạt Chạy Ngầm Liên Tục 🚀",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed Per-App Usage Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHI TIẾT TỪNG ỨNG DỤNG ĐÃ DÙNG (${uiState.detailedAppUsage.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Hôm nay",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.detailedAppUsage.isEmpty()) {
                    Text(
                        text = "Chưa ghi nhận ứng dụng nào sử dụng trên 1 phút.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.detailedAppUsage.forEach { app ->
                            DetailedAppUsageRow(app = app)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule & Android Safe Policy Info Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CHẾ ĐỘ NHẮC NHỞ & MINH BẠCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonViolet,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vespera tuân thủ chính sách Android an toàn: Ứng dụng hoạt động theo cơ chế thông báo nhắc nhở và cảnh báo thống nhất giữa gia đình, không can thiệp khóa cưỡng chế hay lạm dụng Accessibility Service.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Giờ bắt đầu cho phép: ${sched?.allowedStartHour ?: 7}:00 sáng\n• Giờ kết thúc: ${sched?.allowedEndHour ?: 21}:30 tối\n• Ngày áp dụng: ${sched?.daysOfWeek ?: "Cả tuần"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CategoryUsageItem(
    title: String,
    minutes: Int,
    percentage: Float
) {
    val barColor = when (title) {
        "Mạng xã hội" -> NeonViolet
        "Học tập" -> NeonCyan
        "Trò chơi" -> NeonRose
        else -> NeonEmerald
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(text = "$minutes phút", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpaceCardElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage.coerceIn(0.05f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun DetailedAppUsageRow(app: com.example.data.model.DetailedAppUsage) {
    val categoryColor = when (app.category) {
        "Mạng xã hội" -> NeonViolet
        "Học tập" -> NeonCyan
        "Trò chơi" -> NeonRose
        else -> NeonEmerald
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SpaceCardElevated)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(categoryColor.copy(alpha = 0.2f))
                    .border(1.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.take(1).uppercase(),
                    color = categoryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(categoryColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = app.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${app.minutesUsed} phút",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hôm nay",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

