package com.example.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
    var selectedTab by remember { mutableIntStateOf(0) }
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SpaceDarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = SpaceCardSurface,
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
                    onSwitchDemoRole = { role -> viewModel.switchDemoRole(role) },
                    onSendZenlyBump = { viewModel.sendZenlyBump(context) }
                )
                1 -> LocationScreen(
                    uiState = uiState,
                    onRefreshLocation = { viewModel.broadcastSelfTelemetry() },
                    onRequestPrivacyClick = { showPrivacyDialog = true },
                    onSendZenlyBump = { viewModel.sendZenlyBump(context) }
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
                    onSendMessage = { text -> viewModel.sendMessage(text) },
                    onToggleStar = { id, star -> viewModel.toggleMessageStar(id, star) },
                    onSelectEmotion = { emotion -> viewModel.setSelectedEmotion(emotion) }
                )
                4 -> SettingsScreen(
                    uiState = uiState,
                    onRoleSelected = { role -> viewModel.selectRole(role) },
                    onOpenUsageSettings = onOpenUsageSettings,
                    onUnpair = { viewModel.unpairConnection() },
                    onSignOut = { viewModel.signOut() },
                    onToggleIosPreview = { viewModel.toggleIosPreviewMode() }
                )
            }
        }
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
