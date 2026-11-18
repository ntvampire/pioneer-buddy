package com.pioneer.buddy.model

// ==========================================
// 1. АУДИО НАСТРОЙКИ (AUDIO SETTINGS)
// ==========================================

enum class PioneerEqPreset(val code: Int, val title: String) {
    SUPER_BASS(0, "Super Bass"),
    POWERFUL(1, "Powerful"),
    NATURAL(2, "Natural"),
    VOCAL(3, "Vocal"),
    FLAT(10, "Flat"),
    POP_ROCK(5, "Pop / Rock"),
    ELECTRONICA(6, "Electronica"),
    CUSTOM_1(11, "Custom 1"),
    CUSTOM_2(12, "Custom 2");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: FLAT
    }
}

enum class SubwooferPhase(val code: Int, val title: String) {
    NORMAL(0, "Normal (0°)"),
    REVERSE(1, "Reverse (180°)");
}

enum class StandardCutoff(val code: Int, val frequencyHz: Int) {
    _50HZ(0, 50),
    _63HZ(1, 63),
    _80HZ(2, 80),
    _100HZ(3, 100),
    _125HZ(4, 125),
    _160HZ(5, 160),
    _200HZ(6, 200);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: _80HZ
    }
}

enum class StandardSlope(val code: Int, val dbOct: Int) {
    _6DB(0, -6),
    _12DB(1, -12),
    _18DB(2, -18),
    _24DB(3, -24);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: _18DB
    }
}

enum class ListeningPosition(val code: Int, val title: String) {
    OFF(0, "Выкл"),
    FRONT_LEFT(1, "Водитель (Front Left)"),
    FRONT_RIGHT(2, "Пассажир (Front Right)"),
    FRONT(3, "Фронт (Front)"),
    ALL(4, "Все (All)");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: OFF
    }
}

enum class LoudnessSetting(val code: Int, val title: String) {
    OFF(0, "Выкл"),
    LOW(1, "Низкая (Low)"),
    MID(2, "Средняя (Mid)"),
    HIGH(3, "Высокая (High)");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: OFF
    }
}

enum class SoundRetrieverSetting(val code: Int, val title: String) {
    OFF(0, "Выкл"),
    MODE1(1, "Режим 1"),
    MODE2(2, "Режим 2");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: OFF
    }
}

// 13 полос эквалайзера Pioneer SPH-10BT
data class Pioneer13BandEq(
    val band50Hz: Int = 0,    // -12 .. +12 dB
    val band80Hz: Int = 0,
    val band125Hz: Int = 0,
    val band200Hz: Int = 0,
    val band315Hz: Int = 0,
    val band500Hz: Int = 0,
    val band800Hz: Int = 0,
    val band1_25kHz: Int = 0,
    val band2kHz: Int = 0,
    val band3_15kHz: Int = 0,
    val band5kHz: Int = 0,
    val band8kHz: Int = 0,
    val band12_5kHz: Int = 0
) {
    fun toArray(): IntArray = intArrayOf(
        band50Hz, band80Hz, band125Hz, band200Hz, band315Hz, band500Hz,
        band800Hz, band1_25kHz, band2kHz, band3_15kHz, band5kHz, band8kHz, band12_5kHz
    )
}

// ==========================================
// 2. СИСТЕМНЫЕ НАСТРОЙКИ (SYSTEM SETTINGS)
// ==========================================

enum class AttMuteSetting(val code: Int, val title: String) {
    MUTE(0, "MUTE (Полная тишина)"),
    ATT_20DB(1, "ATT -20 dB"),
    ATT_10DB(2, "ATT -10 dB");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: MUTE
    }
}

enum class RearPreoutSetting(val code: Int, val title: String) {
    REAR_REAR(0, "Тыл / Тыл (REAR / REAR)"),
    REAR_SUBWOOFER(1, "Тыл / Сабвуфер (REAR / SUB.W)");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: REAR_SUBWOOFER
    }
}

enum class FmStep(val code: Int, val title: String) {
    _50KHZ(0, "50 кГц (Европа / РФ)"),
    _100KHZ(1, "100 кГц (США / Азия)");
}

enum class AmStep(val code: Int, val title: String) {
    _9KHZ(0, "9 кГц (Европа / РФ)"),
    _10KHZ(1, "10 кГц (США / Азия)");
}

enum class DistanceUnit(val code: Int, val title: String) {
    KILOMETER(0, "Километры (км, км/ч)"),
    MILE(1, "Мили (mi, mph)");
}

// ==========================================
// 3. ПОДСВЕТКА И ДИММЕР (ILLUMINATION)
// ==========================================

enum class DimmerMode(val code: Int, val title: String) {
    ILLUMI_LINE(0, "Авто (по габаритам ILLUMI wire)"),
    SYNC_CLOCK(1, "По расписанию (таймер часов)"),
    ON(2, "Всегда включен (приглушен)"),
    OFF(3, "Всегда выключен (яркий)"),
    MANUAL(4, "Ручное переключение");

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: ILLUMI_LINE
    }
}

enum class IlluminationZone(val code: Int, val title: String) {
    KEYS(1, "Кнопки (Зона 1)"),
    DISPLAY(2, "Дисплей (Зона 2)"),
    COMMON(3, "Все зоны синхронно");
}

enum class PioneerPresetColor(val code: Int, val title: String, val hex: Long) {
    WHITE(0, "Белый (White)", 0xFFF0F0F0),
    RED(1, "Красный (Red)", 0xFFFF2222),
    AMBER(2, "Янтарный (Amber)", 0xFFFF8C00),
    ORANGE(3, "Оранжевый (Orange)", 0xFFFF5500),
    YELLOW(4, "Желтый (Yellow)", 0xFFFFD700),
    PURE_GREEN(5, "Чисто-зеленый (Pure Green)", 0xFF00FF44),
    GREEN(6, "Зеленый (Green)", 0xFF00DD00),
    TURQUOISE(7, "Бирюзовый (Turquoise)", 0xFF00CED1),
    LIGHT_BLUE(8, "Светло-синий (Light Blue)", 0xFF00BFFF),
    BLUE(9, "Синий (Blue)", 0xFF1E90FF),
    PURPLE(10, "Фиолетовый (Purple)", 0xFF9932CC),
    PINK(11, "Розовый (Pink)", 0xFFFF1493),
    SCAN(12, "Переливание (Scan)", 0xFF00FFFF),
    CUSTOM(13, "Пользовательский RGB (0..60)", 0xFF00D2D2);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: TURQUOISE
    }
}

// ==========================================
// 4. ПАРКТРОНИК (CAR SAFETY ND-PS1)
// ==========================================

enum class AlarmOutput(val code: Int, val title: String) {
    FRONT(0, "Фронтальные динамики"),
    FRONT_LEFT(1, "Только левый динамик"),
    FRONT_RIGHT(2, "Только правый динамик");
}

enum class BackPolarity(val code: Int, val title: String) {
    BATTERY(0, "Плюс батареи (+12V)"),
    GROUND(1, "Масса (Ground)");
}

// ==========================================
// 5. ТЮНЕР (RADIO FUNCTIONS)
// ==========================================

enum class LocalSeekLevel(val code: Int, val title: String) {
    OFF(0, "Выкл (Все станции)"),
    LEVEL_1(1, "Уровень 1 (Слабый порог)"),
    LEVEL_2(2, "Уровень 2"),
    LEVEL_3(3, "Уровень 3"),
    LEVEL_4(4, "Уровень 4 (Только сильнейший сигнал)");
}
