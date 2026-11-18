package com.pioneer.buddy.model

enum class ConnectionStatus {
    DISCONNECTED,
    LISTENING,
    CONNECTED
}

enum class MediaSource {
    BLUETOOTH_MUSIC,
    TUNER_FM,
    TUNER_AM,
    USB,
    AUX
}

data class DashboardState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val deviceName: String = "Pioneer SPH-10BT",
    val speedKmH: Int = 0,
    val volume: Int = 20,
    val isMuted: Boolean = false,
    val currentSource: MediaSource = MediaSource.BLUETOOTH_MUSIC,

    // Media Player state
    val trackTitle: String = "Не воспроизводится",
    val artistName: String = "Pioneer Buddy",
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false,
    val activePlayerPackage: String = "",
    val activePlayerName: String = "Медиаплеер",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,

    // Radio Tuner state
    val radioFrequency: String = "104.2 FM",
    val stationName: String = "Европа Плюс",
    val radioText: String = "Хит за хитом",
    val currentPreset: Int = 1,
    val presets: List<String> = listOf("104.2", "101.8", "107.0", "103.7", "105.3", "106.2")
)

data class QuickButtonItem(
    val id: Int,
    val title: String,
    val iconName: String,
    val packageName: String? = null,
    val actionType: QuickActionType = QuickActionType.LAUNCH_APP
)

enum class QuickActionType {
    LAUNCH_APP,
    TOGGLE_RADIO,
    OPEN_DSP_EQUALIZER,
    DIRECT_CALL,
    OPEN_SETTINGS
}
