# Task: Build xsgrok2 Android App

Create a complete Android novel generator app that uses Grok 4.20 API to generate novels.

## Core Requirements
1. Call Grok 4.20 API at https://api.apiyi.com/v1 to generate novels
2. API key entered by user in Settings screen
3. Model selection: grok-4.20-beta (base), grok-4.20-beta-0309-reasoning (reasoning)
4. User inputs novel genre and description → AI generates: world setting, key characters, outline
5. User reviews/edits → AI generates chapters
6. Multiple novels management with Room database
7. Universal - works for ALL genres (fantasy/romance/mystery/scifi/daily/comedy)

## Technical Stack
- Kotlin + Jetpack Compose + Material 3
- MVVM architecture + Room database
- OkHttp/Retrofit for API calls (OpenAI-compatible format)
- minSdk 26, targetSdk 34
- Kotlin 1.9.22, Compose BOM 2024.02.00
- Gradle 8.5, AGP 8.2.2

## UI Flow
1. Home: novel list + new button
2. Settings: API Key, model selector, API Base URL (default: https://api.apiyi.com/v1)
3. Create: input genre/description → AI generates settings → review/edit → confirm
4. Reader: chapter list → read → generate next chapter

## GitHub Actions CI/CD
The repo has these GitHub Secrets already set:
- KEYSTORE_BASE64: base64 encoded keystore
- KEYSTORE_PASSWORD: xsgrok2024
- KEY_ALIAS: xsgrok
- KEY_PASSWORD: xsgrok2024

Create .github/workflows/android.yml that:
- Triggers on push to main
- Uses ubuntu-latest + JDK 17
- Has self-contained gradle wrapper
- Does release signed build
- Decodes keystore from KEYSTORE_BASE64 secret
- Reads signing passwords from secrets
- Uploads APK artifact on success

## Steps
1. Create ALL project files (gradle configs, AndroidManifest, all Kotlin source, all resources)
2. Commit and push to GitHub
3. Wait for GitHub Actions build
4. If build fails, read the error log, fix the code, push again
5. Repeat until APK builds successfully

## Important
- Output COMPLETE files, never abbreviate or use "..." 
- Make sure all imports are correct
- Make sure gradle wrapper properties are included
- The app should be functional and build-ready
