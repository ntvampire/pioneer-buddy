package com.pioneer.buddy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pioneer.buddy.MainActivity
import com.pioneer.buddy.R
import com.pioneer.buddy.model.UpdateInfo
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateDownloadService : Service() {

    private val TAG = "UpdateDownloadService"
    private val CHANNEL_ID = "pioneer_updates_channel"
    private val NOTIF_ID = 202

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val EXTRA_URL = "extra_apk_url"
        const val EXTRA_SHA256 = "extra_sha256"
        const val EXTRA_VERSION_NAME = "extra_version_name"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PioneerBuddy:UpdateDownload")
        wakeLock?.acquire(10 * 60 * 1000L) // 10 минут макс.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val apkUrl = intent?.getStringExtra(EXTRA_URL)
        val expectedSha = intent?.getStringExtra(EXTRA_SHA256) ?: ""
        val versionName = intent?.getStringExtra(EXTRA_VERSION_NAME) ?: "update"

        if (apkUrl.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, createProgressNotification(0, "Подключение..."))

        serviceScope.launch {
            downloadApk(apkUrl, expectedSha, versionName)
        }

        return START_NOT_STICKY
    }

    private suspend fun downloadApk(apkUrl: String, expectedSha: String, versionName: String) {
        val updatesDir = File(cacheDir, "updates")
        if (!updatesDir.exists()) {
            updatesDir.mkdirs()
        }
        val targetApk = File(updatesDir, "PioneerBuddy-$versionName.apk")

        try {
            val url = URL(apkUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "PioneerBuddy-Updater")
                setRequestProperty("Accept-Encoding", "identity") // Гарантирует точный Content-Length
            }

            val responseCode = connection.responseCode
            // Поддержка HTTP-редиректов (GitHub Releases редиректит на CDN)
            val finalConnection = if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == 307 || responseCode == 308
            ) {
                val newUrl = connection.getHeaderField("Location")
                (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "PioneerBuddy-Updater")
                    setRequestProperty("Accept-Encoding", "identity")
                }
            } else {
                connection
            }

            val totalBytes = finalConnection.contentLength.toLong()
            var downloadedBytes = 0L
            var lastReportedPercent = -1

            val digest = MessageDigest.getInstance("SHA-256")
            val input: InputStream = finalConnection.inputStream
            val output = FileOutputStream(targetApk)

            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                if (totalBytes > 0) {
                    val percent = ((downloadedBytes * 100) / totalBytes).toInt()
                    if (percent != lastReportedPercent) {
                        lastReportedPercent = percent
                        UpdateManager.updateProgress(percent)
                        updateNotification(percent, "$percent% ($downloadedBytes / $totalBytes б)")
                    }
                }
            }

            output.flush()
            output.close()
            input.close()

            // Валидация SHA-256 при наличии хэша в манифесте
            val calculatedSha = digest.digest().joinToString("") { "%02x".format(it) }
            if (expectedSha.isNotBlank() && !calculatedSha.equals(expectedSha, ignoreCase = true)) {
                targetApk.delete()
                val err = "Контрольная сумма SHA-256 не совпала"
                Log.e(TAG, "$err: ожидалось=$expectedSha, получено=$calculatedSha")
                UpdateManager.setError(err)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            Log.i(TAG, "Загрузка завершена успешно: ${targetApk.absolutePath} ($downloadedBytes байт)")
            val info = UpdateInfo(0, versionName, apkUrl, "", expectedSha)
            UpdateManager.setReadyToInstall(targetApk, info)

            // Уведомление о готовности к установке
            showReadyNotification(targetApk)

            // Запускаем установку напрямую
            UpdateManager.installApk(applicationContext, targetApk)

        } catch (e: Exception) {
            Log.e(TAG, "Сбой при скачивании обновления", e)
            targetApk.delete()
            UpdateManager.setError("Ошибка загрузки: ${e.localizedMessage ?: "обрыв связи"}")
        } finally {
            wakeLock?.let { if (it.isHeld) it.release() }
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обновление приложения",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Загрузка новых версий Pioneer Buddy"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createProgressNotification(percent: Int, statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Загрузка обновления Pioneer Buddy")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, percent == 0)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(percent: Int, statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, createProgressNotification(percent, statusText))
    }

    private fun showReadyNotification(apkFile: File) {
        val apkUri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "$packageName.updateprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, installIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Обновление готово к установке")
            .setContentText("Нажмите для завершения установки")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
