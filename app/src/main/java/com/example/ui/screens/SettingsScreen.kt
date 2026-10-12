package com.example.ui.screens

import com.example.service.TelemetryAndUsageHelper
import com.example.service.sos.PermissionHandler


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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SettingsScreen(
    uiState: VesperaUiState,
    onOpenUsageSettings: () -> Unit,
    onUnpair: () -> Unit,
    onSignOut: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onColorThemeSelected: (String) -> Unit,
    onToggleIosPreview: () -> Unit = {},
    onSaveEmergencyPhone: (String) -> Unit = { _ -> },
    onSaveSosSchedule: (Boolean, Int, Int) -> Unit = { _, _, _ -> },
    onUpdateProfile: (String, String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    var showUnpairDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var newDisplayName by remember(uiState.userProfile?.displayName) { mutableStateOf(uiState.userProfile?.displayName ?: "") }

    var emergencyPhoneInput by remember(uiState.emergencyPhoneNumber) { mutableStateOf(uiState.emergencyPhoneNumber) }
    var isSosRestricted by remember(uiState.isSosRestricted) { mutableStateOf(uiState.isSosRestricted) }
    var sosStartHour by remember(uiState.sosStartHour) { mutableStateOf(uiState.sosStartHour.toString()) }
    var sosEndHour by remember(uiState.sosEndHour) { mutableStateOf(uiState.sosEndHour.toString()) }

    val userProfile = uiState.userProfile
    val isPaired = uiState.activePairing != null && uiState.activePairing.status == PairingStatus.PAIRED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "CÀI ĐẶT & GIAO DIỆN",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tùy chỉnh giao diện sáng tối, màu sắc & liên kết gia đình",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        val telemetryHelper = TelemetryAndUsageHelper(context)
        val missingPerms = PermissionHandler.getMissingPermissions(context)
        val hasUsage = telemetryHelper.hasUsageStatsPermission()
        val allPermissionsGranted = missingPerms.isEmpty() && hasUsage


        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val denied = results.filterValues { !it }.keys.toList()
            if (denied.isEmpty()) {
                android.widget.Toast.makeText(context, "Đã cấp tất cả quyền hệ thống thành công!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Còn ${denied.size} quyền chưa được cấp.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TRẠNG THÁI QUYỀN HỆ THỐNG",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (allPermissionsGranted) NeonEmerald else NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (allPermissionsGranted) "Tất cả quyền quan trọng đã sẵn sàng" else "Có quyền hệ thống đang thiếu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = if (allPermissionsGranted) Icons.Default.Verified else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (allPermissionsGranted) NeonEmerald else NeonAmber,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val locationOk = com.example.service.sos.PermissionHandler.hasPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                val smsOk = com.example.service.sos.PermissionHandler.hasPermission(context, android.Manifest.permission.SEND_SMS)
                val notifOk = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU || com.example.service.sos.PermissionHandler.hasPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)

                PermissionStatusRow("Quyền Vị trí GPS (Theo dõi an toàn)", locationOk)
                PermissionStatusRow("Quyền Gửi tin nhắn SMS Cứu nạn", smsOk)
                PermissionStatusRow("Quyền Thông báo đẩy (Alerts)", notifOk)
                PermissionStatusRow("Quyền Truy cập dữ liệu sử dụng", hasUsage)

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!allPermissionsGranted) {
                        Button(
                            onClick = {
                                val missing = com.example.service.sos.PermissionHandler.getMissingPermissions(context)
                                if (missing.isNotEmpty()) {
                                    permissionLauncher.launch(missing)
                                } else if (!hasUsage) {
                                    onOpenUsageSettings()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("grant_permissions_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                        ) {
                            Text("Ấn 1 nút cấp ngay quyền thiếu", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                android.widget.Toast.makeText(context, "Hệ thống đã được cấp đầy đủ quyền!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                            enabled = false
                        ) {
                            Text("Đã cấp đầy đủ quyền hệ thống", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme & Color Customization Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "GIAO DIỆN SÁNG / TỐI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isDarkMode) "Chế độ tối (Dark Mode)" else "Chế độ sáng (Light Mode)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.material3.Switch(
                        checked = uiState.isDarkMode,
                        onCheckedChange = onToggleDarkMode
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile & Role Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(NeonViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile?.displayName?.take(1)?.uppercase() ?: "U",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.displayName ?: "Người dùng Google",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    TextButton(onClick = { showEditProfileDialog = true }) {
                        Text("Sửa")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))


                if (userProfile?.role == UserRole.CHILD && userProfile.studentCode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Mã nhận diện học sinh: ${userProfile.studentCode}",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Đổi tên tài khoản") },
            text = {
                OutlinedTextField(
                    value = newDisplayName,
                    onValueChange = { newDisplayName = it },
                    label = { Text("Tên hiển thị mới") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEditProfileDialog = false
                        onUpdateProfile(newDisplayName, userProfile?.photoUrl ?: "")
                    }
                ) {
                    Text("Lưu", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = SpaceDarkBackground
        )
    }

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Management Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "QUẢN LÝ LIÊN KẾT GIA ĐÌNH",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonEmerald,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isPaired) {
                    val p = uiState.activePairing!!
                    val isParent = userProfile?.role == UserRole.PARENT
                    val partnerTitle = if (isParent) "Đang kết nối với Học sinh: ${p.childName}" else "Đang kết nối với Phụ huynh: ${p.parentName}"
                    Text(
                        text = partnerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mã kết nối học sinh: ${p.studentCode} • Trạng thái: Đã liên kết an toàn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonEmerald
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showUnpairDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("unpair_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpaceCardElevated,
                            contentColor = NeonRose
                        )
                    ) {
                        Icon(imageVector = Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hủy liên kết thiết bị", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Hiện tại chưa có liên kết gia đình nào đang hoạt động.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CẤU HÌNH CỨU HỘ KHẨN CẤP & CÀI ĐẶT NÚT SOS
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NeonRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CỨU HỘ KHẨN CẤP & CÀI ĐẶT NÚT SOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonRose,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Số điện thoại nhận tin SMS khẩn cấp:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Được dùng để gửi SMS kèm link Google Maps khi mất Internet & ngoài bán kính Mesh 50m.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = emergencyPhoneInput,
                        onValueChange = { emergencyPhoneInput = it },
                        placeholder = { Text("Ví dụ: 0912345678", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSaveEmergencyPhone(emergencyPhoneInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRose)
                    ) {
                        Text("Lưu", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thông tin SOS (Luôn hoạt động 24/7)
                Text(
                    text = "Nút SOS luôn hoạt động 24/7 để đảm bảo an toàn tuyệt đối.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonEmerald,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System Permissions Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "QUYỀN HỆ THỐNG ANDROID",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = android.net.Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quyền ứng dụng & Vị trí hệ thống",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Nhấn để quản lý và cấp phép quyền vị trí & thông báo.",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonAmber
                        )
                    }

                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = android.net.Uri.fromParts("package", context.packageName, null)
                            intent.data = uri
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpaceCardElevated,
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cài đặt")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account Actions
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "TÀI KHOẢN & BẢO MẬT",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonViolet,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("sign_out_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpaceCardElevated,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng xuất tài khoản Google")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("delete_account_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpaceCardElevated,
                        contentColor = NeonRose
                    )
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yêu cầu xóa tài khoản & Dữ liệu")
                }
            }
        }


        Spacer(modifier = Modifier.height(32.dp))
    }

    // Unpair Dialog
    if (showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairDialog = false },
            title = { Text("Xác nhận hủy liên kết?") },
            text = {
                Text("Khi hủy liên kết, toàn bộ quyền chia sẻ vị trí realtime, lịch giờ và tin nhắn sẽ lập tức bị thu hồi trên cả hai máy.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnpairDialog = false
                        onUnpair()
                    }
                ) {
                    Text("Hủy liên kết ngay", color = NeonRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairDialog = false }) {
                    Text("Quay lại", color = TextSecondary)
                }
            },
            containerColor = SpaceDarkBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Yêu cầu xóa tài khoản") },
            text = {
                Text("Mọi thông tin định danh, vị trí lịch sử và tin nhắn gia đình của bạn sẽ được xóa vĩnh viễn theo chính sách quyền riêng tư.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        onSignOut()
                    }
                ) {
                    Text("Xóa tài khoản", color = NeonRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = SpaceDarkBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
fun PermissionStatusRow(title: String, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Text(
            text = if (isGranted) "✓ Đã cấp" else "✗ Chưa cấp",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGranted) NeonEmerald else NeonRose,
            fontWeight = FontWeight.Bold
        )
    }
}

