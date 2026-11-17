# EagleOSINT (Android / Jetpack Compose)

Native Android application built with Kotlin, Jetpack Compose, Material 3, and Navigation Compose, featuring a Telegram-inspired messenger interface for OSINT reconnaissance.

## Features

- **UserRecon Bot**: Checks username availability/profiles across 10 platforms (GitHub, Reddit, GitLab, Habr, Medium, Dev.to, Pinterest, Twitch, Steam, Telegram) via OkHttp on background coroutines.
- **GoDorker Search**: Generates ready-to-open Google Dork search queries for social mentions, public documents (`PDF`/`DOCX`), GitHub Gists/Pastebin, and forum profiles.
- **GitHub Inspector**: Queries the GitHub REST API for user profile metrics (repositories, followers, bio) and recently updated public repositories.
- **Social Network Scanner**: Cross-platform username reconnaissance chat module.
- **Telegram-Styled UI**: Classic Blue light & dark Material 3 themes, live chat module search, per-module chat history state, edge-to-edge layout with safe insets, and custom adaptive launcher icon.

## Architecture

- **UI**: Jetpack Compose (`DashboardScreen`, `ScanScreen`, `EagleOSINTTheme`) with type-safe Navigation Compose (`@Serializable` routes).
- **State**: `OsintViewModel` with `StateFlow<Map<String, ScanUiState>>`.
- **Network**: `OsintRepository` using OkHttp and Kotlin Coroutines `Flow` on `Dispatchers.IO`.
