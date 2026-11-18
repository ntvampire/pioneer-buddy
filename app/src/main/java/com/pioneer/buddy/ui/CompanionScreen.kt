package com.pioneer.buddy.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pioneer.buddy.BuildConfig
import com.pioneer.buddy.model.ConnectionStatus
import com.pioneer.buddy.model.DashboardState
import com.pioneer.buddy.model.DimmerMode
import com.pioneer.buddy.model.PioneerPresetColor
import com.pioneer.buddy.model.UpdateInfo
import com.pioneer.buddy.model.UpdateState
import com.pioneer.buddy.protocol.DeviceControlCommand
import com.pioneer.buddy.service.KeyActionDispatcher
import com.pioneer.buddy.service.PioneerConnectionManager
import com.pioneer.buddy.service.PioneerSettingsManager
import com.pioneer.buddy.service.SmartNotificationListenerService
import com.pioneer.buddy.service.UpdateManager
import com.pioneer.buddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionScreen() {
    val context = LocalContext.current
    val dashboardState by PioneerConnectionManager.dashboardState.collectAsStateWithLifecycle()
    val illuminationState by PioneerSettingsManager.illuminationState.collectAsStateWithLifecycle()
    val remapperState by PioneerSettingsManager.remapperState.collectAsStateWithLifecycle()
    val updateState by UpdateManager.updateState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        UpdateManager.checkForUpdates(manual = false)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PIONEER BUDDY",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = PioneerCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PioneerCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "SPH-10BT",
                                color = PioneerCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { launchPioneerSmartSync(context) }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Pioneer Smart Sync",
                            tint = PioneerAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Баннер OTA обновлений
            AppUpdateBannerCard(
                updateState = updateState,
                onDownload = { info -> UpdateManager.startDownload(context, info) },
                onInstall = { file -> UpdateManager.installApk(context, file) },
                onDismiss = { UpdateManager.resetState() }
            )

            // 1. Статус связи и мини-плеер
            CompanionStatusCard(dashboardState = dashboardState, context = context)

            // 2. Управление подсветкой (Нижняя зона под кредлом и кнопки)
            IlluminationQuickCard(
                customRed = illuminationState.customRed,
                customGreen = illuminationState.customGreen,
                customBlue = illuminationState.customBlue,
                activePreset = illuminationState.presetColor,
                brightness = illuminationState.displayBrightness,
                dimmerMode = illuminationState.dimmerMode,
                onPresetSelected = { preset ->
                    PioneerSettingsManager.setAllIlluminationPreset(preset)
                },
                onRgbChanged = { r, g, b ->
                    PioneerSettingsManager.setAllIlluminationRgb(r, g, b)
                },
                onBrightnessChanged = { level ->
                    PioneerSettingsManager.setAllBrightness(level)
                },
                onDimmerChanged = { mode ->
                    PioneerSettingsManager.setDimmerMode(mode)
                }
            )

            // 3. Ремаппинг аппаратных кнопок магнитолы
            HardwareRemapperCard(
                navShort = remapperState.navShortAction,
                navLong = remapperState.navLongAction,
                msgShort = remapperState.msgShortAction,
                msgLong = remapperState.msgLongAction,
                telShort = remapperState.telShortAction,
                telLong = remapperState.telLongAction,
                micShort = remapperState.micShortAction,
                micLong = remapperState.micLongAction,
                onUpdate = { updater ->
                    PioneerSettingsManager.updateRemapper(updater)
                }
            )

            // 4. Умные твики SPH-10BT (Громкость, бегущая строка, TTS)
            SmartTweaksCard(
                volumeRamping = remapperState.continuousVolumeRamping,
                infiniteMarquee = remapperState.infiniteMarqueeScroll,
                ttsTelegram = remapperState.ttsTelegram,
                ttsWhatsApp = remapperState.ttsWhatsApp,
                ttsVk = remapperState.ttsVk,
                ttsMax = remapperState.ttsMax,
                ttsSms = remapperState.ttsSms,
                onVolumeRampingChanged = { enabled ->
                    PioneerSettingsManager.updateRemapper { it.copy(continuousVolumeRamping = enabled) }
                },
                onInfiniteMarqueeChanged = { enabled ->
                    PioneerSettingsManager.updateRemapper { it.copy(infiniteMarqueeScroll = enabled) }
                },
                onTtsToggle = { type, enabled ->
                    PioneerSettingsManager.updateRemapper {
                        when (type) {
                            "tg" -> it.copy(ttsTelegram = enabled)
                            "wa" -> it.copy(ttsWhatsApp = enabled)
                            "vk" -> it.copy(ttsVk = enabled)
                            "max" -> it.copy(ttsMax = enabled)
                            "sms" -> it.copy(ttsSms = enabled)
                            else -> it
                        }
                    }
                }
            )

            // 5. Сервисная плашка: Smart Sync для редкой настройки акустики
            SmartSyncInfoCard(onOpen = { launchPioneerSmartSync(context) })

            // 6. Подвал: Версия и проверка обновлений
            AppVersionFooter(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                isChecking = updateState is UpdateState.Checking,
                onCheckUpdates = { UpdateManager.checkForUpdates(manual = true) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// 1. Статус связи и мини-плеер
// -----------------------------------------------------------------------------
@Composable
private fun CompanionStatusCard(dashboardState: DashboardState, context: Context) {
    val isConnected = dashboardState.connectionStatus == ConnectionStatus.CONNECTED
    val isListening = dashboardState.connectionStatus == ConnectionStatus.LISTENING

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Индикатор статуса Bluetooth
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isConnected -> Color(0xFF00E676)
                                    isListening -> PioneerAmber
                                    else -> Color.Gray
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isConnected -> "Магнитола подключена"
                            isListening -> "Ожидание подключения..."
                            else -> "Bluetooth не активен"
                        },
                        color = if (isConnected) Color(0xFF00E676) else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = dashboardState.deviceName,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Активный трек и кнопка Лайк
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dashboardState.trackTitle.ifBlank { "Музыка не воспроизводится" },
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dashboardState.artistName.ifBlank { "Яндекс Музыка / VK / Плеер" },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Кнопка Лайк для любого плеера
                IconButton(
                    onClick = {
                        SmartNotificationListenerService.triggerLikeAction()
                        Toast.makeText(context, "❤️ Лайк отправлен в плеер", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PioneerRed.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Лайк трека",
                        tint = PioneerRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play / Pause
                IconButton(
                    onClick = {
                        PioneerConnectionManager.sendDeviceCommand(DeviceControlCommand.PLAY_PAUSE)
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PioneerCyan.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (dashboardState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Воспроизведение / Пауза",
                        tint = PioneerCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 2. Управление подсветкой (Bottom Illumination & Keys)
// -----------------------------------------------------------------------------
@Composable
private fun IlluminationQuickCard(
    customRed: Int,
    customGreen: Int,
    customBlue: Int,
    activePreset: PioneerPresetColor,
    brightness: Int,
    dimmerMode: DimmerMode,
    onPresetSelected: (PioneerPresetColor) -> Unit,
    onRgbChanged: (Int, Int, Int) -> Unit,
    onBrightnessChanged: (Int) -> Unit,
    onDimmerChanged: (DimmerMode) -> Unit
) {
    val presetList = listOf(
        PioneerPresetColor.TURQUOISE to Color(0xFF00E5FF),
        PioneerPresetColor.BLUE to Color(0xFF007AFF),
        PioneerPresetColor.LIGHT_BLUE to Color(0xFF40C4FF),
        PioneerPresetColor.PURE_GREEN to Color(0xFF00E676),
        PioneerPresetColor.GREEN to Color(0xFF34C759),
        PioneerPresetColor.YELLOW to Color(0xFFFFD600),
        PioneerPresetColor.AMBER to Color(0xFFFF9100),
        PioneerPresetColor.ORANGE to Color(0xFFFF6D00),
        PioneerPresetColor.RED to Color(0xFFFF1744),
        PioneerPresetColor.PINK to Color(0xFFFF4081),
        PioneerPresetColor.PURPLE to Color(0xFFAA00FF),
        PioneerPresetColor.WHITE to Color(0xFFFFFFFF)
    )

    // Преобразуем шкалу Pioneer (0..60) в экранный цвет (0..255)
    val currentColor = Color(
        red = (customRed * 255 / 60).coerceIn(0, 255),
        green = (customGreen * 255 / 60).coerceIn(0, 255),
        blue = (customBlue * 255 / 60).coerceIn(0, 255)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ПОДСВЕТКА КРЕДЛА И КНОПОК",
                    color = PioneerCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Плашка предпросмотра цвета
                Box(
                    modifier = Modifier
                        .size(36.dp, 20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(currentColor)
                        .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "Готовые оттенки", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Пресеты в горизонтальной строке
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetList.forEach { (preset, col) ->
                    val isSelected = activePreset == preset
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(col)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else CardBorder,
                                shape = CircleShape
                            )
                            .clickable { onPresetSelected(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Точная RGB настройка (Pioneer 0..60): R=$customRed, G=$customGreen, B=$customBlue",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // R Slider
            RgbSliderRow(label = "R", value = customRed, color = PioneerRed) { r ->
                onRgbChanged(r, customGreen, customBlue)
            }
            // G Slider
            RgbSliderRow(label = "G", value = customGreen, color = Color(0xFF00E676)) { g ->
                onRgbChanged(customRed, g, customBlue)
            }
            // B Slider
            RgbSliderRow(label = "B", value = customBlue, color = PioneerCyan) { b ->
                onRgbChanged(customRed, customGreen, b)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Яркость подсветки
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Яркость: $brightness / 10", color = TextPrimary, fontSize = 13.sp)
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { onBrightnessChanged(it.toInt().coerceIn(1, 10)) },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.width(180.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = PioneerCyan,
                        activeTrackColor = PioneerCyan
                    )
                )
            }
        }
    }
}

@Composable
private fun RgbSliderRow(label: String, value: Int, color: Color, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(0, 60)) },
            valueRange = 0f..60f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        Text(
            text = "$value",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(28.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// 3. Ремаппинг аппаратных кнопок магнитолы
// -----------------------------------------------------------------------------
@Composable
private fun HardwareRemapperCard(
    navShort: String,
    navLong: String,
    msgShort: String,
    msgLong: String,
    telShort: String,
    telLong: String,
    micShort: String,
    micLong: String,
    onUpdate: ((com.pioneer.buddy.service.RemapperSettingsState) -> com.pioneer.buddy.service.RemapperSettingsState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "НАЗНАЧЕНИЕ КНОПОК SPH-10BT",
                color = PioneerCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // NAV Button
            KeyRowSelector(
                keyTitle = "NAV (Навигация)",
                shortAction = navShort,
                longAction = navLong,
                options = listOf("Яндекс Навигатор", "2ГИС", "Google Карты", "Следующий трек"),
                onShortSelect = { chosen -> onUpdate { it.copy(navShortAction = chosen) } },
                onLongSelect = { chosen -> onUpdate { it.copy(navLongAction = chosen) } }
            )

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

            // MSG Button
            KeyRowSelector(
                keyTitle = "MSG (Сообщения)",
                shortAction = msgShort,
                longAction = msgLong,
                options = listOf("Озвучить сообщения (TTS)", "Открыть Telegram", "Открыть WhatsApp", "Поставить Лайк ❤️"),
                onShortSelect = { chosen -> onUpdate { it.copy(msgShortAction = chosen) } },
                onLongSelect = { chosen -> onUpdate { it.copy(msgLongAction = chosen) } }
            )

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

            // TEL Button
            KeyRowSelector(
                keyTitle = "TEL (Телефон)",
                shortAction = telShort,
                longAction = telLong,
                options = listOf("Контакты / Вызовы", "Быстрый вызов (Избранное)", "Открыть Telegram", "Поставить Лайк ❤️"),
                onShortSelect = { chosen -> onUpdate { it.copy(telShortAction = chosen) } },
                onLongSelect = { chosen -> onUpdate { it.copy(telLongAction = chosen) } }
            )

            HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

            // MIC Button
            KeyRowSelector(
                keyTitle = "MIC (Голос / Меню)",
                shortAction = micShort,
                longAction = micLong,
                options = listOf("Голосовой ассистент", "MUTE / ATT", "Поставить Лайк ❤️", "Озвучить сообщения (TTS)"),
                onShortSelect = { chosen -> onUpdate { it.copy(micShortAction = chosen) } },
                onLongSelect = { chosen -> onUpdate { it.copy(micLongAction = chosen) } }
            )
        }
    }
}

@Composable
private fun KeyRowSelector(
    keyTitle: String,
    shortAction: String,
    longAction: String,
    options: List<String>,
    onShortSelect: (String) -> Unit,
    onLongSelect: (String) -> Unit
) {
    Column {
        Text(text = keyTitle, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DropdownActionSelector(
                label = "Короткое",
                selected = shortAction,
                options = options,
                modifier = Modifier.weight(1f),
                onSelect = onShortSelect
            )

            DropdownActionSelector(
                label = "Удержание",
                selected = longAction,
                options = options,
                modifier = Modifier.weight(1f),
                onSelect = onLongSelect
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownActionSelector(
    label: String,
    selected: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PioneerCyan,
                unfocusedBorderColor = CardBorder,
                focusedLabelColor = PioneerCyan,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CardBackground)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextPrimary, fontSize = 13.sp) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// 4. Умные твики SPH-10BT (Громкость, бегущая строка, TTS)
// -----------------------------------------------------------------------------
@Composable
private fun SmartTweaksCard(
    volumeRamping: Boolean,
    infiniteMarquee: Boolean,
    ttsTelegram: Boolean,
    ttsWhatsApp: Boolean,
    ttsVk: Boolean,
    ttsMax: Boolean,
    ttsSms: Boolean,
    onVolumeRampingChanged: (Boolean) -> Unit,
    onInfiniteMarqueeChanged: (Boolean) -> Unit,
    onTtsToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ФИРМЕННЫЕ АЛГОРИТМЫ PIONEER BUDDY",
                color = PioneerCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Volume Ramping Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Плавное нарастание громкости",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "При удержании кнопок ^ / v непрерывно регулирует громкость вместо случайного MUTE",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = volumeRamping,
                    onCheckedChange = onVolumeRampingChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PioneerCyan,
                        checkedTrackColor = PioneerCyan.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Infinite Marquee Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Бесконечная бегущая строка",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Название трека на 1-строчном экране магнитолы крутится циклически и не замирает",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = infiniteMarquee,
                    onCheckedChange = onInfiniteMarqueeChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PioneerCyan,
                        checkedTrackColor = PioneerCyan.copy(alpha = 0.3f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Мессенджеры для озвучки (TTS)
            Text(
                text = "Озвучивать входящие сообщения (TTS):",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(label = "Telegram", checked = ttsTelegram) { onTtsToggle("tg", it) }
                FilterChipItem(label = "WhatsApp", checked = ttsWhatsApp) { onTtsToggle("wa", it) }
                FilterChipItem(label = "MAX", checked = ttsMax) { onTtsToggle("max", it) }
                FilterChipItem(label = "VK", checked = ttsVk) { onTtsToggle("vk", it) }
                FilterChipItem(label = "SMS", checked = ttsSms) { onTtsToggle("sms", it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    FilterChip(
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        label = { Text(label, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PioneerCyan.copy(alpha = 0.2f),
            selectedLabelColor = PioneerCyan,
            containerColor = DarkBackground,
            labelColor = TextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = checked,
            borderColor = CardBorder,
            selectedBorderColor = PioneerCyan
        )
    )
}

// -----------------------------------------------------------------------------
// 5. Сервисная плашка: Smart Sync
// -----------------------------------------------------------------------------
@Composable
private fun SmartSyncInfoCard(onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Тонкая настройка звука (DSP)",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Для разовой калибровки эквалайзера и кроссовера используйте Pioneer Smart Sync",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            OutlinedButton(
                onClick = onOpen,
                border = androidx.compose.foundation.BorderStroke(1.dp, PioneerAmber),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PioneerAmber),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Smart Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun launchPioneerSmartSync(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage("jp.pioneer.carsync")
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Pioneer Smart Sync не установлен на телефоне", Toast.LENGTH_LONG).show()
    }
}

// -----------------------------------------------------------------------------
// 6. Баннер OTA обновлений и подвал версии
// -----------------------------------------------------------------------------
@Composable
private fun AppUpdateBannerCard(
    updateState: UpdateState,
    onDownload: (UpdateInfo) -> Unit,
    onInstall: (java.io.File) -> Unit,
    onDismiss: () -> Unit
) {
    when (updateState) {
        is UpdateState.UpdateAvailable -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PioneerCyan.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PioneerCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = PioneerCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Доступно обновление v${updateState.info.versionName}",
                                color = PioneerCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (updateState.info.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = updateState.info.releaseNotes,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onDownload(updateState.info) },
                        colors = ButtonDefaults.buttonColors(containerColor = PioneerCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Скачать и установить", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        is UpdateState.Downloading -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, PioneerCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Загрузка обновления...",
                            color = PioneerCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${updateState.percent}%",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { updateState.percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = PioneerCyan,
                        trackColor = CardBorder
                    )
                }
            }
        }
        is UpdateState.ReadyToInstall -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00E676).copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Обновление v${updateState.info.versionName} готово к установке",
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onInstall(updateState.apkFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Установить сейчас", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        is UpdateState.UpToDate -> {
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("У вас установлена последняя версия", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        is UpdateState.Error -> {
            Surface(
                color = PioneerRed.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PioneerRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = updateState.message, color = PioneerRed, fontSize = 12.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = PioneerRed, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun AppVersionFooter(
    versionName: String,
    versionCode: Int,
    isChecking: Boolean,
    onCheckUpdates: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Pioneer Buddy v$versionName (сборка $versionCode)",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "GitHub OTA Release Engine",
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }

        TextButton(
            onClick = onCheckUpdates,
            enabled = !isChecking
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = PioneerCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = if (isChecking) "Проверка..." else "Проверить",
                color = PioneerCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
