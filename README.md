<div align="center">

<img width="2560" height="800" alt="banner" src="https://github.com/user-attachments/assets/5874adf2-626b-4c7c-b3b0-2d1efc267027" />

[![Downloads](https://img.shields.io/github/downloads/iravikant/Black_Call/total?style=for-the-badge&color=22C55E)](https://github.com/iravikant/Black_Call/releases)
[![CI](https://img.shields.io/github/actions/workflow/status/iravikant/Black_Call/ci.yml?style=for-the-badge&label=build)](https://github.com/iravikant/Black_Call/actions/workflows/ci.yml)
![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)

### **[⬇️ Download the latest APK](https://github.com/iravikant/Black_Call/releases/latest)**

</div>

---

## ✨ Why Black Call?

Most call blockers upload your call history to a server. **Black Call never leaves your phone.**
It uses Android's official `CallScreeningService`, so unwanted calls are stopped *before the phone rings*, and the
app has **no internet permission at all**.

| | |
|---|---|
| 🛡️ **Blocks before ringing** | Built on the system call-screening role, not hacks or overlays |
| 🔒 **100% on-device** | No internet permission, no accounts, no analytics |
| 🧠 **Smart spam scoring** | Heuristics tuned for Indian numbering (e.g. the `140` telemarketing series) |
| 🎯 **Flexible rules** | Blocked list, allowed list, prefixes (`140`, `+92`), contacts-only, quiet hours |
| 🕵️ **Spoof detection** | Flags unverified / spoofed callers using STIR/SHAKEN signals |
| 📜 **Fully transparent** | Every block is logged with its reason, spam score and signals |
| 💾 **Backup** | Export / import your lists as CSV |
| 🎨 **Gorgeous UI** | Jetpack Compose + Material 3 with a black / teal / green gradient theme |

## 📸 Screenshots

<!-- Add screenshots to /assets and uncomment:
<p align="center">
  <img src="assets/shield.png" width="23%"/>
  <img src="assets/lists.png" width="23%"/>
  <img src="assets/activity.png" width="23%"/>
  <img src="assets/settings.png" width="23%"/>
</p>
-->
*Shield (home) · Lists · Activity · Settings*

## 📲 Install

1. Open the **[Releases](https://github.com/iravikant/Black_Call/releases/latest)** page.
2. Download `Black-Call-vX.Y.Z.apk` on your phone.
3. Tap it and allow **Install unknown apps** if Android asks.
4. Open Black Call and grant the **Call screening** role (the onboarding wizard walks you through it).

> Every release also ships an `.aab` and a `SHA256SUMS.txt`.
> Verify a download with `sha256sum -c SHA256SUMS.txt`.

## 🧭 How a call is decided

First match wins:

```
emergency → protection off → allowed list → blocked list → prefix rule → saved contact
→ quiet hours → contacts-only → hidden number → spoofed → unknown → spam score
```

Blocked calls are either **rejected** or **silenced** (stay as a missed call), your choice.

## 🏗️ Architecture

```
domain/       Pure Kotlin: BlockEngine, SpamScorer, PhoneNumbers, CsvBackup   (unit tested)
data/         Room (rules + call log), DataStore (settings), repository, contacts lookup
service/      BlackCallScreeningService, BlockNotifier
permissions/  PermissionController (state machine + launchers)
ui/           theme, components, navigation, one package per screen (ViewModel + Screen)
di/           AppContainer (manual DI; the service reads an in-memory rule snapshot)
```

**Stack:** Kotlin · Jetpack Compose · Material 3 · Room · DataStore · Coroutines/Flow · Navigation Compose

## 🔨 Build from source

```bash
git clone https://github.com/iravikant/Black_Call.git
cd Black_Call
./gradlew testDebugUnitTest      # run unit tests
./gradlew assembleDebug          # app/build/outputs/apk/debug/
```

Requires JDK 17 and Android Studio (AGP 8.13). Run on a device with a SIM, then call it from another phone
while that number is on the blocked list.

## 🚀 Releasing (maintainers)

Releases are fully automated: **pushing a tag builds, signs and publishes everything.**

```bash
./scripts/release.sh 1.1.0          # bumps nothing locally, creates + pushes tag v1.1.0
./scripts/release.sh 1.2.0-beta.1   # pre-release
```

The `Release` workflow then:

1. derives `versionName` (`1.1.0`) and `versionCode` (`10100`) from the tag,
2. runs unit tests,
3. builds a **signed APK and AAB**,
4. generates `SHA256SUMS.txt`,
5. publishes a **GitHub Release** with auto-generated notes and all files attached.

If a release build succeeds but publishing fails, use the `Release` workflow's **Run workflow**
option on the `main` branch and enter the existing version tag to retry publishing it.

One-time setup: create a keystore (`./scripts/make-keystore.sh`) and add these repository secrets
(*Settings → Secrets and variables → Actions*): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
`./scripts/setup-github.sh` can set them for you.

> ⚠️ Back up your keystore. Updates must be signed with the same key.

## 🔐 Privacy

- No `INTERNET` permission, so nothing can be sent off-device.
- Permissions used: contacts (to allow saved contacts), call log (screening/activity), notifications (block alerts).
- All rules and logs live in a local Room database.

## 🗺️ Notes

- Spam scoring is tuned for Indian numbering. Adjust `SpamScorer` for other regions.
- The app deliberately does **not** request the default-dialer role; that would break the phone without an in-call UI.

## 🤝 Contributing

Issues and PRs are welcome. Run `./gradlew testDebugUnitTest` before opening a PR. Use the
`feature`, `bug`, `ui`, `security` labels so release notes categorise your change.

---

<div align="center">Made with 🖤 for quieter phones.</div>
