package com.pioneer.buddy.service

import com.pioneer.buddy.model.*
import com.pioneer.buddy.protocol.OutgoingPackets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AudioSettingsState(
    val eqPreset: PioneerEqPreset = PioneerEqPreset.FLAT,
    val eqBands: Pioneer13BandEq = Pioneer13BandEq(),
    val fader: Int = 0,         // -25 (Rear) .. +25 (Front)
    val balance: Int = 0,       // -25 (Left) .. +25 (Right)
    val subwooferEnabled: Boolean = true,
    val subwooferPhase: SubwooferPhase = SubwooferPhase.NORMAL,
    val subwooferCutoff: StandardCutoff = StandardCutoff._80HZ,
    val subwooferSlope: StandardSlope = StandardSlope._18DB,
    val hpfFrontEnabled: Boolean = false,
    val hpfFrontCutoff: StandardCutoff = StandardCutoff._80HZ,
    val hpfFrontSlope: StandardSlope = StandardSlope._12DB,
    val hpfRearEnabled: Boolean = false,
    val hpfRearCutoff: StandardCutoff = StandardCutoff._80HZ,
    val hpfRearSlope: StandardSlope = StandardSlope._12DB,
    val listeningPosition: ListeningPosition = ListeningPosition.FRONT_LEFT,
    val timeAlignmentFlCm: Float = 50.0f,
    val timeAlignmentFrCm: Float = 90.0f,
    val timeAlignmentRlCm: Float = 110.0f,
    val timeAlignmentRrCm: Float = 130.0f,
    val timeAlignmentSwCm: Float = 150.0f,
    val speakerLevelFlDb: Int = 0,
    val speakerLevelFrDb: Int = 0,
    val speakerLevelRlDb: Int = 0,
    val speakerLevelRrDb: Int = 0,
    val speakerLevelSwDb: Int = 0,
    val loudness: LoudnessSetting = LoudnessSetting.OFF,
    val bassBooster: Int = 0,    // 0 .. 6
    val sla: Int = 0,            // -4 .. +4 dB
    val soundRetriever: SoundRetrieverSetting = SoundRetrieverSetting.OFF
)

data class SystemSettingsState(
    val beepTone: Boolean = false,
    val powerSaveMode: Boolean = false,
    val usbAuto: Boolean = true,
    val appAutoLaunch: Boolean = true,
    val attMute: AttMuteSetting = AttMuteSetting.MUTE,
    val rearPreout: RearPreoutSetting = RearPreoutSetting.REAR_SUBWOOFER,
    val autoPi: Boolean = false,
    val auxEnabled: Boolean = true,
    val btAudioEnabled: Boolean = true,
    val fmStep: FmStep = FmStep._50KHZ,
    val amStep: AmStep = AmStep._9KHZ,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETER
)

data class IlluminationSettingsState(
    val dimmerMode: DimmerMode = DimmerMode.ILLUMI_LINE,
    val keyBrightness: Int = 8,        // 1 .. 10
    val displayBrightness: Int = 10,   // 1 .. 10
    val presetColor: PioneerPresetColor = PioneerPresetColor.TURQUOISE,
    val customRed: Int = 0,            // 0 .. 60
    val customGreen: Int = 60,
    val customBlue: Int = 60,
    val audioLevelMeter: Boolean = false
)

data class ParkingSensorSettingsState(
    val enabled: Boolean = false,
    val alarmVolume: Int = 2,          // 1 .. 3
    val outputDestination: AlarmOutput = AlarmOutput.FRONT,
    val backPolarity: BackPolarity = BackPolarity.BATTERY
)

data class RadioFunctionSettingsState(
    val localSeek: LocalSeekLevel = LocalSeekLevel.OFF,
    val ta: Boolean = false,
    val af: Boolean = false,
    val reg: Boolean = false,
    val news: Boolean = false
)

