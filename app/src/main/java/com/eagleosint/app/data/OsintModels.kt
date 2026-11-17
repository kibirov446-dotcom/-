package com.eagleosint.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.graphics.vector.ImageVector

data class ScanResultItem(
    val platform: String,
    val url: String,
    val status: String,
    val found: Boolean
)

data class OsintModule(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val hint: String,
    val badge: String
)

val telegramModules = listOf(
    OsintModule(
        title = "UserRecon Bot",
        description = "Поиск аккаунтов по никнейму в реальном времени",
        icon = Icons.Default.PersonSearch,
        route = "user_recon",
        hint = "Введите никнейм (например, durov)",
        badge = "10 платформ"
    ),
    OsintModule(
        title = "GoDorker Search",
        description = "Автоматическая генерация поисковых дорк-запросов",
        icon = Icons.Default.TravelExplore,
        route = "godorker",
        hint = "Введите цель или ключевое слово",
        badge = "Google"
    ),
    OsintModule(
        title = "GitHub Inspector",
        description = "Анализ открытых репозиториев и активности",
        icon = Icons.Default.Code,
        route = "github",
        hint = "Введите ник на GitHub",
        badge = "API"
    ),
    OsintModule(
        title = "Social Network Scanner",
        description = "Проверка профилей и связанных учетных записей",
        icon = Icons.Default.Public,
        route = "social",
        hint = "Имя пользователя для проверки",
        badge = "Web"
    )
)

fun findModuleByRoute(route: String): OsintModule =
    telegramModules.firstOrNull { it.route == route } ?: telegramModules.first()
