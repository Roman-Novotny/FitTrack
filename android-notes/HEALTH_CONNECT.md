# Samsung Health / Galaxy Watch integration plan

Samsung Health on the Galaxy Watch syncs supported measurements to Samsung Health on the phone. FitTrack's Android layer should read shared records through Android Health Connect after the user grants each permission.

Suggested first read permissions:
- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_HEART_RATE`
- `android.permission.health.READ_SLEEP`
- `android.permission.health.READ_EXERCISE`
- `android.permission.health.READ_WEIGHT`

Implementation sequence:
1. Add AndroidX Health Connect client dependency to the native Android app.
2. Declare Health Connect permissions and rationale in AndroidManifest.xml.
3. Check Health Connect availability on device and request runtime permissions.
4. Read step, heart-rate, sleep-session, exercise-session and weight records with date-range filters.
5. Normalize records in the native bridge and sync to Supabase under the signed-in user's ID.
6. Provide a connection status, last-sync time, manual sync, permission management and data deletion controls.

Samsung Health must be configured to share data with Health Connect. Some Samsung-specific activity metrics may not be available through Health Connect. For additional supported data types, evaluate Samsung Health Data SDK; it requires Java 17+, Samsung Health 6.30.2+, device support and explicit user consent. Do not use the deprecated Samsung Health SDK for Android.

Never infer a medical diagnosis from heart-rate, sleep or exercise data. Use least-privilege permissions and collect only the data the user enables.
