package com.pioneer.buddy.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.pioneer.buddy.protocol.DeviceControlCommand
import kotlinx.coroutines.*

object KeyActionDispatcher {

    private const val TAG = "KeyActionDispatcher"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var marqueeJob: Job? = null
    private var volumeRampJob: Job? = null

    fun dispatchKey(context: Context, keyCode: Int, statusCode: Int) {
        val remapper = PioneerSettingsManager.remapperState.value
        Log.i(TAG, "Обработка аппаратной клавиши: keyCode=$keyCode, statusCode=$statusCode")

        when (keyCode) {
            2 -> { // NAVI
                val action = if (statusCode == 0) remapper.navShortAction else remapper.navLongAction
                executeAction(context, action)
            }
            5 -> { // MAIL / MSG
                val action = if (statusCode == 0) remapper.msgShortAction else remapper.msgLongAction
                executeAction(context, action)
            }
            3 -> { // PHONE
                val action = if (statusCode == 0) remapper.telShortAction else remapper.telLongAction
                executeAction(context, action)
            }
            6 -> { // VR / MIC
                val action = if (statusCode == 0) remapper.micShortAction else remapper.micLongAction
                executeAction(context, action)
            }
            1 -> { // MUSIC
                PioneerConnectionManager.sendDeviceCommand(DeviceControlCommand.PLAY_PAUSE)
            }
            0 -> { // BACK / SRC
                PioneerConnectionManager.toggleSource()
            }
            else -> {
                Log.w(TAG, "Неизвестный код клавиши: $keyCode")
            }
        }
    }

    fun executeAction(context: Context, actionName: String) {
        Log.i(TAG, "Выполнение действия: $actionName")
        try {
            when (actionName) {
                "Яндекс Навигатор" -> launchApp(context, "ru.yandex.yandexnavi", "geo:0,0")
                "2ГИС" -> launchApp(context, "ru.dublgis.dgismobile", "geo:0,0")
                "Google Карты" -> launchApp(context, "com.google.android.apps.maps", "geo:0,0")
                "Озвучить сообщения (TTS)" -> SmartNotificationListenerService.speakLastMessage()
                "Открыть Telegram" -> launchApp(context, "org.telegram.messenger")
                "Открыть WhatsApp" -> launchApp(context, "com.whatsapp")
                "Контакты / Вызовы" -> {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                "Быстрый вызов (Избранное)" -> {
                    val intent = Intent(Intent.ACTION_CALL_BUTTON).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                "Голосовой ассистент" -> {
                    val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                "MUTE / ATT" -> {
                    PioneerConnectionManager.sendDeviceCommand(DeviceControlCommand.MUTE)
                }
                "Поставить Лайк ❤️" -> {
                    SmartNotificationListenerService.triggerLikeAction()
                }
                "Следующий трек" -> {
                    PioneerConnectionManager.sendDeviceCommand(DeviceControlCommand.SEEK_UP)
                }
                else -> {
                    Log.w(TAG, "Действие \"$actionName\" не распознано")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка выполнения действия $actionName", e)
        }
    }

    private fun launchApp(context: Context, packageName: String, fallbackUri: String? = null) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else if (fallbackUri != null) {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } else {
            Log.w(TAG, "Приложение $packageName не найдено")
        }
    }

    // --- Volume Ramping Engine ---
    fun startVolumeRamping(isUp: Boolean) {
        if (!PioneerSettingsManager.remapperState.value.continuousVolumeRamping) {
            PioneerConnectionManager.setVolumeStep(isUp)
            return
        }
        stopVolumeRamping()
        volumeRampJob = scope.launch {
            while (isActive) {
                PioneerConnectionManager.setVolumeStep(isUp)
                delay(120) // Пошаговый импульс каждые 120 мс
            }
        }
    }

    fun stopVolumeRamping() {
        volumeRampJob?.cancel()
        volumeRampJob = null
    }

    // --- Infinite Display Marquee Engine ---
    fun startMarqueeLoop(title: String, artist: String) {
        marqueeJob?.cancel()
        if (!PioneerSettingsManager.remapperState.value.infiniteMarqueeScroll) return

        marqueeJob = scope.launch {
            while (isActive) {
                PioneerConnectionManager.sendAudioInfo(title, artist)
                // Циклическое обновление дисплея магнитолы раз в 4 секунды, чтобы текст не замирал
                delay(4000)
            }
        }
    }

    fun stopMarqueeLoop() {
        marqueeJob?.cancel()
        marqueeJob = null
    }
}
