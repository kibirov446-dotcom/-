package com.eagleosint.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OsintRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val platforms = mapOf(
        "GitHub" to "https://github.com/%s",
        "Reddit" to "https://www.reddit.com/user/%s",
        "GitLab" to "https://gitlab.com/%s",
        "Habr" to "https://habr.com/ru/users/%s/",
        "Medium" to "https://medium.com/@%s",
        "Dev.to" to "https://dev.to/%s",
        "Pinterest" to "https://www.pinterest.com/%s/",
        "Twitch" to "https://www.twitch.tv/%s",
        "Steam" to "https://steamcommunity.com/id/%s",
        "Telegram" to "https://t.me/%s"
    )

    val platformCount: Int
        get() = platforms.size

    fun checkUsername(username: String): Flow<ScanResultItem> = flow {
        val cleanUser = URLEncoder.encode(username.trim(), "UTF-8")
        for ((name, template) in platforms) {
            val url = String.format(template, cleanUser)
            val result = try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 17) AppleWebKit/537.36")
                    .build()

                client.newCall(request).execute().use { response ->
                    val isFound = response.code == 200
                    ScanResultItem(
                        platform = name,
                        url = url,
                        status = if (isFound) "Профиль найден (HTTP 200)" else "Не найдено (${response.code})",
                        found = isFound
                    )
                }
            } catch (e: Exception) {
                ScanResultItem(
                    platform = name,
                    url = url,
                    status = "Ошибка сети: ${e.localizedMessage ?: "таймаут"}",
                    found = false
                )
            }
            emit(result)
        }
    }.flowOn(Dispatchers.IO)

    fun inspectGitHubUser(username: String): Flow<ScanResultItem> = flow {
        val cleanUser = URLEncoder.encode(username.trim(), "UTF-8")
        val profileUrl = "https://github.com/$cleanUser"
        val userApiUrl = "https://api.github.com/users/$cleanUser"
        val reposApiUrl = "https://api.github.com/users/$cleanUser/repos?sort=updated&per_page=5"

        try {
            val userRequest = Request.Builder()
                .url(userApiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "EagleOSINT-Android")
                .build()

            var profileFound = false
            client.newCall(userRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val login = json.optString("login", username.trim())
                    val name = json.optString("name", "").takeIf { it != "null" && it.isNotBlank() }
                    val repos = json.optInt("public_repos", 0)
                    val followers = json.optInt("followers", 0)
                    val bio = json.optString("bio", "").takeIf { it != "null" && it.isNotBlank() }
                    val summary = buildString {
                        append(name ?: login)
                        append(" • Репозитории: $repos • Подписчики: $followers")
                        if (!bio.isNullOrBlank()) {
                            append("\n$bio")
                        }
                    }
                    profileFound = true
                    emit(
                        ScanResultItem(
                            platform = "GitHub Профиль (@$login)",
                            url = json.optString("html_url", profileUrl),
                            status = summary,
                            found = true
                        )
                    )
                } else {
                    emit(
                        ScanResultItem(
                            platform = "GitHub Профиль",
                            url = profileUrl,
                            status = if (response.code == 404) {
                                "Пользователь не найден (HTTP 404)"
                            } else {
                                "GitHub API ответ: HTTP ${response.code}"
                            },
                            found = false
                        )
                    )
                }
            }

            if (profileFound) {
                val reposRequest = Request.Builder()
                    .url(reposApiUrl)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "EagleOSINT-Android")
                    .build()

                client.newCall(reposRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val array = JSONArray(response.body?.string().orEmpty())
                        if (array.length() == 0) {
                            emit(
                                ScanResultItem(
                                    platform = "Публичные репозитории",
                                    url = profileUrl,
                                    status = "Открытые репозитории отсутствуют",
                                    found = false
                                )
                            )
                        } else {
                            for (i in 0 until array.length()) {
                                val repo = array.getJSONObject(i)
                                val repoName = repo.optString("name", "repo")
                                val lang = repo.optString("language", "—").takeIf { it != "null" && it.isNotBlank() } ?: "—"
                                val stars = repo.optInt("stargazers_count", 0)
                                val desc = repo.optString("description", "").takeIf { it != "null" && it.isNotBlank() }
                                val statusText = buildString {
                                    append("Язык: $lang • ★ $stars")
                                    if (!desc.isNullOrBlank()) append("\n$desc")
                                }
                                emit(
                                    ScanResultItem(
                                        platform = repoName,
                                        url = repo.optString("html_url", profileUrl),
                                        status = statusText,
                                        found = true
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit(
                ScanResultItem(
                    platform = "GitHub Inspector",
                    url = profileUrl,
                    status = "Ошибка сети: ${e.localizedMessage ?: "таймаут"}",
                    found = false
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    fun generateDorks(target: String): List<ScanResultItem> {
        val encoded = URLEncoder.encode(target.trim(), "UTF-8")
        val queries = listOf(
            "Упоминания в соцсетях" to "https://www.google.com/search?q=%22$encoded%22+site:linkedin.com+OR+site:github.com",
            "Публичные документы (PDF/DOC)" to "https://www.google.com/search?q=%22$encoded%22+filetype:pdf+OR+filetype:docx",
            "Открытые репозитории и Gist" to "https://www.google.com/search?q=%22$encoded%22+site:gist.github.com+OR+site:pastebin.com",
            "Индексация на форумах" to "https://www.google.com/search?q=%22$encoded%22+inurl:profile+OR+inurl:user"
        )
        return queries.map { (title, url) ->
            ScanResultItem(
                platform = title,
                url = url,
                status = "Готовый поисковый запрос",
                found = true
            )
        }
    }
}
