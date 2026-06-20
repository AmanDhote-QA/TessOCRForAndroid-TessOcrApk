# TesseractOcrHelper

An Android helper application that performs on-device OCR using the Tesseract engine. Built for use in mobile test automation frameworks via Appium, with a manual testing UI included.

---

## How It Works

The app exposes two entry points:

**MainActivity** — a physical UI you can open on the device. Pick any image from the gallery, tap Run OCR, and the extracted text appears on screen. Used for manual verification.

**OcrActivity** — a background, translucent activity with no visible UI. Receives an image path and result filename via Intent extras, runs Tesseract OCR on the image, and writes the extracted text to the app's internal storage. Used by automation frameworks.

The OCR engine (Tesseract) runs entirely on-device. No data leaves the phone — there are no network calls, no telemetry, and no external dependencies at runtime.

On first launch, the app copies `eng.traineddata` from its bundled assets to internal storage. Subsequent runs skip this step.

---

## Project Structure

```
app/src/main/
├── java/com/example/tesseractocr/
│   ├── MainActivity.java       # Physical app UI — pick image, view OCR result
│   └── OcrActivity.java        # Background activity for automation use
├── assets/
│   └── tessdata/
│       └── eng.traineddata     # Bundled English language model (~23MB)
├── res/
│   └── layout/
│       └── activity_main.xml   # Layout for the manual testing UI
└── AndroidManifest.xml
```

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| com.rmtheis:tess-two | 9.1.0 | Tesseract OCR engine for Android |

Minimum SDK: API 23 (Android 6.0)  
Target SDK: API 34

---

## Building

1. Open the project in Android Studio
2. Let Gradle sync complete
3. Go to Build → Build Bundle(s) / APK(s) → Build APK(s)
4. APK is generated at `app/build/outputs/apk/debug/app-debug.apk`

---

## Installation

```bash
adb install -r "Apk Path"
```

---

## Manual Testing via ADB

```bash
# Push a test image to the device
adb push "png image for ocr" /data/local/tmp/ocr_test.png

# Force-stop any running instance first
adb shell am force-stop com.example.tesseractocr

# Start OcrActivity with the image path and result filename
adb shell am start -n com.example.tesseractocr/.OcrActivity \
  --es path /data/local/tmp/ocr_test.png \
  --es resultFile ocr_result_test.txt

# Wait 5 seconds, then read the result
adb shell run-as com.example.tesseractocr cat files/ocr_result_test.txt
```

Note: `force-stop` is required before each manual ADB test to avoid the intent being delivered to a previously running instance instead of starting a fresh one.

---

## Automation Integration (Appium)

From your Appium test framework, call OcrActivity using the following pattern:

```java
String uid            = String.valueOf(System.currentTimeMillis());
String deviceImg      = "/data/local/tmp/ocr_input_" + uid + ".png";
String resultFile     = "ocr_result_" + uid + ".txt";
String internalResult = "/sdcard/Android/data/com.example.tesseractocr/files/" + resultFile;

((AndroidDriver) driver).pushFile(deviceImg, cropFile);

driver.executeScript("mobile: terminateApp", Map.of("appId", "com.example.tesseractocr"));

Activity ocrActivity = new Activity("com.example.tesseractocr", ".OcrActivity");
ocrActivity.setStopApp(false);
ocrActivity.setOptionalIntentArguments("--es path " + deviceImg + " --es resultFile " + resultFile);
((AndroidDriver) driver).startActivity(ocrActivity);

// Poll for result
for (int i = 1; i <= 20; i++) {
    Thread.sleep(1000);
    byte[] bytes = ((AndroidDriver) driver).pullFile(internalResult);
    if (bytes != null && bytes.length > 0) {
        String result = new String(bytes).trim();
        if (!result.isEmpty() && !result.startsWith("ERROR")) break;
    }
}
```

`terminateApp` replaces the manual `force-stop` step and ensures a clean state before each OCR call.

---

## Security

- No `INTERNET` permission is declared. Android blocks all outbound network calls at OS level.
- Verify this independently: `aapt dump permissions app-debug.apk`
- Tesseract engine is Apache 2.0 licensed — full source available at github.com/tesseract-ocr/tesseract

---

## OcrActivity Intent Parameters

| Extra key | Type | Description |
|---|---|---|
| path | String | Absolute path to the input PNG on the device |
| resultFile | String | Filename (not full path) for the OCR result text file |

Result is written to the app's internal files directory:  
`/sdcard/Android/data/com.example.tesseractocr/files/<resultFile>`
