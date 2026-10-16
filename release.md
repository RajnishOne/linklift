# Release Guide for LinkLift (GitHub & F-Droid)

This guide documents the step-by-step process for preparing, testing, and publishing a new release of LinkLift for both GitHub Releases and F-Droid.

---

## Prerequisites

- Working directory is clean (`git status`).
- Required development tools installed:
  - **JDK 21** (Temurin recommended)
  - **Python 3.13** (required for Chaquopy local builds)
  - Android SDK configured (via `ANDROID_HOME` or `local.properties`)

---

## Step 1: Bump Version in `app/build.gradle.kts`

Open [`app/build.gradle.kts`](file:///Volumes/SSD/StudioProjects/LinkLift/app/build.gradle.kts) and locate the `defaultConfig` block:

```kotlin
    defaultConfig {
        applicationId = "com.rjnsdev.linklift.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 20        // Increment integer by 1 (e.g., 19 -> 20)
        versionName = "3.0"     // Set new semantic version string (e.g., "2.9" -> "3.0")
        ...
    }
```

- **`versionCode`**: Must be strictly incremented by `1` from the previous release.
- **`versionName`**: Semantic version string (e.g., `3.0`, `3.0.1`).

---

## Step 2: Add Fastlane Changelog File

Create a new changelog file named after the new **`versionCode`** (not `versionName`):

- **Path**: [`fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`](file:///Volumes/SSD/StudioProjects/LinkLift/fastlane/metadata/android/en-US/changelogs/) (e.g., `fastlane/metadata/android/en-US/changelogs/20.txt`)

### Format Guidelines
- Write concise, bulleted lines describing user-facing improvements, new platform features, and bug fixes.
- This file is parsed by:
  - **F-Droid Client**: Displays what's new in this version.
  - **GitHub Actions**: Automatically extracted by [`.github/workflows/release.yml`](file:///Volumes/SSD/StudioProjects/LinkLift/.github/workflows/release.yml) to generate GitHub Release notes.

**Example (`20.txt`):**
```text
- Added high-speed batch download support for YouTube playlists
- Improved Instagram Reels audio extraction reliability
- Updated yt-dlp core extractor dependencies
- UI enhancements and memory optimizations for long background downloads
```

---

## Step 3: Update Fastlane Descriptions (Optional / When Needed)

If there are updates to store branding or keywords, review:
- [`fastlane/metadata/android/en-US/short_description.txt`](file:///Volumes/SSD/StudioProjects/LinkLift/fastlane/metadata/android/en-US/short_description.txt) (Max 80 characters)
- [`fastlane/metadata/android/en-US/full_description.txt`](file:///Volumes/SSD/StudioProjects/LinkLift/fastlane/metadata/android/en-US/full_description.txt) (Max 4000 characters)

---

## Step 4: Update F-Droid Recipe (`fdroid/com.rjnsdev.linklift.app.yml`)

Open [`fdroid/com.rjnsdev.linklift.app.yml`](file:///Volumes/SSD/StudioProjects/LinkLift/fdroid/com.rjnsdev.linklift.app.yml) and update:

1. **Add new build entry under `Builds:`**

```yaml
  - versionName: '3.0'
    versionCode: 20
    commit: v3.0
    subdir: app
    gradle:
      - yes
```

2. **Update `CurrentVersion` and `CurrentVersionCode` at the bottom:**

```yaml
CurrentVersion: '3.0'
CurrentVersionCode: 20
```

> **Note:** The `commit` field points to the git tag (e.g., `v3.0`).

---

## Step 5: Test & Validate the Build Locally

Run unit tests and verify the release assemble task passes without errors:

```bash
# 1. Run unit tests
./gradlew testDebugUnitTest

# 2. Build release APK
./gradlew assembleRelease

# 3. (Optional) Run on a connected device / emulator
./scripts/run-android.sh
```

---

## Step 6: Commit and Push Changes to `main`

Stage all modified and created files, commit, and push to GitHub:

```bash
# Stage the files
git add app/build.gradle.kts
git add fastlane/metadata/android/en-US/changelogs/<versionCode>.txt
git add fdroid/com.rjnsdev.linklift.app.yml
git add fastlane/metadata/android/en-US/  # If descriptions were updated

# Commit
git commit -m "feat: bump version to <versionName> (versionCode <versionCode>)"

# Push to main
git push origin main
```

---

## Step 7: Create and Push the Git Release Tag

Tag the release with a `v` prefix matching `versionName`:

```bash
# Create annotated tag
git tag -a v<versionName> -m "Release v<versionName>"

# Push tag to GitHub
git push origin v<versionName>
```

### What Happens Next (Automated via GitHub Actions):
1. Pushing the `v*` tag triggers [`.github/workflows/release.yml`](file:///Volumes/SSD/StudioProjects/LinkLift/.github/workflows/release.yml).
2. The workflow:
   - Sets up JDK 21 and Python 3.13.
   - Signs the APK with repository release secrets (if configured).
   - Reads the latest changelog from `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
   - Generates a GitHub Release tagged `v<versionName>` with attached signed APKs.

---

## Step 8: F-Droid Ingestion & Synchronization

LinkLift's F-Droid recipe is configured with:
```yaml
AutoUpdateMode: Version
UpdateCheckMode: Tags
```

- **Automatic Check**: The F-Droid build robot (`checkupdates`) scans tags in the LinkLift GitHub repository periodically. Once `v<versionName>` is pushed, F-Droid detects the new release, builds it in their clean container, and signs it with the F-Droid keys.
- **Manual Upstream Sync (fdroiddata)**: If needed to expedite inclusion or update repository metadata in the official F-Droid catalog:
  1. Fork / Clone [fdroiddata](https://gitlab.com/fdroid/fdroiddata).
  2. Copy [`fdroid/com.rjnsdev.linklift.app.yml`](file:///Volumes/SSD/StudioProjects/LinkLift/fdroid/com.rjnsdev.linklift.app.yml) into `metadata/com.rjnsdev.linklift.app.yml`.
  3. Submit a Merge Request (MR) against `fdroid/fdroiddata`.

---

## Quick Release Checklist

| Step | Action | Key File |
|---|---|---|
| 1 | Bump `versionCode` & `versionName` | [`app/build.gradle.kts`](file:///Volumes/SSD/StudioProjects/LinkLift/app/build.gradle.kts) |
| 2 | Create changelog `<versionCode>.txt` | [`fastlane/metadata/android/en-US/changelogs/`](file:///Volumes/SSD/StudioProjects/LinkLift/fastlane/metadata/android/en-US/changelogs/) |
| 3 | (Optional) Review SEO descriptions | [`fastlane/metadata/android/en-US/`](file:///Volumes/SSD/StudioProjects/LinkLift/fastlane/metadata/android/en-US/) |
| 4 | Add build entry & update current version | [`fdroid/com.rjnsdev.linklift.app.yml`](file:///Volumes/SSD/StudioProjects/LinkLift/fdroid/com.rjnsdev.linklift.app.yml) |
| 5 | Verify tests & assemble locally | `./gradlew testDebugUnitTest && ./gradlew assembleRelease` |
| 6 | Commit & push to `main` | `git push origin main` |
| 7 | Create & push tag `v<versionName>` | `git tag -a v<versionName> && git push origin v<versionName>` |
