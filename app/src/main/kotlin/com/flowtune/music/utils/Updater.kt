package com.flowtune.music.utils
import com.flowtune.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject
object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set
    private fun compareVersions(v1: String, v2: String): Int {
        val v1Parts = v1.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = v2.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until maxLength) {
            val part1 = v1Parts.getOrNull(i) ?: 0
            val part2 = v2Parts.getOrNull(i) ?: 0
            when {
                part1 > part2 -> return 1
                part1 < part2 -> return -1
            }
        }
        return 0
    }
    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }
    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            val response =
                client.get("https:
                    .bodyAsText()
            val json = JSONObject(response)
            val versionName = json.getString("name")
            lastCheckTime = System.currentTimeMillis()
            versionName
        }
    fun getLatestDownloadUrl(): String {
        val baseUrl = "https:
        val architecture = BuildConfig.ARCHITECTURE
        val isGmsVariant = BuildConfig.CAST_AVAILABLE
        return if (architecture == "universal") {
            if (isGmsVariant) {
                baseUrl + "Flowtune-with-Google-Cast.apk"
            } else {
                baseUrl + "Flowtune.apk"
            }
        } else {
            if (isGmsVariant) {
                baseUrl + "app-${architecture}-with-Google-Cast.apk"
            } else {
                baseUrl + "app-${architecture}-release.apk"
            }
        }
    }
}