# [Death Note] - Android Application

A sleek, responsive Android application built with modern **Jetpack Compose**, **Material Design 3**, and an expansive multi-directional infinite writing canvas. The app features adaptive launcher icons, dynamic UI themes, and an automated continuous delivery pipeline for signed release APK generation.

---

## 🛠 Tech Stack & Requirements

- **Kotlin**: 1.9+
- **UI Toolkit**: Jetpack Compose & Material 3
- **JDK**: Java 17 (Recommended: Eclipse Temurin or OpenJDK 17)
- **Android SDK**:
  - `compileSdk`: 34
  - `minSdk`: 24
  - `targetSdk`: 34
- **Build System**: Gradle 8.x with Kotlin DSL (`build.gradle.kts`) & Gradle Wrapper

---

## 🚀 Local Setup & Compilation

### 1. Clone the Repository
```bash
git clone https://github.com/<your-username>/<your-repo-name>.git
cd <your-repo-name>
```

### 2. Verify Prerequisites
Ensure Java 17 is active in your terminal environment:
```bash
java -version
```

### 3. Build Commands

- **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
  The compiled APK will be located at:
  `app/build/outputs/apk/debug/app-debug.apk`

- **Run JVM Unit Tests:**
  ```bash
  ./gradlew testDebugUnitTest
  ```

- **Build Release APK:**
  ```bash
  ./gradlew assembleRelease
  ```
  *Note:* When run locally without custom environment variables, the build system automatically falls back to local signing keys so local development and offline testing are never blocked.

---

## 🔐 Sensitive Value Management & `BuildConfig`

Sensitive tokens and API keys must **never** be committed to version control. This project utilizes the **Secrets Gradle Plugin** configured with `.gitignore` compliance.

### How It Works:
1. **Local Properties / Environment (`.env` or `local.properties`):**
   Copy the provided `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Add your keys in `.env` (or `local.properties`):
   ```properties
   GEMINI_API_KEY=your_secret_api_key_here
   API_SECRET_TOKEN=your_backend_token_here
   ```

2. **Automated Generation into `BuildConfig`:**
   During the Gradle build, the plugin reads properties from `.env` and safely generates strongly typed static constants in:
   ```kotlin
   com.example.BuildConfig.GEMINI_API_KEY
   ```
   Both `.env` and `local.properties` are listed in `.gitignore` to prevent secret leakage.

---

## 🤖 CI/CD with GitHub Actions

The repository includes an automated pipeline in `.github/workflows/build-apk.yml` that builds and signs the production release APK upon every push to `main`.

### Step-by-Step GitHub Repository Secrets Checklist

To enable automated release signing in GitHub Actions, configure the **4 required secrets**:

#### Step 1: Generate or Locate Your Release Keystore
If you don't already have a release keystore, generate one using `keytool`:
```bash
keytool -genkeypair -v -keystore release.keystore -alias deathnote_release -keyalg RSA -keysize 2048 -validity 10000
```

#### Step 2: Encode the Keystore to Base64
Encode the binary `.keystore` or `.jks` file to a single base64 string:
- **macOS / Linux:**
  ```bash
  base64 -w 0 release.keystore > release.keystore.base64.txt
  # or on macOS:
  base64 -i release.keystore | tr -d '\n' > release.keystore.base64.txt
  ```
- **Windows (PowerShell):**
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore")) | Out-File -Encoding ASCII release.keystore.base64.txt
  ```

#### Step 3: Add Secrets to GitHub Repository Settings
1. Navigate to your repository on GitHub.
2. Click **Settings** (gear icon in the top repository menu).
3. In the left sidebar, expand **Secrets and variables** and select **Actions**.
4. Click the green button **New repository secret**.
5. Add each of the following 4 secrets:

| Secret Name | Value Description | Example / Note |
| :--- | :--- | :--- |
| `SIGNING_KEY` | The full base64 string copied from `release.keystore.base64.txt`. | Single line base64 text |
| `KEY_STORE_PASSWORD` | Password created for the keystore file. | Keystore password |
| `ALIAS` | The alias name assigned to the key entry. | e.g. `deathnote_release` |
| `KEY_PASSWORD` | Password created for the specific key entry. | Key password |

#### Step 4: Verification & Artifact Download
1. Push a commit or merge a PR to the `main` branch.
2. Go to the **Actions** tab in your GitHub repository.
3. Select the **Build Signed Release APK** workflow run.
4. Once completed, scroll to the **Artifacts** section at the bottom of the summary page to download `death-note-release-apk.zip` containing the production signed release APK.
