package com.pioneer.buddy.service

import com.pioneer.buddy.model.ConnectionStatus
import com.pioneer.buddy.model.DashboardState
import com.pioneer.buddy.model.MediaSource
import com.pioneer.buddy.protocol.DeviceControlCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object PioneerConnectionManager {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private var packetSender: ((ByteArray) -> Unit)? = null
    private var customKeyDispatcher: ((Int, Int) -> Unit)? = null

    fun registerSender(sender: (ByteArray) -> Unit) {
        packetSender = sender
    }

    fun registerKeyDispatcher(dispatcher: (Int, Int) -> Unit) {
        customKeyDispatcher = dispatcher
    }

    fun setConnectionStatus(status: ConnectionStatus, deviceName: String = "Pioneer SPH-10BT") {
        _dashboardState.update { it.copy(connectionStatus = status, deviceName = deviceName) }
    }

    fun updateSpeed(speed: Int) {
        _dashboardState.update { it.copy(speedKmH = speed) }
    }

    fun updateMediaInfo(
        title: String,
        artist: String,
        isPlaying: Boolean,
        activeApp: String,
        isLiked: Boolean,
        positionMs: Long = 0,
        durationMs: Long = 0
    ) {
        _dashboardState.update {
            it.copy(
                trackTitle = title,
                artistName = artist,
                isPlaying = isPlaying,
                activePlayerName = activeApp,
                isLiked = isLiked,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    fun updateRadioInfo(freq: String, station: String, text: String, currentPreset: Int) {
        _dashboardState.update {
            it.copy(
                radioFrequency = freq,
                stationName = station,
                radioText = text,
                currentPreset = currentPreset,
                currentSource = MediaSource.TUNER_FM
            )
        }
    }

    fun setPreset(presetIndex: Int) {
        if (presetIndex in 1..6) {
            val cmd = when (presetIndex) {
                1 -> DeviceControlCommand.PRESET_1
                2 -> DeviceControlCommand.PRESET_2
                3 -> DeviceControlCommand.PRESET_3
                4 -> DeviceControlCommand.PRESET_4
                5 -> DeviceControlCommand.PRESET_5
                6 -> DeviceControlCommand.PRESET_6
                else -> DeviceControlCommand.PRESET_1
            }
            sendDeviceCommand(cmd)
            _dashboardState.update { it.copy(currentPreset = presetIndex) }
        }
    }

    fun sendDeviceCommand(cmd: DeviceControlCommand) {
        packetSender?.invoke(cmd.toFrame())
    }

    fun sendRawPacket(frame: ByteArray) {
        packetSender?.invoke(frame)
    }

    fun sendAudioInfo(title: String, artist: String) {
        sendRawPacket(com.pioneer.buddy.protocol.OutgoingPackets.buildAudioInfo(title, artist))
    }

    fun handleIncomingHardwareKey(keyCode: Int, statusCode: Int) {
        customKeyDispatcher?.invoke(keyCode, statusCode)
    }

    fun handleIncomingMediaCommand(cmdCode: Int) {
        when (cmdCode) {
            0 -> sendDeviceCommand(DeviceControlCommand.PLAY_PAUSE)
            3 -> sendDeviceCommand(DeviceControlCommand.SEEK_UP)
            4 -> sendDeviceCommand(DeviceControlCommand.CROSS_LEFT)
            else -> {}
        }
    }

    fun toggleSource() {
        sendDeviceCommand(DeviceControlCommand.SOURCE)
        _dashboardState.update {
            val nextSource = if (it.currentSource == MediaSource.BLUETOOTH_MUSIC) {
                MediaSource.TUNER_FM
            } else {
                MediaSource.BLUETOOTH_MUSIC
            }
            it.copy(currentSource = nextSource)
        }
    }

    fun setVolumeStep(isUp: Boolean) {
        val cmd = if (isUp) DeviceControlCommand.VOLUME_UP else DeviceControlCommand.VOLUME_DOWN
        sendDeviceCommand(cmd)
        _dashboardState.update {
            val newVol = if (isUp) (it.volume + 1).coerceAtMost(62) else (it.volume - 1).coerceAtLeast(0)
            it.copy(volume = newVol)
        }
    }
}
