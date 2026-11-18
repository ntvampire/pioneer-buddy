package com.pioneer.buddy.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SmartNotificationListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private val TAG = "PioneerNotifListener"
    private var tts: TextToSpeech? = null
    private var lastMessageText: String = ""
    private var lastSender: String = ""

    companion object {
        var instance: SmartNotificationListenerService? = null
            private set

        var currentLikeIntent: PendingIntent? = null
            private set

        fun triggerLikeAction() {
            try {
                currentLikeIntent?.send()
                Log.i("PioneerNotifListener", "Системный лайк отправлен в активный плеер")
            } catch (e: Exception) {
                Log.e("PioneerNotifListener", "Ошибка отправки лайка", e)
            }
        }

        fun speakLastMessage() {
            instance?.speakLastMessageInternal()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ru", "RU")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName
        val extras = sbn.notification.extras

        // 1. Проверяем, является ли уведомление медиаплеером
        val isMedia = sbn.notification.actions?.any { action ->
            action.title?.toString()?.contains("like", ignoreCase = true) == true ||
            action.title?.toString()?.contains("нравится", ignoreCase = true) == true ||
            action.title?.toString()?.contains("избран", ignoreCase = true) == true
        } == true || extras.containsKey(Notification.EXTRA_MEDIA_SESSION)

        if (isMedia) {
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Ищем действие "Лайк" в кнопках шторки
            sbn.notification.actions?.forEach { action ->
                val actionTitle = action.title?.toString()?.lowercase() ?: ""
                if (actionTitle.contains("like") || actionTitle.contains("нравится") || actionTitle.contains("избран")) {
                    currentLikeIntent = action.actionIntent
                }
            }

            if (title.isNotBlank()) {
                PioneerConnectionManager.updateMediaInfo(
                    title = title,
                    artist = artist,
                    isPlaying = true,
                    activeApp = packageName,
                    isLiked = false
                )
            }
        }

        // 2. Проверяем входящие сообщения из мессенджеров (MAX, Telegram, WhatsApp, SMS, VK)
        val isMessenger = packageName.contains("telegram", ignoreCase = true) ||
                          packageName.contains("whatsapp", ignoreCase = true) ||
                          packageName.contains("max", ignoreCase = true) ||
                          packageName.contains("mms", ignoreCase = true) ||
                          packageName.contains("vkontakte", ignoreCase = true)

        if (isMessenger) {
            val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Новое сообщение"
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            if (text.isNotBlank()) {
                lastSender = sender
                lastMessageText = text
            }
        }
    }

    private fun speakLastMessageInternal() {
        if (lastMessageText.isNotBlank()) {
            val phrase = "Сообщение от $lastSender: $lastMessageText"
            tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "PIONEER_MSG")
        } else {
            tts?.speak("Новых сообщений нет", TextToSpeech.QUEUE_FLUSH, null, "PIONEER_MSG")
        }
    }

    override fun onDestroy() {
        instance = null
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
