# RemindCare

RemindCare is a production-quality, safety-critical Android care coordination application designed for elderly patients and their caregivers. It prioritizes clarity, calmness, and reliability.

## Architecture

- **UI Framework**: Kotlin, Jetpack Compose, Material 3
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Data Persistence**: Local Room Database (Entities: Profile, Reminder, HistoryEvent)
- **Background Tasks**: AlarmManager and BroadcastReceivers for reliable, exact alarm scheduling
- **Security**: Local PIN protection for Caregiver management tools

## Redesign & Refactoring Summary

1. **Design Overhaul**: Replaced generic scaffolding with a calm, high-contrast, Apple-inspired clarity using a refined Material 3 design system. Colors signify intent (Navy for trust, Sage for completion, Coral for urgency).
2. **Simplified Navigation**: Patient navigation is stripped down to essentials. Caregivers have dedicated dashboards for tracking tasks.
3. **Role Segregation & Security**: Patients cannot access or modify caregiver settings or edit the reminder schedule without the Caregiver PIN.
4. **Reliability**: Uses `AlarmManager` with `SCHEDULE_EXACT_ALARM` to trigger an actual Full-Screen Android Intent (`ReminderAlarmActivity`) that wakes up the device, instead of relying on a fragile in-app overlay.
5. **Cleaned Dependencies**: Removed bloat (Firebase, Retrofit, heavy CameraX implementations) since core functionality is meant to be offline-first and privacy-focused.

## Setup & Offline Behavior

- **Offline-First**: RemindCare does not require an active internet connection to schedule, alert, or log completed tasks. All data is persisted locally in the Room Database.
- **Pairing (Simulated Sync)**: Caregiver-Patient pairing currently uses a local verification code to link profiles for demonstration. An optional `SyncRepository` interface could be added for cloud sync.
- **Permissions Required**: 
  - `POST_NOTIFICATIONS`: To show persistent and alarm notifications.
  - `USE_FULL_SCREEN_INTENT` & `DISABLE_KEYGUARD`: To safely wake up the device and show urgent reminder screens when locked.
  - `SCHEDULE_EXACT_ALARM`: For precise medication scheduling.
  - `CAMERA`: For capturing local photo proof of completed tasks.
