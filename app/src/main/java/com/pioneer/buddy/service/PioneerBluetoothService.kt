package com.pioneer.buddy.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pioneer.buddy.MainActivity
import com.pioneer.buddy.R
import com.pioneer.buddy.model.ConnectionStatus
import com.pioneer.buddy.protocol.CrpConstants
import com.pioneer.buddy.protocol.OutgoingPackets
import com.pioneer.buddy.protocol.PacketUtil
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

class PioneerBluetoothService : Service() {

    private val TAG = "PioneerBTService"
    private val CHANNEL_ID = "pioneer_buddy_channel"
    private val NOTIF_ID = 101

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null
    private val isRunning = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, createNotification("Инициализация сервиса..."))

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = btManager?.adapter

        PioneerConnectionManager.registerSender { frame ->
            connectedThread?.write(frame)
        }

        PioneerConnectionManager.registerKeyDispatcher { keyCode, statusCode ->
            KeyActionDispatcher.dispatchKey(this@PioneerBluetoothService, keyCode, statusCode)
        }

        startServer()
    }

    private fun startServer() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "Bluetooth не включен или недоступен")
            PioneerConnectionManager.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            return
        }

        stopThreads()
        isRunning.set(true)
        acceptThread = AcceptThread().apply { start() }
        PioneerConnectionManager.setConnectionStatus(ConnectionStatus.LISTENING)
        updateNotification("Ожидание подключения к Pioneer SPH-10BT")
    }

    private fun stopThreads() {
        acceptThread?.cancel()
        acceptThread = null
        connectedThread?.cancel()
        connectedThread = null
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread("Pioneer-AcceptThread") {
        private var serverSocket: BluetoothServerSocket? = null

        init {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    CrpConstants.SERVICE_NAME,
                    CrpConstants.UUID_SPP
                )
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка открытия BluetoothServerSocket", e)
            }
        }

        override fun run() {
            Log.i(TAG, "RFCOMM Сервер запущен. Слушаем UUID: ${CrpConstants.UUID_SPP}")
            while (isRunning.get()) {
                val socket: BluetoothSocket? = try {
                    serverSocket?.accept()
                } catch (e: Exception) {
                    if (isRunning.get()) Log.w(TAG, "Ошибка accept()", e)
                    null
                }

                if (socket != null) {
                    Log.i(TAG, "SPH-10BT подключилась: ${socket.remoteDevice?.name}")
                    manageConnectedSocket(socket)
                    break
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка закрытия serverSocket", e)
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        connectedThread = ConnectedThread(socket).apply { start() }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread("Pioneer-ConnectedThread") {
        private val inStream: InputStream = socket.inputStream
        private val outStream: OutputStream = socket.outputStream
        private val isConnected = AtomicBoolean(true)

        override fun run() {
            PioneerConnectionManager.setConnectionStatus(ConnectionStatus.CONNECTED, socket.remoteDevice?.name ?: "SPH-10BT")
            updateNotification("SPH-10BT подключена (${socket.remoteDevice?.name})")

            // Handshake процедура
            try {
                sleep(1500)
                write(OutgoingPackets.startInitialAuth())
                write(OutgoingPackets.classIdRequest())
                write(OutgoingPackets.protocolVersionRequest())
                write(OutgoingPackets.protocolVersionNotification(4, 8))
                write(OutgoingPackets.endInitialAuth())
                Log.i(TAG, "Handshake с SPH-10BT успешно отправлен")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка рукопожатия", e)
            }

            val buffer = ByteArray(1024)
            while (isConnected.get()) {
                try {
                    val bytesRead = inStream.read(buffer)
                    if (bytesRead == -1) break
                    handleIncomingRawData(buffer, bytesRead)
                } catch (e: Exception) {
                    Log.w(TAG, "Соединение разорвано", e)
                    break
                }
            }

            PioneerConnectionManager.setConnectionStatus(ConnectionStatus.DISCONNECTED)
            updateNotification("Магнитола отключена. Повторный запуск сервера...")
            if (isRunning.get()) {
                startServer()
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outStream.write(bytes)
                outStream.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка отправки пакета", e)
            }
        }

        fun cancel() {
            isConnected.set(false)
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка закрытия сокета", e)
            }
        }
    }

    private fun handleIncomingRawData(data: ByteArray, length: Int) {
        // Простой детектор входящих событий клавиш (ID = 0x21)
        for (i in 0 until length - 4) {
            // Ищем заголовок 0x9F 0x02
            if (data[i] == CrpConstants.DLE && data[i + 1] == CrpConstants.STX) {
                val id = PacketUtil.ubyteToInt(data[i + 2])
                val type = PacketUtil.ubyteToInt(data[i + 3])

                // 0x21 (33) -> SMART_PHONE_CONTROL_COMMAND_NOTIFICATION (аппаратные кнопки)
                if (id == 0x21 && type == 0x00) {
                    val d0 = if (i + 4 < length) PacketUtil.ubyteToInt(data[i + 4]) else -1
                    if (d0 == 1 && i + 6 < length) {
                        val keyCode = PacketUtil.ubyteToInt(data[i + 5])
                        val statusCode = PacketUtil.ubyteToInt(data[i + 6])
                        Log.i(TAG, "Нажата кнопка на панели магнитолы: KeyCode=$keyCode, Status=$statusCode")
                        PioneerConnectionManager.handleIncomingHardwareKey(keyCode, statusCode)
                    }
                }

                // 0x21 (33) -> SMART_PHONE_MEDIA_COMMAND (Seek / Play / Pause)
                if (id == 0x21 && type == 0x01) {
                    val mediaCmd = if (i + 4 < length) PacketUtil.ubyteToInt(data[i + 4]) else -1
                    if (mediaCmd != -1) {
                        Log.i(TAG, "Медиа-команда магнитолы: MediaCode=$mediaCmd")
                        PioneerConnectionManager.handleIncomingMediaCommand(mediaCmd)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pioneer Buddy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновый обмен данными с магнитолой Pioneer SPH-10BT"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pioneer Buddy")
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, createNotification(content))
    }

    override fun onDestroy() {
        isRunning.set(false)
        stopThreads()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
