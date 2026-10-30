# Black Call

On-device call blocker for Android 10+, built with Jetpack Compose. Uses Android's
`CallScreeningService`, so calls are stopped before the phone rings. No internet permission.

## What's inside

| Area | Details |
|---|---|
| UI | 100% Jetpack Compose + Material 3, dark black/teal/green/white gradient theme |
| Screens | Onboarding wizard, Shield (home), Lists, Activity, Settings |
| Filters | Blocked list, allowed list, number prefixes (`140`, `+92`), unknown, hidden, spoofed (STIR/SHAKEN), spam score, contacts-only, quiet hours |
| Block action | Reject, or silence (call stays as a missed call) |
| Transparency | Every block is logged with its reason, spam score and signals |
| Backup | CSV export/import of lists |
| Permissions | Call screening role, Contacts, Notifications: guided, with permanent-denial detection and settings fallbacks |

## Architecture

```
domain/      Pure Kotlin: BlockEngine, SpamScorer, PhoneNumbers, CsvBackup  (unit tested)
data/        Room (rules + call log), DataStore (settings), repository, contacts lookup
service/     BlackCallScreeningService, BlockNotifier
permissions/ PermissionController (state machine + launchers)
ui/          theme, components, navigation, one package per screen (ViewModel + Screen)
di/          AppContainer (manual DI; the service reads an in-memory rule snapshot)
```

Decision order (first match wins): emergency → protection off → allowed list → blocked list →
prefix rule → saved contact → quiet hours → contacts-only → hidden → spoofed → unknown → spam score.

## Build

Open in Android Studio (AGP 8.13, JDK 11+), sync, run on a device with a SIM.
On first launch grant the **Call screening** role. To test, call the device from another
phone while the number is on the blocked list.

Unit tests (`app/src/test`) cover the engine, scorer, number handling and CSV backup.

## Notes for upgraders from v1

- The old AppLock module, Lottie animations, TFLite dependency and ~60 SVG drawables were removed.
- The old `MyInCallService` and the **default dialer** role request were removed: becoming the
  default dialer without an in-call UI would break the phone for users.
- Database is new (`blackcall.db`); v1 data is not migrated.
- Spam scoring is tuned for Indian numbering (140 series). Adjust `SpamScorer` for other regions.
