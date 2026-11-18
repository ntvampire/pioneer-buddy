package com.pioneer.buddy.protocol

import com.pioneer.buddy.model.*

enum class HardwareKey(val code: Int) {
    BACK(0),
    MUSIC(1),
    NAVI(2),
    PHONE(3),
    MESSAGE(5),
    VOICE(6),
    APP(7);

    companion object {
        fun fromCode(code: Int): HardwareKey? = entries.firstOrNull { it.code == code }
    }
}

enum class KeyPressType(val code: Int) {
    SHORT(0),
    LONG(1);

    companion object {
        fun fromCode(code: Int): KeyPressType = if (code == 1) LONG else SHORT
    }
}

enum class MediaCommand(val code: Int) {
    PLAY_PAUSE(0),
    PLAY_RESUME(1),
    PAUSE(2),
    TRACK_UP(3),
    TRACK_DOWN(4),
    FAST_FORWARD(5),
    REWIND(6),
    REPEAT(7),
    RANDOM(8);

    companion object {
        fun fromCode(code: Int): MediaCommand? = entries.firstOrNull { it.code == code }
    }
}

enum class DeviceControlCommand(val code: Int) {
    BAND_ESC(0),
    CROSS_UP(2),     // Preset Up
    CROSS_DOWN(3),   // Preset Down
    CROSS_RIGHT(4),  // Manual Tune Up
    CROSS_LEFT(5),   // Manual Tune Down
    VOLUME_UP(6),
    VOLUME_DOWN(7),
    SOURCE(8),
    MUTE(14),
    ATT(16),
    VOICE_START(18),
    PRESET_1(64),
    PRESET_2(65),
    PRESET_3(66),
    PRESET_4(67),
    PRESET_5(68),
    PRESET_6(69),
    SEEK_UP(128),
    PLAY_PAUSE(131);

    fun toFrame(): ByteArray {
        // DEVICE_CONTROL_COMMAND: ID = 0x20, TYPE = 0, DATA = [0, code]
        return PacketUtil.buildFrame(0x20, 0, byteArrayOf(0, code.toByte()))
    }
}

object OutgoingPackets {
    // Initial Handshake
    fun startInitialAuth(): ByteArray =
        PacketUtil.buildFrame(0x00, 0x00, byteArrayOf(0x00))

    fun classIdRequest(): ByteArray =
        PacketUtil.buildFrame(0x00, 0x01, byteArrayOf(0x80.toByte()))

    fun protocolVersionRequest(): ByteArray =
        PacketUtil.buildFrame(0x00, 0x02, byteArrayOf(0x80.toByte()))

    fun protocolVersionNotification(major: Byte = 4, minor: Byte = 8): ByteArray =
        PacketUtil.buildFrame(0x00, 0x02, byteArrayOf(0x00, major, minor))

    fun endInitialAuth(): ByteArray =
        PacketUtil.buildFrame(0x00, 0x00, byteArrayOf(0x01))

    // Send Title/Artist to SPH-10BT 1-line segment display
    fun buildAudioInfo(title: String, artist: String): ByteArray {
        val displayStr = if (artist.isNotBlank()) "$title - $artist" else title
        val utf8Bytes = displayStr.take(64).toByteArray(Charsets.UTF_8)
        val data = ByteArray(3 + utf8Bytes.size)
        data[0] = 0 // D0
        data[1] = 1 // APP_MUSIC source
        data[2] = 1 // Song Title type
        System.arraycopy(utf8Bytes, 0, data, 3, utf8Bytes.size)
        return PacketUtil.buildFrame(0x36, 0x00, data)
    }

    // =========================================================================
    // 1. АУДИО НАСТРОЙКИ (PACKET ID = 0x22, TYPE = 1)
    // =========================================================================

