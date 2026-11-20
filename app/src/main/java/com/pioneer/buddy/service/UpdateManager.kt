package com.pioneer.buddy.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.pioneer.buddy.BuildConfig
import com.pioneer.buddy.model.UpdateInfo
import com.pioneer.buddy.model.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val TAG = "UpdateManager"
    const val MANIFEST_URL = "https://ntvampire.github.io/pioneer-buddy/update.json"
    const val RELEASES_PAGE_URL = "https://github.com/ntvampire/pioneer-buddy/releases"

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun checkForUpdates(manual: Boolean = false) {
        if (_updateState.value is UpdateState.Checking || _updateState.value is UpdateState.Downloading) {
            return
        }

        _updateState.value = UpdateState.Checking
        scope.launch {
            try {
                val url = URL(MANIFEST_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "PioneerBuddy-Updater")
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonText = reader.readText()
                    reader.close()

                    val json = JSONObject(jsonText)
                    val remoteCode = json.getInt("versionCode")
                    val remoteName = json.getString("versionName")
                    val apkUrl = json.getString("apkUrl")
                    val notes = json.optString("releaseNotes", "")
                    val sha256 = json.optString("sha256", "")

                    val updateInfo = UpdateInfo(
                        versionCode = remoteCode,
                        versionName = remoteName,
                        apkUrl = apkUrl,
                        releaseNotes = notes,
                        sha256 = sha256
                    )

                    val currentCode = BuildConfig.VERSION_CODE
                    Log.i(TAG, "Проверка обновлений: текущая версия=$currentCode, на сервере=$remoteCode ($remoteName)")

                    if (remoteCode > currentCode) {
                        _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                    } else {
                        _updateState.value = if (manual) UpdateState.UpToDate else UpdateState.Idle
                    }
                } else {
                    Log.w(TAG, "Ошибка проверки манифеста: HTTP ${connection.responseCode}")
                    _updateState.value = UpdateState.Error("Сервер вернул код ${connection.responseCode}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка сети при проверке обновления", e)
                _updateState.value = UpdateState.Error("Сетевая ошибка: ${e.localizedMessage ?: "тайм-аут"}")
            }
        }
    }

    fun startDownload(context: Context, info: UpdateInfo) {
        val intent = Intent(context, UpdateDownloadService::class.java).apply {
            putExtra(UpdateDownloadService.EXTRA_URL, info.apkUrl)
            putExtra(UpdateDownloadService.EXTRA_SHA256, info.sha256)
            putExtra(UpdateDownloadService.EXTRA_VERSION_NAME, info.versionName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun updateProgress(percent: Int) {
        _updateState.value = UpdateState.Downloading(percent)
    }

    fun setReadyToInstall(apkFile: File, info: UpdateInfo) {
        _updateState.value = UpdateState.ReadyToInstall(apkFile, info)
    }

    fun setError(message: String) {
        _updateState.value = UpdateState.Error(message)
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            _updateState.value = UpdateState.Error("Файл APK не найден")
            return
        }

        // Проверка разрешения на установку пакетов (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }
        }

        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updateprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска установщика через FileProvider, открываем браузер", e)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }
}
