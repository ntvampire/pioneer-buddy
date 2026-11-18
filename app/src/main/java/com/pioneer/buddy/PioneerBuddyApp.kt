package com.pioneer.buddy

import android.app.Application
import android.content.Intent
import android.os.Build
import com.pioneer.buddy.service.PioneerBluetoothService

class PioneerBuddyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Запуск фонового сервиса связи с магнитолой
        val serviceIntent = Intent(this, PioneerBluetoothService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}
