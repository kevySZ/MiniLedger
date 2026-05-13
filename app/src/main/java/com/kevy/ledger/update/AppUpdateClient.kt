package com.kevy.ledger.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.kevy.ledger.R
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val title: String,
    val changelog: List<String>,
    val apkUrl: String,
    val apkMirrors: List<String>,
    val sha256: String?,
    val force: Boolean
) {
    fun candidateApkUrls(): List<String> {
        return buildList {
            add(apkUrl)
            addAll(apkMirrors)
        }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
}

data class DownloadResult(
    val file: File,
    val fromUrl: String
)

class AppUpdateClient(private val context: Context) {
    private val packageInfo: PackageInfo by lazy {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }

    private val currentVersionCode: Long by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    suspend fun fetchLatestUpdate(): AppUpdateInfo? {
        val sources = context.resources.getStringArray(R.array.update_manifest_urls)
        var lastError: Throwable? = null
        for (source in sources) {
            runCatching {
                return parseManifest(fetchText(source))
            }.onFailure {
                lastError = it
            }
        }
        lastError?.printStackTrace()
        return null
    }

    fun hasNewVersion(info: AppUpdateInfo): Boolean {
        return info.versionCode > currentVersionCode
    }

    suspend fun downloadApk(
        info: AppUpdateInfo,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): DownloadResult {
        val cacheDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(cacheDir, buildApkFileName(info)).apply {
            if (exists()) delete()
        }

        var lastError: Throwable? = null
        for (url in info.candidateApkUrls()) {
            runCatching {
                val result = downloadFromUrl(url, target, onProgress)
                if (!info.sha256.isNullOrBlank()) {
                    val checksum = sha256Of(target)
                    require(checksum.equals(info.sha256, ignoreCase = true)) { "APK 校验失败" }
                }
                return result
            }.onFailure {
                lastError = it
                if (target.exists()) target.delete()
            }
        }
        throw lastError ?: IllegalStateException("下载失败")
    }

    fun canInstallUnknownApps(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    fun buildUnknownAppSourcesIntent(): Intent {
        return Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun buildInstallIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun parseManifest(raw: String): AppUpdateInfo {
        val json = JSONObject(raw)
        val mirrorsJson = json.optJSONArray("apkMirrors")
            ?: json.optJSONArray("downloadMirrors")
            ?: JSONArray()
        val mirrors = buildList {
            for (i in 0 until mirrorsJson.length()) {
                add(mirrorsJson.optString(i))
            }
        }
        val changelogJson = json.optJSONArray("changelog")
            ?: json.optJSONArray("notes")
            ?: JSONArray()
        val changelog = buildList {
            for (i in 0 until changelogJson.length()) {
                add(changelogJson.optString(i))
            }
        }
        return AppUpdateInfo(
            versionCode = json.optLong("versionCode", 0L),
            versionName = json.optString("versionName", ""),
            title = json.optString("title", ""),
            changelog = changelog,
            apkUrl = json.optString("apkUrl", ""),
            apkMirrors = mirrors,
            sha256 = json.optString("sha256", null),
            force = json.optBoolean("force", false)
        )
    }

    private fun fetchText(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        connection.connect()
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("HTTP $responseCode")
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun downloadFromUrl(
        url: String,
        target: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit
    ): DownloadResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 20_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        connection.connect()
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("HTTP $responseCode")
        }
        val totalBytes = connection.contentLengthLong.coerceAtLeast(0L)
        connection.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded, totalBytes)
                }
                output.flush()
            }
        }
        return DownloadResult(file = target, fromUrl = url)
    }

    private fun buildApkFileName(info: AppUpdateInfo): String {
        val safeVersion = info.versionName.ifBlank { info.versionCode.toString() }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "ledger_update_$safeVersion.apk"
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