    fun buildEqPreset(preset: PioneerEqPreset): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(0, preset.code.toByte()))

    fun buildCustomEq(customEqIndex: Int, bands: IntArray): ByteArray {
        val data = ByteArray(15)
        data[0] = 1 // D0
        data[1] = customEqIndex.toByte() // 0 = Custom 1, 1 = Custom 2
        for (i in 0 until 13.coerceAtMost(bands.size)) {
            data[2 + i] = bands[i].coerceIn(-12, 12).toByte()
        }
        return PacketUtil.buildFrame(0x22, 1, data)
    }

    fun buildFaderBalance(fader: Int, balance: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(2, fader.coerceIn(-25, 25).toByte(), balance.coerceIn(-25, 25).toByte()))

    fun buildSubwooferEnable(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(3, if (enabled) 1 else 0))

    fun buildSubwooferPhase(phase: SubwooferPhase): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(4, phase.code.toByte()))

    fun buildSpeakerLevel(speakerIndex: Int, levelDb: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(5, speakerIndex.toByte(), levelDb.coerceIn(-24, 10).toByte()))

    fun buildCrossoverHpfLpf(speakerType: Int, enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(6, speakerType.toByte(), if (enabled) 1 else 0))

    fun buildCrossoverCutoff(speakerType: Int, cutoff: StandardCutoff): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(7, speakerType.toByte(), cutoff.code.toByte()))

    fun buildCrossoverSlope(speakerType: Int, slope: StandardSlope): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(8, speakerType.toByte(), slope.code.toByte()))

    fun buildListeningPosition(position: ListeningPosition): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(9, position.code.toByte()))

    fun buildTimeAlignmentDistance(speakerIndex: Int, distanceSteps: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(10, speakerIndex.toByte(), distanceSteps.coerceIn(0, 140).toByte()))

    fun buildBassBoosterLevel(level: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(11, level.coerceIn(0, 6).toByte()))

    fun buildLoudness(setting: LoudnessSetting): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(12, setting.code.toByte()))

    fun buildSourceLevelAdjuster(slaStep: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(15, slaStep.coerceIn(-4, 4).toByte()))

    fun buildSoundRetriever(setting: SoundRetrieverSetting): ByteArray =
        PacketUtil.buildFrame(0x22, 1, byteArrayOf(18, setting.code.toByte()))

    // =========================================================================
    // 2. СИСТЕМНЫЕ НАСТРОЙКИ (PACKET ID = 0x22, TYPE = 0)
    // =========================================================================

    fun buildBeepTone(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(0, if (enabled) 1 else 0))

    fun buildAttMute(setting: AttMuteSetting): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(1, setting.code.toByte()))

    fun buildAutoPi(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(2, if (enabled) 1 else 0))

    fun buildPowerSave(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(3, if (enabled) 1 else 0))

    fun buildBtAudioSource(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(4, if (enabled) 1 else 0))

    fun buildAuxSource(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(7, if (enabled) 1 else 0))

    fun buildAppAutoStart(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(8, if (enabled) 1 else 0))

    fun buildUsbAuto(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(9, if (enabled) 1 else 0))

    fun buildDisplayOff(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(10, if (enabled) 1 else 0))

    fun buildDistanceUnit(unit: DistanceUnit): ByteArray =
        PacketUtil.buildFrame(0x22, 0, byteArrayOf(11, unit.code.toByte()))

    // =========================================================================
    // 3. ПОДСВЕТКА И ДИММЕР (PACKET ID = 0x22, TYPE = 2)
    // =========================================================================

    fun buildPresetColor(zone: Int, color: PioneerPresetColor): ByteArray =
        PacketUtil.buildFrame(0x22, 2, byteArrayOf(0, zone.toByte(), color.code.toByte()))

    fun buildCustomColor(zone: Int, red: Int, green: Int, blue: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 2, byteArrayOf(1, zone.toByte(), red.coerceIn(0, 60).toByte(), green.coerceIn(0, 60).toByte(), blue.coerceIn(0, 60).toByte()))

    fun buildDimmerSetting(mode: DimmerMode): ByteArray =
        PacketUtil.buildFrame(0x22, 2, byteArrayOf(2, mode.code.toByte()))

    fun buildBrightness(zone: Int, level: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 2, byteArrayOf(3, zone.toByte(), level.coerceIn(1, 10).toByte()))

    fun buildAudioLevelMeter(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 2, byteArrayOf(8, if (enabled) 1 else 0))

    // =========================================================================
    // 4. ИНИЦИАЛИЗАЦИЯ И ШАГ ТЮНЕРА (PACKET ID = 0x22, TYPE = 3)
    // =========================================================================

    fun buildFmStep(step: FmStep): ByteArray =
        PacketUtil.buildFrame(0x22, 3, byteArrayOf(0, step.code.toByte()))

    fun buildAmStep(step: AmStep): ByteArray =
        PacketUtil.buildFrame(0x22, 3, byteArrayOf(1, step.code.toByte()))

    fun buildRearOutputPreout(setting: RearPreoutSetting): ByteArray =
        PacketUtil.buildFrame(0x22, 3, byteArrayOf(2, setting.code.toByte()))

    // =========================================================================
    // 5. ПАРКТРОНИК PIONEER ND-PS1 (PACKET ID = 0x22, TYPE = 4)
    // =========================================================================

    fun buildParkingSensor(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 4, byteArrayOf(0, if (enabled) 1 else 0))

    fun buildAlarmVolume(volume: Int): ByteArray =
        PacketUtil.buildFrame(0x22, 4, byteArrayOf(1, volume.coerceIn(1, 3).toByte()))

    fun buildAlarmOutputDestination(dest: AlarmOutput): ByteArray =
        PacketUtil.buildFrame(0x22, 4, byteArrayOf(2, dest.code.toByte()))

    fun buildBackPolarity(polarity: BackPolarity): ByteArray =
        PacketUtil.buildFrame(0x22, 4, byteArrayOf(3, polarity.code.toByte()))

    // =========================================================================
    // 6. ФУНКЦИИ РАДИО ТЮНЕРА (PACKET ID = 0x22, TYPE = 5)
    // =========================================================================

    fun buildBsm(): ByteArray =
        PacketUtil.buildFrame(0x22, 5, byteArrayOf(0, 1))

    fun buildLocalSeek(level: LocalSeekLevel): ByteArray =
        PacketUtil.buildFrame(0x22, 5, byteArrayOf(1, level.code.toByte()))

    fun buildTa(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 5, byteArrayOf(2, if (enabled) 1 else 0))

    fun buildAf(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 5, byteArrayOf(3, if (enabled) 1 else 0))

    fun buildReg(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 5, byteArrayOf(4, if (enabled) 1 else 0))

    fun buildNews(enabled: Boolean): ByteArray =
        PacketUtil.buildFrame(0x22, 5, byteArrayOf(5, if (enabled) 1 else 0))
}
