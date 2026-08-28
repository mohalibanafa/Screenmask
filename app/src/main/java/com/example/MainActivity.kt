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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.service.OverlayService
import com.example.ui.ScreenMaskApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if overlay permission is already granted
        if (hasOverlayPermission()) {
            launchFloatingOverlayAndFinish()
            return
        }

        enableEdgeToEdge()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color.Black,
                    primaryContainer = Color(0xFF0284C7),
                    onPrimaryContainer = Color.White,
                    surface = Color(0xFF0F172A),
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    ScreenMaskApp(
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
        if (hasOverlayPermission()) {
            launchFloatingOverlayAndFinish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (hasOverlayPermission()) {
            launchFloatingOverlayAndFinish()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun launchFloatingOverlayAndFinish() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_EDIT_MODE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        finish()
    }
}
