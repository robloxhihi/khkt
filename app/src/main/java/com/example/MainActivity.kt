package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserRole
import com.example.data.repository.VesperaRepository
import com.example.service.TelemetryAndUsageHelper
import com.example.ui.screens.MainAppScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SpaceDarkBackground
import com.example.ui.viewmodel.VesperaViewModel

class MainActivity : ComponentActivity() {

    private lateinit var telemetryHelper: TelemetryAndUsageHelper
    private lateinit var repository: VesperaRepository

    private val viewModel: VesperaViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VesperaViewModel(repository, telemetryHelper, applicationContext) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        telemetryHelper = TelemetryAndUsageHelper(applicationContext)
        repository = VesperaRepository(applicationContext)

        if (intent?.getBooleanExtra("TRIGGER_SOS", false) == true || intent?.action == "ACTION_SOS") {
            viewModel.sendSosAlert()
            Toast.makeText(this, "🚨 Đã tự động phát tín hiệu SOS khẩn cấp từ màn hình khóa!", Toast.LENGTH_LONG).show()
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MyApplicationTheme(
                darkTheme = uiState.isDarkMode,
                colorTheme = uiState.colorTheme
            ) {
                val context = LocalContext.current
                var selectedRoleForSignUp by remember { mutableStateOf(UserRole.PARENT) }

                // Runtime permissions launcher for GPS, SMS, Bluetooth Mesh & Notifications
                val permissionsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (locationGranted) {
                        // Automatically start real background telemetry & usage service
                        com.example.service.VesperaTelemetryService.start(applicationContext)
                        viewModel.broadcastSelfTelemetry()
                    }
                    if (!telemetryHelper.hasUsageStatsPermission()) {
                        Toast.makeText(
                            this@MainActivity,
                            "Gia Đình Việt: Vui lòng cấp quyền 'Truy cập dữ liệu sử dụng' để xem thời gian ứng dụng chạy ngầm",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    val missingPerms = com.example.service.sos.PermissionHandler.getMissingPermissions(applicationContext)
                    if (missingPerms.isNotEmpty()) {
                        permissionsLauncher.launch(missingPerms)
                    }

                    // Auto-start telemetry service if permission is already granted
                    if (telemetryHelper.hasLocationPermission()) {
                        com.example.service.VesperaTelemetryService.start(applicationContext)
                    }
                }

                if (!uiState.isAuthenticated) {
                    SignInScreen(
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onSignInClick = {
                            viewModel.signInWithGoogle(this@MainActivity, selectedRoleForSignUp)
                        },
                        onSignIn = { username, pass ->
                            viewModel.signInWithUsername(username, pass, selectedRoleForSignUp)
                        },
                        onSignUp = { username, pass ->
                            viewModel.signUpWithUsername(username, pass, selectedRoleForSignUp)
                        },
                        selectedRole = selectedRoleForSignUp,
                        onRoleSelected = { role ->
                            selectedRoleForSignUp = role
                        }
                    )
                } else {
                    MainAppScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onOpenUsageSettings = {
                            try {
                                startActivity(telemetryHelper.getUsageAccessSettingsIntent())
                            } catch (e: Exception) {
                                Toast.makeText(context, "Không thể mở cài đặt: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("TRIGGER_SOS", false) == true || intent.action == "ACTION_SOS") {
            viewModel.sendSosAlert()
            Toast.makeText(this, "🚨 Đã tự động phát tín hiệu SOS khẩn cấp từ màn hình khóa!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::telemetryHelper.isInitialized) {
            viewModel.refreshUsageStats()
            viewModel.broadcastSelfTelemetry()
        }
    }
}
