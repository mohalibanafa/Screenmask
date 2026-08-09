# Screen Mask | قناع الشاشة 🛡️📱

Lightweight, open-source, non-root Android application designed to cover damaged, flickering, or light-bleeding screen areas with independent pure black screen overlay masks (`TYPE_APPLICATION_OVERLAY`).

---

## 🌟 Key Features

- **No Root Required**: Built purely using Android standard `WindowManager` and `TYPE_APPLICATION_OVERLAY`.
- **Pure Black Masks (RGB 0,0,0)**: Default 100% opaque black rectangular masks that cover dead pixels, screen cracks, or bright spots.
- **Unlimited Independent Masks**: Create, drag, resize, lock, and hide multiple masks simultaneously.
- **Pass-Through Touch (Block Mode)**: In **Block Mode**, borders and control handles disappear completely. Masks use `FLAG_NOT_TOUCHABLE` & `FLAG_NOT_FOCUSABLE`, allowing all touch inputs to pass through directly to underlying applications.
- **Interactive Edit Mode**: Position masks easily with corner resize handles and drag controls.
- **Floating Controller Widget**: Draggable quick control panel for toggling modes, adding masks, or deleting selected masks outside the app.
- **Persistent Room Database**: Mask coordinates are stored using screen ratios (0.0 .. 1.0) to preserve exact positions across device reboots, screen orientation changes, and status bar/navigation bar inset shifts.
- **Foreground Service & QS Tile**: Runs reliably in the background with a persistent control notification and a Quick Settings Tile.

---

## 🛠️ Architecture & Project Structure

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose + Custom Canvas Overlay Views
- **Persistence**: Room Database (`MaskEntity`, `MaskDao`, `AppDatabase`) + SharedPreferences (`AppSettingsRepository`)
- **Service**: `OverlayService` (Foreground Service managing WindowManager overlay layers)
- **Quick Settings Tile**: `ScreenMaskTileService` (`TileService`)

```
app/src/main/java/com/example/
├── MainActivity.kt                  # Main entry activity
├── data/
│   ├── MaskEntity.kt                # Room entity storing ratios, color, lock state
│   ├── MaskDao.kt                   # Database access object
│   ├── AppDatabase.kt               # Room database instance
│   ├── MaskRepository.kt            # Repository pattern abstraction
│   └── AppSettingsRepository.kt     # Preferences management
├── service/
│   ├── OverlayService.kt            # Foreground service managing WindowManager
│   ├── OverlayMaskView.kt           # Custom view for interactive masks
│   ├── FloatingControlView.kt       # Floating control bar widget
│   └── ScreenMaskTileService.kt     # Quick Settings tile service
├── receiver/
│   └── BootReceiver.kt              # Restores service on boot
├── ui/
│   └── ScreenMaskApp.kt             # Jetpack Compose management interface
└── viewmodel/
    └── MainViewModel.kt             # State management
```

---

## 🚀 Building & Installation

### Prerequisites
- **Android Studio**: Ladybug / Jellyfish or newer
- **JDK**: JDK 17
- **Gradle**: 8.x+ (configured via project Gradle)
- **Minimum SDK**: API 24 (Android 7.0+, full overlay support on API 26+)
- **Target SDK**: API 36 (Android 15+)

### Build Commands
To build the debug APK:
```bash
gradle :app:assembleDebug
```
The resulting APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Permissions & Safety

- **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**: Required to draw floating mask layers over the Android screen. Requested at runtime.
- **Foreground Service (`FOREGROUND_SERVICE`)**: Ensures masks stay active reliably without being killed by Android memory management.
- **No Accessibility / No Camera / No MediaProjection**: The app does **NOT** read screen content, record video, or intercept user keystrokes.

---

## 📄 License
This project is open source and released under the [MIT License](LICENSE).
