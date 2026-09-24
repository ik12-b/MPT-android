# MoneyPrinterTurbo Android (Kotlin) – Mode 2 Local/Hybrid

Versi Android untuk **pemakaian pribadi**. Build bisa murni lewat **GitHub Actions** (tanpa Android Studio).

## Build lewat GitHub Actions (tanpa Android Studio)

1. Buat repo kosong di GitHub
2. Upload / push isi folder project ini ke repo
3. Buka tab **Actions** → pilih workflow **Build Android APK**
4. Klik **Run workflow** (atau push ke `main` supaya otomatis jalan)
5. Tunggu selesai (~10–20 menit pertama kali)
6. Download artifact **app-debug-apk** → dapat file `.apk`
7. Install ke HP:
   ```bash
   adb install -r app-debug.apk
   ```
   atau kirim file APK ke HP lalu install manual (aktifkan “Install from unknown sources”)

### Catatan CI
- FFmpeg-kit memakai fork yang masih aktif: `dev.ffmpegkit-maintained`
- APK debug unsigned (cukup untuk pemakaian pribadi)
- Artifact disimpan 14 hari

## Setup di dalam app (setelah install)

1. Buka tab **Pengaturan**
2. Isi:
   - **LLM Base URL** – contoh `https://api.openai.com` atau `https://api.deepseek.com`
   - **LLM API Key**
   - **Model** – contoh `gpt-4o-mini` / `deepseek-chat`
   - **Pexels API Key** – gratis di https://www.pexels.com/api/
3. (Opsional) Copy lagu `.mp3` ke:
   `Android/data/com.moneyprinter.turbo/files/bgm/`
4. Tab **Buat** → isi topik → Generate
5. Hasil di tab **Riwayat** → Putar

## Fitur

| Fitur | Status |
|-------|--------|
| AI script (OpenAI-compatible) | ✅ |
| Keyword materi | ✅ |
| TTS lokal | ✅ |
| Download Pexels | ✅ |
| Subtitle SRT | ✅ |
| BGM lokal | ✅ |
| Render FFmpeg | ✅ |
| History + putar video | ✅ |
| Settings API Key | ✅ |
| GitHub Actions APK | ✅ |

## Struktur

```
├── .github/workflows/android-build.yml   ← CI build
├── app/
│   └── src/main/java/com/moneyprinter/turbo/
│       ├── data/       model, Room, API, Settings
│       ├── service/    LLM, TTS, Material, Subtitle, Video, BGM, Pipeline
│       ├── ui/         Create, History, Settings
│       ├── worker/     WorkManager
│       └── util/       PermissionHelper
├── gradle/wrapper/
├── gradlew
└── README.md
```

## Build lokal (opsional)

Kalau punya Android SDK:
```bash
cp local.properties.example local.properties
# edit sdk.dir
chmod +x gradlew
./gradlew assembleDebug
```

---

Siap untuk pemakaian pribadi. Build utama disarankan lewat GitHub Actions.
