package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PairingStatus
import com.example.data.model.UserRole
import com.example.ui.components.DraggableFloatingSosButton
import com.example.ui.components.ExtraTimeDialog
import com.example.ui.components.PrivacyDialog
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SpaceCardSurface
import com.example.ui.theme.SpaceDarkBackground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.VesperaUiState
import com.example.ui.viewmodel.VesperaViewModel

@Composable
fun MainAppScreen(
    viewModel: VesperaViewModel,
    uiState: VesperaUiState,
    onOpenUsageSettings: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isChild = uiState.userProfile?.role == UserRole.CHILD
    var selectedTab by remember { mutableIntStateOf(0) }
    var studentShowChat by remember { mutableStateOf(false) }
    var showExtraTimeDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isSosAlertActive) {
            com.example.ui.components.SosEmergencyWarningDialog(
                message = uiState.activeSosMessage,
                channel = uiState.currentSosChannel,
                onDismiss = { viewModel.dismissSosAlert() }
            )
        }

        // 1. DEDICATED STUDENT INTERFACE
        // Displays purely the unique student code for parents to connect, runs background telemetry, and status
        if (isChild) {
            // Always show the full navigation bar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.navigationBarsPadding().testTag("bottom_navigation_bar")
                    ) {
                        val items = listOf(
                            Triple("Tổng quan", Icons.Default.Home, 0),
                            Triple("Vị trí", Icons.Default.LocationOn, 1),
                            Triple("Chat", Icons.Default.Chat, 3)
                        )

                        items.forEach { (label, icon, index) ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text(text = label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = NeonViolet.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier.testTag("nav_tab_$index")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val isPaired = uiState.activePairing != null && uiState.activePairing.status == PairingStatus.PAIRED
                    
                    when (selectedTab) {
                        0 -> StudentMainScreen(
                            uiState = uiState,
                            onAcceptPairing = { req -> viewModel.acceptPairing(req) },
                            onRejectPairing = { id -> viewModel.rejectPairing(id) },
                            onSendZenlyBump = { viewModel.sendZenlyBump(context) },
                            onOpenChat = { selectedTab = 3 },
                            onRequestExtraTime = { showExtraTimeDialog = true },
                            onSignOut = { viewModel.signOut() },
                            onRegenerateCode = { viewModel.regenerateStudentCode() }
                        )
                        1 -> if (isPaired) {
                            LocationScreen(
                                uiState = uiState,
                                onRefreshLocation = { viewModel.broadcastSelfTelemetry() },
                                onRequestPrivacyClick = { showPrivacyDialog = true },
                                onSendZenlyBump = { viewModel.sendZenlyBump(context) },
                                onToggleLocationHistory = { enabled -> viewModel.toggleLocationHistory(enabled) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Vui lòng ghép nối với phụ huynh để xem vị trí.")
                            }
                        }
                        3 -> if (isPaired) {
                            ChatScreen(
                                uiState = uiState,
                                onSendMessage = { text, url, type -> viewModel.sendMessage(text, url, type) },
                                onToggleStar = { id, star -> viewModel.toggleMessageStar(id, star) },
                                onSelectEmotion = { emotion -> viewModel.setSelectedEmotion(emotion) },
                                onConnectMeshPeer = { peerId -> viewModel.connectMeshPeer(peerId) },
                                onSendMeshMessage = { peerId, text -> viewModel.sendMeshMessage(peerId, text) }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Vui lòng ghép nối với phụ huynh để trò chuyện.")
                            }
                        }
                    }
                }
            }
        } else {
        // 2. PARENT MONITORING INTERFACE (Full Multi-Tab Dashboard)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.navigationBarsPadding().testTag("bottom_navigation_bar")
                ) {
                    val items = listOf(
                        Triple("Tổng quan", Icons.Default.Home, 0),
                        Triple("Vị trí", Icons.Default.LocationOn, 1),
                        Triple("Giờ dùng", Icons.Default.Schedule, 2),
                        Triple("Chat", Icons.Default.Chat, 3),
                        Triple("Cài đặt", Icons.Default.Settings, 4)
                    )

                    items.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(text = label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = NeonViolet.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag("nav_tab_$index")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        uiState = uiState,
                        onSendPairingRequest = { code -> viewModel.sendPairingRequest(code) },
                        onAcceptPairing = { req -> viewModel.acceptPairing(req) },
                        onRejectPairing = { id -> viewModel.rejectPairing(id) },
                        onApproveExtraTime = { reqId, mins -> viewModel.respondToExtraTime(reqId, true, mins) },
                        onRejectExtraTime = { reqId -> viewModel.respondToExtraTime(reqId, false, 0) },
                        onApprovePrivacy = { reqId, childId -> viewModel.respondToPrivacyRequest(reqId, childId, true) },
                        onRejectPrivacy = { reqId -> viewModel.respondToPrivacyRequest(reqId, "", false) },
                        onRequestExtraTimeClick = { showExtraTimeDialog = true },
                        onRequestPrivacyClick = { showPrivacyDialog = true },
                        onNavigateToTab = { index -> selectedTab = index },
                        onSelectEmotion = { emotion -> viewModel.setSelectedEmotion(emotion) },
                        onSendZenlyBump = { viewModel.sendZenlyBump(context) }
                    )
                    1 -> LocationScreen(
                        uiState = uiState,
                        onRefreshLocation = { viewModel.broadcastSelfTelemetry() },
                        onRequestPrivacyClick = { showPrivacyDialog = true },
                        onSendZenlyBump = { viewModel.sendZenlyBump(context) },
                        onToggleLocationHistory = { enabled -> viewModel.toggleLocationHistory(enabled) }
                    )
                    2 -> ScreenTimeScreen(
                        uiState = uiState,
                        onOpenUsageSettings = onOpenUsageSettings,
                        onRequestExtraTimeClick = { showExtraTimeDialog = true },
                        onEditScheduleClick = { /* Can toggle settings or trigger modal */ },
                        onToggleBackgroundService = { viewModel.toggleBackgroundService(context) },
                        onStartFocusSession = { viewModel.startFocusSession(25) },
                        onStopFocusSession = { viewModel.stopFocusSession() }
                    )
                    3 -> ChatScreen(
                        uiState = uiState,
                        onSendMessage = { text, url, type -> viewModel.sendMessage(text, url, type) },
                        onToggleStar = { id, star -> viewModel.toggleMessageStar(id, star) },
                        onSelectEmotion = { emotion -> viewModel.setSelectedEmotion(emotion) },
                        onConnectMeshPeer = { peerId -> viewModel.connectMeshPeer(peerId) },
                        onSendMeshMessage = { peerId, text -> viewModel.sendMeshMessage(peerId, text) }
                    )
                    4 -> SettingsScreen(
                        uiState = uiState,
                        onOpenUsageSettings = onOpenUsageSettings,
                        onUnpair = { viewModel.unpairConnection() },
                        onSignOut = { viewModel.signOut() },
                        onToggleDarkMode = { enabled -> viewModel.setDarkMode(enabled) },
                        onColorThemeSelected = { theme -> viewModel.setColorTheme(theme) },
                        onToggleIosPreview = { viewModel.toggleIosPreviewMode() },
                        onSaveEmergencyPhone = { phone -> viewModel.saveEmergencyPhoneNumber(phone) },
                        onSaveSosSchedule = { restricted, startH, endH -> viewModel.saveSosSchedule(restricted, startH, endH) },
                        onUpdateProfile = { name, photo -> viewModel.updateUserProfile(name, photo) }
                    )
                }
            }
        }
    }

    // NÚT SOS NỔI DI CHUYỂN ĐƯỢC TRÊN MỌI MÀN HÌNH (DRAGGABLE FLOATING SOS)
    DraggableFloatingSosButton(
            isOnline = uiState.isOnline,
            currentChannel = uiState.currentSosChannel,
            isSosAllowedNow = viewModel.isSosAllowedNow(),
            onSendSosToFamily = { viewModel.sendSmartSosToFamily() },
            onSendSosToRescue = { hotline -> viewModel.sendSmartSosToRescue(hotline) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = if (isChild) 36.dp else 98.dp)
        )
    }

    if (showExtraTimeDialog) {
        ExtraTimeDialog(
            onDismiss = { showExtraTimeDialog = false },
            onSubmit = { mins, reason ->
                showExtraTimeDialog = false
                viewModel.requestExtraTime(mins, reason)
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyDialog(
            onDismiss = { showPrivacyDialog = false },
            onSubmit = { startH, endH, reason ->
                showPrivacyDialog = false
                viewModel.requestPrivacyHours(startH, endH, reason)
            }
        )
    }
}
