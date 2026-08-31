package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.service.OverlayService
import com.example.ui.ScreenMaskApp

class MainActivity : ComponentActivity() {

    private var hasPermissionState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permitted = hasOverlayPermission()
        hasPermissionState = permitted

        if (permitted) {
            startFloatingOverlayService()
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF0284C7),
                    onPrimaryContainer = Color.White,
                    surface = if (hasPermissionState) Color.White else Color(0xFF0F172A),
                    onSurface = if (hasPermissionState) Color.Black else Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (hasPermissionState) Color.White else Color(0xFF0F172A)
                ) {
                    ScreenMaskApp(
                        hasPermission = hasPermissionState,
                        onGrantPermission = {
                            // User opened settings to grant permission
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val permitted = hasOverlayPermission()
        hasPermissionState = permitted
        if (permitted) {
            startFloatingOverlayService()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val permitted = hasOverlayPermission()
        hasPermissionState = permitted
        if (permitted) {
            startFloatingOverlayService()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun startFloatingOverlayService() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_EDIT_MODE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
