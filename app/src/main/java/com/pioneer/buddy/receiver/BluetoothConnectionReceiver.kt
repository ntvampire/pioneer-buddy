package com.pioneer.buddy.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pioneer.buddy.service.PioneerBluetoothService

class BluetoothConnectionReceiver : BroadcastReceiver() {

    private val TAG = "PioneerBTReceiver"

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Получено системное событие: $action")

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                val deviceName = device?.name ?: ""
                Log.i(TAG, "Bluetooth подключение к устройству: $deviceName (${device?.address})")

                if (deviceName.contains("SPH-10BT", ignoreCase = true) || deviceName.contains("Pioneer", ignoreCase = true)) {
                    Log.i(TAG, "Обнаружена магнитола Pioneer SPH-10BT. Активация PioneerBluetoothService...")
                    startPioneerService(context)
                }
            }
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    Log.i(TAG, "Bluetooth адаптер включен. Пробуждение сервиса Pioneer...")
                    startPioneerService(context)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Устройство загружено. Запуск PioneerBluetoothService...")
                startPioneerService(context)
            }
        }
    }

    private fun startPioneerService(context: Context) {
        try {
            val serviceIntent = Intent(context, PioneerBluetoothService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка запуска PioneerBluetoothService", e)
        }
    }
}