data class RemapperSettingsState(
    val navShortAction: String = "Яндекс Навигатор",
    val navLongAction: String = "2ГИС",
    val msgShortAction: String = "Озвучить сообщения (TTS)",
    val msgLongAction: String = "Открыть Telegram",
    val telShortAction: String = "Контакты / Вызовы",
    val telLongAction: String = "Быстрый вызов (Избранное)",
    val micShortAction: String = "Голосовой ассистент",
    val micLongAction: String = "MUTE / ATT",
    val continuousVolumeRamping: Boolean = true,
    val infiniteMarqueeScroll: Boolean = true,
    val ttsTelegram: Boolean = true,
    val ttsWhatsApp: Boolean = true,
    val ttsVk: Boolean = true,
    val ttsMax: Boolean = true,
    val ttsSms: Boolean = true
)

object PioneerSettingsManager {

    private var prefs: android.content.SharedPreferences? = null

    private val _audioState = MutableStateFlow(AudioSettingsState())
    val audioState: StateFlow<AudioSettingsState> = _audioState.asStateFlow()

    private val _systemState = MutableStateFlow(SystemSettingsState())
    val systemState: StateFlow<SystemSettingsState> = _systemState.asStateFlow()

    private val _illuminationState = MutableStateFlow(IlluminationSettingsState())
    val illuminationState: StateFlow<IlluminationSettingsState> = _illuminationState.asStateFlow()

    private val _parkingState = MutableStateFlow(ParkingSensorSettingsState())
    val parkingState: StateFlow<ParkingSensorSettingsState> = _parkingState.asStateFlow()

    private val _radioFuncState = MutableStateFlow(RadioFunctionSettingsState())
    val radioFuncState: StateFlow<RadioFunctionSettingsState> = _radioFuncState.asStateFlow()

    private val _remapperState = MutableStateFlow(RemapperSettingsState())
    val remapperState: StateFlow<RemapperSettingsState> = _remapperState.asStateFlow()

    fun init(context: android.content.Context) {
        val sp = context.getSharedPreferences("pioneer_buddy_settings", android.content.Context.MODE_PRIVATE)
        prefs = sp

        // Загрузка сохранённой подсветки
        val colorName = sp.getString("illumination_preset", PioneerPresetColor.TURQUOISE.name)
        val savedColor = try {
            PioneerPresetColor.valueOf(colorName ?: PioneerPresetColor.TURQUOISE.name)
        } catch (e: Exception) {
            PioneerPresetColor.TURQUOISE
        }
        val savedEq = sp.getBoolean("illumination_equalizer", false)
        _illuminationState.update {
            it.copy(presetColor = savedColor, audioLevelMeter = savedEq)
        }

        // Загрузка сохранённого ремаппинга и TTS
        val navShort = sp.getString("nav_short", "Яндекс Навигатор") ?: "Яндекс Навигатор"
        val navLong = sp.getString("nav_long", "2ГИС") ?: "2ГИС"
        val msgShort = sp.getString("msg_short", "Озвучить сообщения (TTS)") ?: "Озвучить сообщения (TTS)"
        val msgLong = sp.getString("msg_long", "Открыть Telegram") ?: "Открыть Telegram"
        val telShort = sp.getString("tel_short", "Контакты / Вызовы") ?: "Контакты / Вызовы"
        val telLong = sp.getString("tel_long", "Быстрый вызов (Избранное)") ?: "Быстрый вызов (Избранное)"
        val micShort = sp.getString("mic_short", "Голосовой ассистент") ?: "Голосовой ассистент"
        val micLong = sp.getString("mic_long", "MUTE / ATT") ?: "MUTE / ATT"
        val ttsTg = sp.getBoolean("tts_tg", true)
        val ttsWa = sp.getBoolean("tts_wa", true)
        val ttsVk = sp.getBoolean("tts_vk", true)
        val ttsMax = sp.getBoolean("tts_max", true)
        val ttsSms = sp.getBoolean("tts_sms", true)

        _remapperState.update {
            it.copy(
                navShortAction = navShort,
                navLongAction = navLong,
                msgShortAction = msgShort,
                msgLongAction = msgLong,
                telShortAction = telShort,
                telLongAction = telLong,
                micShortAction = micShort,
                micLongAction = micLong,
                ttsTelegram = ttsTg,
                ttsWhatsApp = ttsWa,
                ttsVk = ttsVk,
                ttsMax = ttsMax,
                ttsSms = ttsSms
            )
        }
    }

