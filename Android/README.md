## Android App – Dynamic API Base URL

The Android client now reads the backend URL from a Gradle/BuildConfig field so you can point the app at any FastAPI deployment (LAN IP, ngrok tunnel, cloud server, etc.) without touching Kotlin sources.

### How it works

- `app/build.gradle.kts` injects `BuildConfig.API_BASE_URL`. It comes from (in order of precedence):
  1. `-PAPI_BASE_URL=...` passed to Gradle
  2. `API_BASE_URL` environment variable
  3. `http://10.0.2.2:3000/` (emulator-friendly default)
- `ApiConfig.BASE_URL` reads that value at runtime and guarantees a trailing `/`.
- `start.sh` accepts the base URL via argument or environment variable and forwards it to Gradle automatically.

### Building & installing

```bash
# Option A: pass env var
API_BASE_URL="https://your-domain.ngrok-free.app/" ./start.sh

# Option B: pass as first argument
./start.sh https://your-domain.ngrok-free.app/
```

The script will:
1. Assemble the debug APK with the provided URL baked into `BuildConfig`.
2. Install it on the connected device via `adb`.
3. Launch `com.project.lighthouse/.MainActivity`.

### Using ngrok for remote demos

1. Start the backend locally: `wsl python Backend/main.py` (it listens on port `3000`).
2. Start ngrok from the same machine: `ngrok http 3000`. Reserve/keep a stable domain if you need to avoid rebuilding often.
3. Copy the HTTPS URL (e.g., `https://demo.ngrok-free.app`) and use it as the `API_BASE_URL` when running `start.sh`.
4. As long as ngrok is running and forwarding to a live backend, any previously installed APK that points to that hostname will keep working—even if another laptop later runs the backend, provided it serves the same hostname (ngrok reserved domain, DNS record, etc.).

### Tips

- Keep one dedicated URL (reserved ngrok domain, DNS pointing to a remote server, etc.) so the APK does not need to be rebuilt for each network change.
- If someone else runs the backend, have them bind to `0.0.0.0:3000` and either share the LAN IP with you (for a rebuild) or run ngrok with the same reserved domain.
- `API_BASE_URL` must include the protocol and may include a custom port; the script normalizes the trailing slash for Retrofit.


