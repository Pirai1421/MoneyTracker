# MoneyTrack — Android expense tracker

MoneyTrack is a local-first, read-only Android expense tracker MVP.

## Current features
- Kotlin Android app with Jetpack Compose UI.
- Room database stored in app-private local storage.
- Dashboard totals for captured income and expenses (tracked cash flow, not live bank balance).
- Best-effort notification parsing for a small allow-list of payment/bank app package IDs.
- CSV statement import using columns: date,description,amount,type.
- SHA-256 fingerprints and a unique database index to suppress duplicate imports.
- Unit tests for parsing, categorisation and fingerprints.

## Privacy and safety
- The app has no payment initiation, UPI intent, transfer, withdrawal or bill-payment feature.
- It does not request UPI PINs, OTPs, card numbers or bank passwords.
- Transactions are stored locally in the app's private database.
- Android backup is disabled in the manifest.
- Notification access is sensitive and must be explicitly enabled by the device owner.

## Important limitations
- This MVP does not connect directly to GPay, PhonePe, Paytm or bank transaction-history APIs.
- It does not provide universal or guaranteed capture of all transactions.
- Notification capture depends on the app's package ID, notification settings, notification text and parser support; some transactions will be missed.
- No SMS inbox access, Account Aggregator connection or live bank-balance API is implemented.
- CSV import is a simple first version and does not support quoted fields containing commas.
- Database encryption, robust CSV parsing, broader parser validation, privacy policy and security review are still needed before production use.

## Build locally
- JDK 17
- Android SDK with API 35
- Gradle 8.9
- Run: `gradle testDebugUnitTest assembleDebug`

## Automated build
The GitHub Actions workflow at `.github/workflows/android.yml` runs unit tests and assembles a debug APK on pushes to `main`. When a run succeeds, download the `MoneyTrack-debug-apk` artifact from the Actions run page. The artifact is a debug build, not a Play Store release.

## CSV format
Header: `date,description,amount,type`

Example:
```csv
date,description,amount,type
2026-10-31 10:30:00,Swiggy order,249.00,DEBIT
31/10/2026,Salary credit,50000.00,CREDIT
```

Accepted dates: `yyyy-MM-dd HH:mm:ss`, `dd/MM/yyyy`, `dd-MM-yyyy`.
Accepted types: `CREDIT`, `CR`, `INCOME`, `DEBIT`, `DR`, `EXPENSE`.
