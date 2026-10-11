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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    onRoleSelected: (UserRole) -> Unit,
    onOpenUsageSettings: () -> Unit,
    onUnpair: () -> Unit,
    onSignOut: () -> Unit,
    onToggleIosPreview: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showUnpairDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val userProfile = uiState.userProfile
    val isPaired = uiState.activePairing != null && uiState.activePairing.status == PairingStatus.PAIRED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceDarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = "CÀI ĐẶT & KẾT NỐI",
            style = MaterialTheme.typography.displayMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Quản lý liên kết gia đình & Quyền riêng tư",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                    Column {
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Role Selector if not paired
                Text(
                    text = "VAI TRÒ HIỆN TẠI:",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (userProfile?.role == UserRole.PARENT) NeonViolet.copy(alpha = 0.25f) else SpaceCardElevated)
                            .border(1.dp, if (userProfile?.role == UserRole.PARENT) NeonViolet else SpaceCardBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isPaired) { onRoleSelected(UserRole.PARENT) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Phụ huynh",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (userProfile?.role == UserRole.PARENT) TextPrimary else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (userProfile?.role == UserRole.CHILD) NeonCyan.copy(alpha = 0.25f) else SpaceCardElevated)
                            .border(1.dp, if (userProfile?.role == UserRole.CHILD) NeonCyan else SpaceCardBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isPaired) { onRoleSelected(UserRole.CHILD) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Học sinh",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (userProfile?.role == UserRole.CHILD) TextPrimary else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                    Text(
                        text = "Đang kết nối: ${p.parentName} ⟷ ${p.childName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mã kết nối: ${p.studentCode} • Dữ liệu mã hóa trên Firestore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Usage Access (Dữ liệu sử dụng)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.hasUsagePermission) "Đã cấp quyền chuẩn" else "Chưa được cấp quyền",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.hasUsagePermission) NeonEmerald else NeonAmber
                        )
                    }

                    Button(
                        onClick = onOpenUsageSettings,
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

        Spacer(modifier = Modifier.height(16.dp))

        // iOS & Multiplatform Architecture Card
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🍏", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KIẾN TRÚC ĐA NỀN TẢNG (IOS & ANDROID)",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Gia Đình Việt thiết kế theo chuẩn Clean Architecture bảo mật cao, hỗ trợ đồng bộ thời gian thực đa nền tảng.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onToggleIosPreview,
                    modifier = Modifier.fillMaxWidth().testTag("toggle_ios_preview_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isIosPreviewMode) NeonEmerald else SpaceCardElevated,
                        contentColor = if (uiState.isIosPreviewMode) Color.Black else NeonCyan
                    )
                ) {
                    Text(
                        text = if (uiState.isIosPreviewMode) "Đang bật Giao diện iOS (Tắt ✕)" else "Bật Chế độ xem trước Giao diện iOS 🍏",
                        fontWeight = FontWeight.Bold
                    )
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
