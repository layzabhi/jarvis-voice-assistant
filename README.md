# Jarvis: Local-First Android Voice Assistant

Jarvis is a private, local-first voice assistant for Android. A floating overlay bubble stays on screen, and every voice command is parsed and executed completely on-device without any AI backend or API keys.

## Key Features

- **Floating Overlay Bubble**: Draggable 56dp circle over apps (`SYSTEM_ALERT_WINDOW`). Single tap triggers listening; drag moves the bubble and snaps smoothly to screen edges.
- **Visual State Indicators**:
  - **Idle**: Purple (`#7F77DD`)
  - **Listening**: Red (`#FF4444`) with pulse animation
  - **Thinking**: Amber (`#FFAA00`)
  - **Speaking**: Teal (`#00B4D8`)
- **Offline & Rule-Based Natural Language Understanding (NLU)**:
  - Phone Calls: Fuzzy contact matching via Levenshtein distance & prefix scoring (`READ_CONTACTS`, `CALL_PHONE`).
  - Alarms & Timers: Direct integration with Android system clock (`AlarmClock.ACTION_SET_ALARM`, `AlarmClock.ACTION_SET_TIMER`).
  - Reminders & Birthdays: Persistent Room database (`AppDb`), exact alarms with `AlarmManager`, boot rescheduling via `BootReceiver`.
  - Music & Search: Spotify deep linking (`spotify:search:`) and web search fallbacks.
- **Privacy & Offline First**: Zero cloud AI APIs, zero external telemetry; works completely offline with downloaded Google offline speech packs.
- **Setup & Diagnostics UI**: Jetpack Compose screen showing real-time permission checklist, in-app text tester, and active reminders/birthdays manager.

## Architecture

```
Floating Bubble (tap)
  -> SpeechInput (SpeechRecognizer / en-IN / offline preference)
  -> CommandParser (Regex rule matching -> typed Command sealed class)
  -> ActionExecutor (Intents, Room DB, AlarmManager, ContactResolver)
  -> SpeechOutput (TextToSpeech reply + main thread UI color reset)
```

## Permissions Used

- `RECORD_AUDIO`: Voice input for `SpeechRecognizer`.
- `SYSTEM_ALERT_WINDOW`: Drawing the floating overlay bubble above all apps.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` / `FOREGROUND_SERVICE_MICROPHONE`: Keeps bubble alive and manages background microphone access on Android 14+.
- `POST_NOTIFICATIONS`: Persistent bubble notification with "Stop" button and reminder alerts.
- `READ_CONTACTS` & `CALL_PHONE`: Contact resolution and voice dialing.
- `SET_ALARM` & `SCHEDULE_EXACT_ALARM`: Precise clock alarms and reminder delivery.
- `RECEIVE_BOOT_COMPLETED`: Rescheduling alarms across device reboots.

## Running & Building the App

### Option A: Cloud Build via GitHub Actions (No Android Studio Needed!)
1. Create a repository on GitHub (e.g. `jarvis-assistant`).
2. Push this project code:
   ```bash
   git init
   git add .
   git commit -m "Initial commit of Jarvis assistant"
   git branch -M main
   git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPO_NAME>.git
   git push -u origin main
   ```
3. Open your repo on GitHub and click on the **Actions** tab.
4. The workflow **"Build Jarvis APK"** will run automatically, execute unit tests, and compile `app-debug.apk`.
5. Once completed, scroll to the **Artifacts** section at the bottom of the run page, click **jarvis-debug-apk** to download the zip file, and install `app-debug.apk` directly onto your Android device!

### Option B: Local Build via Android Studio
1. Open the project in **Android Studio** (with JDK 17).
2. Sync Gradle and run `./gradlew test` to execute the unit test suite (`CommandParserTest`, `TimeParserTest`, `ContactResolverTest`).
3. Deploy directly to a connected Android device or emulator.

---

## First-Time Device Setup Checklist

Once installed on your phone:
1. Open **Jarvis** and complete the checklist:
   - **Microphone**: Voice recognition access.
   - **Draw Over Other Apps (Overlay)**: Floating bubble window.
   - **Contacts & Calls**: Voice dialing.
   - **Notifications & Exact Alarms**: Background alerts.
   - **Battery Optimization Exemption**: Unrestricted background activity.
2. Tap **"Start Bubble"** to launch the floating overlay.

