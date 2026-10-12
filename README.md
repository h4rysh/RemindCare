# RemindCare 🌿

> **Safety-critical, high-reliability elderly medication reminder and care coordination system.**

RemindCare is an offline-first Android application architected for seniors and their caregivers. Designed with **Apple Human Interface Guidelines (HIG)** craft and built using **Android Clean Architecture**, it combines serene ergonomics with hardware-exact alarm reliability and real-time cloud adherence monitoring.

---

## 🏛️ Architecture

RemindCare strictly adheres to **Android Clean Architecture** with unidirectional data flow (UDF):

```
app/src/main/java/com/aistudio/remindcare/
├── domain/                                  # Pure Kotlin (Zero android.* imports)
│   ├── model/                               # Domain models (Reminder, History, Settings, Pairing)
│   ├── repository/                          # Abstract repository & hardware gateway contracts
│   └── usecase/                             # Single-responsibility business UseCases (22 total)
│       ├── reminder/                        # Medication schedule CRUD
│       ├── dose/                            # Dose completion, snooze, and timeout audit
│       ├── auth/                            # Caregiver PIN validation & PBKDF2 hashing
│       ├── pairing/                         # Handshake & approval flow
│       ├── settings/                        # User preferences & profile management
│       └── sync/                            # Cloud synchronization
├── data/                                    # Persistence & Synchronization
│   ├── mapper/                              # Bidirectional entity <-> domain mappers
│   ├── gateway/                             # AndroidAlarmScheduler, FilePhotoStorage
│   ├── repository/                          # Repository implementations
│   └── FirebaseSyncRepository.kt            # Firestore real-time sync & anonymous auth
└── ui/                                      # Senior-First Jetpack Compose
    ├── RemindCareViewModel.kt               # StateFlows delegating strictly to UseCases
    ├── components/                          # Accessible components (CaregiverPinPadDialog)
    ├── screens/                             # PatientHomeScreen, CaregiverDashboard, Pairing
    └── theme/                               # High-contrast M3 theme & tactile haptics
```

### Architectural Principles
- **Pure Kotlin Domain**: Core business rules have zero dependencies on the Android framework, enabling fast, isolated JVM unit testing.
- **Offline-First Source of Truth**: Room SQLite database guarantees full operation without network access.
- **Role-Segregated Cloud Sync**: Firestore real-time sync with anonymous authentication provides zero-friction caregiver pairing without patient passwords.
- **Zero PHI Telemetry**: Crashlytics logging is strictly anonymized with zero Personal Health Information (PHI).

---

## 👵 Senior Ergonomics & Apple HIG Craft

RemindCare eliminates clinical clutter in favor of serene, dignified ergonomics:
- **WCAG 2.2 AAA Contrast**: Calming high-contrast palette (`NavyPrimary #1C3A5A`, `SageGreen #4A7C59`, `CoralUrgent #C84B31`, `WarmOffWhite #FAF9F6`).
- **Touch Target Assurance**: All buttons, chips, and interactive cards meet or exceed $48\text{dp} \times 48\text{dp}$ touch targets.
- **Accessible Numeric PIN Pad**: Custom 4-digit caregiver keypad with error shake animation and senior-friendly sizing.
- **Tactile Haptic Feedback**: Context-aware physical confirmation on every keypress, dose confirmation, and toggle via `RemindCareHaptics`.
- **Dynamic Type & TalkBack**: Full font scaling support up to 200% with descriptive semantic labels for screen readers.

---

## ⏰ Safety-Critical Alarm Engine

- **Hardware Exact Alarms**: Configured via `AlarmManager.setExactAndAllowWhileIdle()` to guarantee delivery across Android Doze mode and OEM battery restrictions.
- **Full-Screen Wake Intent**: Wakes locked screens with an emergency full-screen activity (`ReminderAlarmActivity`).
- **Boot Persistence**: `BOOT_COMPLETED` receivers reschedule all active reminders following device restarts.
- **Automatic Missed Dose Detection**: Background watchdog marks unconfirmed doses as `MISSED` after timeout and logs adherence rates to the caregiver.

---

## 🧪 Testing & Quality Verification

RemindCare is covered by an automated test suite across all layers:

- **Unit Tests**: 49 tests verifying domain UseCases, repository operations, cryptographic PBKDF2 hashing, and bidirectional mappers.
- **Screenshot Regression Tests**: Visual verification powered by Roborazzi and Robolectric Native Graphics.
- **Code Coverage Gate**: Enforced Kover verification requiring $\ge 80\%$ test coverage on domain UseCases and data mappers (`koverVerify`).

### Run Test Suite
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest koverVerify
```

### Build APK
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```
The compiled APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📱 Permissions
- `SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM`: Precise medication schedule triggering.
- `USE_FULL_SCREEN_INTENT` & `DISABLE_KEYGUARD`: Immediate wake-up on locked devices for critical alerts.
- `RECEIVE_BOOT_COMPLETED`: Alarm rescheduling across device reboots.
- `VIBRATE` & `POST_NOTIFICATIONS`: Tactile feedback and status notifications.
- `INTERNET`: Real-time Firebase Firestore care team synchronization.
