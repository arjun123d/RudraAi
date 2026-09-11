# RUDRA AI — Phase 1 & 2 Build

A hands-free, Bengali/Banglish personal voice assistant for Android, built for Arjun.

## What's actually working in this build

- **Futuristic dark Compose UI** with an animated orb that changes color/state
  (IDLE / LISTENING / THINKING / SPEAKING / ERROR) — Section 13/14 from the spec.
- **Push-to-talk voice loop**: tap the mic orb → `SpeechManager` (Android
  `SpeechRecognizer`, `bn-IN` locale) → `CommandRouter` → `TTSManager` speaks the
  reply. This is the real, working version of the wake→listen→think→act→speak
  flow (Section 4).
- **CommandParser + CommandRouter**: fast offline keyword matching for common
  commands (open app, search web/YouTube, call contact, battery, time, date,
  flashlight, volume, lock), with a **confirmation layer** for destructive
  actions and **contact disambiguation** when a name matches more than one
  contact — no silent guessing (Sections 6, 7, 10, 11).
- **ContactManager / CallManager**: resolves relationship labels ("wife", "mom")
  saved via `MemoryManager`, or a direct name search against real Android
  contacts; supports "call the most recent number" via Call Log (Section 7).
- **MemoryManager**: local-only `SharedPreferences` store for the user's name
  and contact-label mappings. Deliberately contains no path for storing
  passwords/PINs (Section 12, 17).
- **AIManager**: anything CommandParser can't classify (casual chat, multi-step
  or free-form phrasing) is sent to a configurable AI chat-completions endpoint
  with a RUDRA personality system prompt. **No API key is hardcoded** — see
  setup below (Sections 5, 20).
- **PermissionManager**: centralizes runtime permission checks and gives a
  spoken Bengali/Banglish explanation whenever a command needs a permission
  that hasn't been granted yet (Section 16/17).

## What is NOT implemented yet, and why

- **Real always-on "Hey Rudra" wake-word detection.** Android's built-in
  `SpeechRecognizer` cannot do continuous offline hotword spotting — it needs
  a network round trip and stops after a pause. `WakeWordService.kt` is
  scaffolded as the foreground service that should host this, with step-by-step
  instructions in its file header for wiring in **Picovoice Porcupine**
  (free tier, lets you train a custom "Hey Rudra" keyword). This needs your
  own Picovoice account — I can't generate that access key or trained model
  file for you.
- **Multi-step app automation** (e.g. "open Instagram, search Hero, open his
  latest reel, comment X") — this needs Android's Accessibility Service for
  UI automation across arbitrary third-party apps, which is a separate,
  higher-scrutiny permission with its own setup flow. The architecture
  (`AutomationManager`) has a clear slot to be added next; it's intentionally
  left out of this first build so the accessibility-service flow gets its own
  focused pass rather than being rushed.
- **Driving Mode manager** and **lock-screen-specific command surface** —
  planned as Phase 9/10 per your original ordering; the core command engine
  they'd sit on top of is already in place.
- Device Admin lock (`LOCK_DEVICE`) requires the user to grant RUDRA Device
  Admin rights once in system settings — this is real Android security, not
  something an app can skip.

## Setup

1. Open this folder in Android Studio (Koala+ recommended).
2. Let Gradle sync.
3. **Set your AI API key at runtime** (no key is in source control):
   run once from a debug screen or adb shell:
   ```kotlin
   ApiKeyProvider.set(context, "sk-...")
   ```
   or wire a small Settings screen that calls this. Any OpenAI-compatible
   endpoint works — change the `endpoint` string in `AIManager.kt` if you use
   a different provider.
4. Build & run on a real device (emulator mics are unreliable for STT testing).
5. Grant Microphone, Contacts, Phone, Call Log, Camera, Notifications when prompted.
6. In Settings (to be built), map relationship labels once, e.g.:
   ```kotlin
   memory.setContactLabel("wife", contactLookupKey)
   ```

## Next steps (pick what you want built next)

1. Wire Picovoice Porcupine for real "Hey Rudra" always-on detection.
2. Settings screen (contact label mapping, API key entry, preferred name).
3. AccessibilityService-based `AutomationManager` for multi-step app control.
4. Driving Mode.
5. Contact disambiguation picker UI (currently surfaces choices as text only).