    // --- Audio Setters ---

    fun setEqPreset(preset: PioneerEqPreset) {
        _audioState.update { it.copy(eqPreset = preset) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildEqPreset(preset))
    }

    fun setEqBand(bandIndex: Int, levelDb: Int) {
        val current = _audioState.value.eqBands
        val newBands = when (bandIndex) {
            0 -> current.copy(band50Hz = levelDb)
            1 -> current.copy(band80Hz = levelDb)
            2 -> current.copy(band125Hz = levelDb)
            3 -> current.copy(band200Hz = levelDb)
            4 -> current.copy(band315Hz = levelDb)
            5 -> current.copy(band500Hz = levelDb)
            6 -> current.copy(band800Hz = levelDb)
            7 -> current.copy(band1_25kHz = levelDb)
            8 -> current.copy(band2kHz = levelDb)
            9 -> current.copy(band3_15kHz = levelDb)
            10 -> current.copy(band5kHz = levelDb)
            11 -> current.copy(band8kHz = levelDb)
            12 -> current.copy(band12_5kHz = levelDb)
            else -> current
        }
        _audioState.update { it.copy(eqBands = newBands, eqPreset = PioneerEqPreset.CUSTOM_1) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCustomEq(0, newBands.toArray()))
    }

    fun setFaderBalance(fader: Int, balance: Int) {
        _audioState.update { it.copy(fader = fader, balance = balance) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildFaderBalance(fader, balance))
    }

    fun setSubwoofer(enabled: Boolean, phase: SubwooferPhase, cutoff: StandardCutoff, slope: StandardSlope) {
        _audioState.update { it.copy(subwooferEnabled = enabled, subwooferPhase = phase, subwooferCutoff = cutoff, subwooferSlope = slope) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildSubwooferEnable(enabled))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildSubwooferPhase(phase))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverCutoff(2, cutoff)) // Subwoofer = 2
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverSlope(2, slope))
    }

    fun setHpfFront(enabled: Boolean, cutoff: StandardCutoff, slope: StandardSlope) {
        _audioState.update { it.copy(hpfFrontEnabled = enabled, hpfFrontCutoff = cutoff, hpfFrontSlope = slope) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverHpfLpf(0, enabled)) // Front = 0
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverCutoff(0, cutoff))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverSlope(0, slope))
    }

    fun setHpfRear(enabled: Boolean, cutoff: StandardCutoff, slope: StandardSlope) {
        _audioState.update { it.copy(hpfRearEnabled = enabled, hpfRearCutoff = cutoff, hpfRearSlope = slope) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverHpfLpf(1, enabled)) // Rear = 1
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverCutoff(1, cutoff))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCrossoverSlope(1, slope))
    }

    fun setListeningPosition(position: ListeningPosition) {
        _audioState.update { it.copy(listeningPosition = position) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildListeningPosition(position))
    }

    fun setLoudness(loudness: LoudnessSetting) {
        _audioState.update { it.copy(loudness = loudness) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildLoudness(loudness))
    }

    fun setBassBooster(level: Int) {
        _audioState.update { it.copy(bassBooster = level) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBassBoosterLevel(level))
    }

    fun setSla(slaStep: Int) {
        _audioState.update { it.copy(sla = slaStep) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildSourceLevelAdjuster(slaStep))
    }

    fun setSoundRetriever(setting: SoundRetrieverSetting) {
        _audioState.update { it.copy(soundRetriever = setting) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildSoundRetriever(setting))
    }

    private val _savedProfiles = mutableMapOf<String, AudioSettingsState>(
        "Любимый" to AudioSettingsState(
            eqPreset = PioneerEqPreset.POWERFUL,
            bassBooster = 2,
            loudness = LoudnessSetting.LOW,
            subwooferEnabled = true,
            subwooferCutoff = StandardCutoff._80HZ,
            subwooferSlope = StandardSlope._18DB
        )
    )

    fun getSavedProfileNames(): List<String> = _savedProfiles.keys.toList()

    fun saveAudioProfileLocally(profileName: String = "Любимый"): String {
        // Сохранение текущих настроек DSP строго локально в приложении на телефоне.
        // Никакой записи в память (EEPROM) магнитолы не производится!
        _savedProfiles[profileName] = _audioState.value
        return "Профиль \"$profileName\" сохранен в телефоне"
    }

    fun loadAudioProfileLocally(profileName: String = "Любимый"): String {
        val profile = _savedProfiles[profileName] ?: return "Профиль не найден"
        _audioState.value = profile
        // Применение рабочих параметров сеанса в DSP магнитолы без изменения EEPROM
        setEqPreset(profile.eqPreset)
        if (profile.eqPreset == PioneerEqPreset.CUSTOM_1 || profile.eqPreset == PioneerEqPreset.CUSTOM_2) {
            PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCustomEq(0, profile.eqBands.toArray()))
        }
        setFaderBalance(profile.fader, profile.balance)
        setSubwoofer(profile.subwooferEnabled, profile.subwooferPhase, profile.subwooferCutoff, profile.subwooferSlope)
        setHpfFront(profile.hpfFrontEnabled, profile.hpfFrontCutoff, profile.hpfFrontSlope)
        setHpfRear(profile.hpfRearEnabled, profile.hpfRearCutoff, profile.hpfRearSlope)
        setListeningPosition(profile.listeningPosition)
        setLoudness(profile.loudness)
        setBassBooster(profile.bassBooster)
        setSla(profile.sla)
        setSoundRetriever(profile.soundRetriever)
        return "Профиль \"$profileName\" применён в DSP"
    }

    // --- System Setters ---

    fun setBeepTone(enabled: Boolean) {
        _systemState.update { it.copy(beepTone = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBeepTone(enabled))
    }

    fun setPowerSaveMode(enabled: Boolean) {
        _systemState.update { it.copy(powerSaveMode = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildPowerSave(enabled))
    }

    fun setUsbAuto(enabled: Boolean) {
        _systemState.update { it.copy(usbAuto = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildUsbAuto(enabled))
    }

    fun setAppAutoLaunch(enabled: Boolean) {
        _systemState.update { it.copy(appAutoLaunch = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAppAutoStart(enabled))
    }

    fun setAttMute(attMute: AttMuteSetting) {
        _systemState.update { it.copy(attMute = attMute) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAttMute(attMute))
    }

    fun setRearPreout(preout: RearPreoutSetting) {
        _systemState.update { it.copy(rearPreout = preout) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildRearOutputPreout(preout))
    }

    fun setAutoPi(enabled: Boolean) {
        _systemState.update { it.copy(autoPi = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAutoPi(enabled))
    }

    fun setAuxEnabled(enabled: Boolean) {
        _systemState.update { it.copy(auxEnabled = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAuxSource(enabled))
    }

    fun setBtAudioEnabled(enabled: Boolean) {
        _systemState.update { it.copy(btAudioEnabled = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBtAudioSource(enabled))
    }

    fun setFmStep(step: FmStep) {
        _systemState.update { it.copy(fmStep = step) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildFmStep(step))
    }

    fun setAmStep(step: AmStep) {
        _systemState.update { it.copy(amStep = step) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAmStep(step))
    }

    fun setDistanceUnit(unit: DistanceUnit) {
        _systemState.update { it.copy(distanceUnit = unit) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildDistanceUnit(unit))
    }

    // --- Illumination Setters ---

    fun setDimmerMode(mode: DimmerMode) {
        _illuminationState.update { it.copy(dimmerMode = mode) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildDimmerSetting(mode))
    }

    fun setBrightness(zone: Int, level: Int) {
        _illuminationState.update {
            if (zone == 1) it.copy(keyBrightness = level) else it.copy(displayBrightness = level)
        }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBrightness(zone, level))
    }

    fun setPresetColor(zone: Int, color: PioneerPresetColor) {
        _illuminationState.update { it.copy(presetColor = color) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildPresetColor(zone, color))
    }

    fun setAllIlluminationPreset(color: PioneerPresetColor) {
        _illuminationState.update { it.copy(presetColor = color) }
        prefs?.edit()?.putString("illumination_preset", color.name)?.apply()
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildPresetColor(0, color))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildPresetColor(1, color))
    }

    fun setCustomRgb(zone: Int, r: Int, g: Int, b: Int) {
        _illuminationState.update { it.copy(customRed = r, customGreen = g, customBlue = b, presetColor = PioneerPresetColor.CUSTOM) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCustomColor(zone, r, g, b))
    }

    fun setAllIlluminationRgb(r: Int, g: Int, b: Int) {
        _illuminationState.update { it.copy(customRed = r, customGreen = g, customBlue = b, presetColor = PioneerPresetColor.CUSTOM) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCustomColor(0, r, g, b))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildCustomColor(1, r, g, b))
    }

    fun setAllBrightness(level: Int) {
        _illuminationState.update { it.copy(keyBrightness = level, displayBrightness = level) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBrightness(0, level))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBrightness(1, level))
    }

    fun setAudioLevelMeter(enabled: Boolean) {
        _illuminationState.update { it.copy(audioLevelMeter = enabled) }
        prefs?.edit()?.putBoolean("illumination_equalizer", enabled)?.apply()
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAudioLevelMeter(enabled))
    }

    // Автоматическая отправка сохранённых настроек подсветки при подключении SPH-10BT
    fun applyCurrentIllumination() {
        val illumi = _illuminationState.value
        // 1. Установка цвета (Zone 0 = экран, Zone 1 = клавиши и нижняя полоса)
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildPresetColor(0, illumi.presetColor))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildPresetColor(1, illumi.presetColor))
        // 2. Установка режима (Статическая / Эквалайзер)
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAudioLevelMeter(illumi.audioLevelMeter))
    }

    // --- Parking Sensor Setters ---

    fun setParkingSensor(enabled: Boolean, volume: Int, dest: AlarmOutput, pol: BackPolarity) {
        _parkingState.update { it.copy(enabled = enabled, alarmVolume = volume, outputDestination = dest, backPolarity = pol) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildParkingSensor(enabled))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAlarmVolume(volume))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAlarmOutputDestination(dest))
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBackPolarity(pol))
    }

    // --- Radio Function Setters ---

    fun triggerBsm() {
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildBsm())
    }

    fun setLocalSeek(level: LocalSeekLevel) {
        _radioFuncState.update { it.copy(localSeek = level) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildLocalSeek(level))
    }

    fun setTa(enabled: Boolean) {
        _radioFuncState.update { it.copy(ta = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildTa(enabled))
    }

    fun setAf(enabled: Boolean) {
        _radioFuncState.update { it.copy(af = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildAf(enabled))
    }

    fun setReg(enabled: Boolean) {
        _radioFuncState.update { it.copy(reg = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildReg(enabled))
    }

    fun setNews(enabled: Boolean) {
        _radioFuncState.update { it.copy(news = enabled) }
        PioneerConnectionManager.sendRawPacket(OutgoingPackets.buildNews(enabled))
    }

    // --- Pioneer Buddy Remapper Setters ---

    fun updateRemapper(updater: (RemapperSettingsState) -> RemapperSettingsState) {
        val newState = updater(_remapperState.value)
        _remapperState.value = newState
        prefs?.edit()?.apply {
            putString("nav_short", newState.navShortAction)
            putString("nav_long", newState.navLongAction)
            putString("msg_short", newState.msgShortAction)
            putString("msg_long", newState.msgLongAction)
            putString("tel_short", newState.telShortAction)
            putString("tel_long", newState.telLongAction)
            putString("mic_short", newState.micShortAction)
            putString("mic_long", newState.micLongAction)
            putBoolean("tts_tg", newState.ttsTelegram)
            putBoolean("tts_wa", newState.ttsWhatsApp)
            putBoolean("tts_vk", newState.ttsVk)
            putBoolean("tts_max", newState.ttsMax)
            putBoolean("tts_sms", newState.ttsSms)
            apply()
        }
    }
}
