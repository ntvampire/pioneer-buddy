package com.pioneer.buddy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pioneer.buddy.model.*
import com.pioneer.buddy.service.*
import com.pioneer.buddy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "🎵 Звук DSP",
        "⚙️ Система",
        "💡 Подсветка",
        "🅿️ Парктроник",
        "📻 Тюнер",
        "🔘 Кнопки & TTS"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Настройки SPH-10BT",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pioneer Buddy Pro Setup",
                            color = PioneerCyan,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = PioneerCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Горизонтальный переключатель категорий
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = PioneerCyan,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) PioneerCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое выбранной категории
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
            ) {
                when (selectedTab) {
                    0 -> AudioDspSettingsTab()
                    1 -> SystemSettingsTab()
                    2 -> IlluminationSettingsTab()
                    3 -> ParkingSensorSettingsTab()
                    4 -> RadioFunctionSettingsTab()
                    5 -> HardwareRemapperSettingsTab()
                }
            }
        }
    }
}

// =============================================================================
// 1. ВКЛАДКА: АУДИО DSP (AUDIO DSP SETTINGS)
// =============================================================================

@Composable
fun AudioDspSettingsTab() {
    val audioState by PioneerSettingsManager.audioState.collectAsState()
    var showProfileToast by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Сохранение / Загрузка профилей звука на смартфоне (без записи в EEPROM магнитолы)
        item {
            var selectedProfile by remember { mutableStateOf("Любимый") }
            val profiles = listOf("Любимый", "Трасса", "Город")

            SettingsCard(title = "Профили звука (Хранение на смартфоне)") {
                Text(
                    text = "Профили звука хранятся локально на телефоне и применяются в DSP магнитолы только в оперативную память текущего сеанса. Никакой записи в память (EEPROM) магнитолы не производится.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profiles.forEach { prof ->
                        FilterChip(
                            selected = selectedProfile == prof,
                            onClick = { selectedProfile = prof },
                            label = { Text(prof, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showProfileToast = PioneerSettingsManager.saveAudioProfileLocally(selectedProfile)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PioneerCyan)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = PioneerCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить", color = TextPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            showProfileToast = PioneerSettingsManager.loadAudioProfileLocally(selectedProfile)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PioneerAmber)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PioneerAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Применить в DSP", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                if (showProfileToast != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = showProfileToast!!,
                        color = PioneerCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. 13-полосный графический эквалайзер
        item {
            SettingsCard(title = "13-полосный графический эквалайзер") {
                Text(text = "Пресеты эквалайзера:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PioneerEqPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = audioState.eqPreset == preset,
                            onClick = { PioneerSettingsManager.setEqPreset(preset) },
                            label = { Text(preset.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black,
                                containerColor = CardBorder,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Полосы частот (-12 dB .. +12 dB):",
                    fontSize = 12.sp,
                    color = PioneerCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val freqLabels = listOf("50", "80", "125", "200", "315", "500", "800", "1.25k", "2k", "3.15k", "5k", "8k", "12.5k")
                val bands = audioState.eqBands.toArray()

                // Горизонтальный скролл 13 ползунков
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    freqLabels.forEachIndexed { index, label ->
                        val currentLevel = bands[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(56.dp)
                                .background(CardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (currentLevel > 0) "+$currentLevel" else "$currentLevel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentLevel != 0) PioneerAmber else TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Вертикальный слайдер или кнопки +/-
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentLevel > -12) PioneerSettingsManager.setEqBand(index, currentLevel - 1)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("-", fontSize = 16.sp, color = PioneerCyan, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = {
                                        if (currentLevel < 12) PioneerSettingsManager.setEqBand(index, currentLevel + 1)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text("+", fontSize = 16.sp, color = PioneerCyan, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = label, fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(text = "Hz", fontSize = 9.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // 3. Фейдер и Баланс
        item {
            SettingsCard(title = "Фейдер / Баланс (Fader & Balance)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val faderText = when {
                        audioState.fader > 0 -> "Фронт +${audioState.fader}"
                        audioState.fader < 0 -> "Тыл ${audioState.fader}"
                        else -> "Центр (0)"
                    }
                    Text("Фейдер (Fader):", fontSize = 13.sp, color = TextPrimary)
                    Text(faderText, fontSize = 13.sp, color = PioneerCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = audioState.fader.toFloat(),
                    onValueChange = { PioneerSettingsManager.setFaderBalance(it.toInt(), audioState.balance) },
                    valueRange = -25f..25f,
                    colors = SliderDefaults.colors(thumbColor = PioneerCyan, activeTrackColor = PioneerCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val balanceText = when {
                        audioState.balance > 0 -> "Правый +${audioState.balance}"
                        audioState.balance < 0 -> "Левый ${audioState.balance}"
                        else -> "Центр (0)"
                    }
                    Text("Баланс (Balance):", fontSize = 13.sp, color = TextPrimary)
                    Text(balanceText, fontSize = 13.sp, color = PioneerCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = audioState.balance.toFloat(),
                    onValueChange = { PioneerSettingsManager.setFaderBalance(audioState.fader, it.toInt()) },
                    valueRange = -25f..25f,
                    colors = SliderDefaults.colors(thumbColor = PioneerCyan, activeTrackColor = PioneerCyan)
                )

                Button(
                    onClick = { PioneerSettingsManager.setFaderBalance(0, 0) },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Сброс в центр", fontSize = 11.sp, color = TextPrimary)
                }
            }
        }

        // 4. Сабвуфер (LPF и Фаза)
        item {
            SettingsCard(title = "Сабвуфер и срез LPF") {
                SettingSwitch(
                    title = "Сабвуфер включен",
                    subtitle = "Активация выхода RCA Subwoofer и фильтра НЧ",
                    checked = audioState.subwooferEnabled,
                    onCheckedChange = {
                        PioneerSettingsManager.setSubwoofer(it, audioState.subwooferPhase, audioState.subwooferCutoff, audioState.subwooferSlope)
                    }
                )

                if (audioState.subwooferEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Фаза сабвуфера:", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubwooferPhase.entries.forEach { phase ->
                            FilterChip(
                                selected = audioState.subwooferPhase == phase,
                                onClick = {
                                    PioneerSettingsManager.setSubwoofer(audioState.subwooferEnabled, phase, audioState.subwooferCutoff, audioState.subwooferSlope)
                                },
                                label = { Text(phase.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PioneerCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Частота среза LPF (Гц):", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StandardCutoff.entries.forEach { cutoff ->
                            FilterChip(
                                selected = audioState.subwooferCutoff == cutoff,
                                onClick = {
                                    PioneerSettingsManager.setSubwoofer(audioState.subwooferEnabled, audioState.subwooferPhase, cutoff, audioState.subwooferSlope)
                                },
                                label = { Text("${cutoff.frequencyHz} Гц", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PioneerCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Крутизна спада (Slope):", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StandardSlope.entries.forEach { slope ->
                            FilterChip(
                                selected = audioState.subwooferSlope == slope,
                                onClick = {
                                    PioneerSettingsManager.setSubwoofer(audioState.subwooferEnabled, audioState.subwooferPhase, audioState.subwooferCutoff, slope)
                                },
                                label = { Text("${slope.dbOct} dB/oct", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PioneerCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Кроссовер HPF (Фронт и Тыл)
        item {
            SettingsCard(title = "Кроссовер HPF (Фронт и Тыл)") {
                SettingSwitch(
                    title = "HPF Фронт (Фильтр ВЧ)",
                    subtitle = "Срез низких частот с фронтальных динамиков",
                    checked = audioState.hpfFrontEnabled,
                    onCheckedChange = {
                        PioneerSettingsManager.setHpfFront(it, audioState.hpfFrontCutoff, audioState.hpfFrontSlope)
                    }
                )
                if (audioState.hpfFrontEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StandardCutoff.entries.forEach { cutoff ->
                            FilterChip(
                                selected = audioState.hpfFrontCutoff == cutoff,
                                onClick = {
                                    PioneerSettingsManager.setHpfFront(true, cutoff, audioState.hpfFrontSlope)
                                },
                                label = { Text("${cutoff.frequencyHz} Гц", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PioneerCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                SettingSwitch(
                    title = "HPF Тыл (Фильтр ВЧ)",
                    subtitle = "Срез низких частот с тыловых динамиков",
                    checked = audioState.hpfRearEnabled,
                    onCheckedChange = {
                        PioneerSettingsManager.setHpfRear(it, audioState.hpfRearCutoff, audioState.hpfRearSlope)
                    }
                )
                if (audioState.hpfRearEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StandardCutoff.entries.forEach { cutoff ->
                            FilterChip(
                                selected = audioState.hpfRearCutoff == cutoff,
                                onClick = {
                                    PioneerSettingsManager.setHpfRear(true, cutoff, audioState.hpfRearSlope)
                                },
                                label = { Text("${cutoff.frequencyHz} Гц", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PioneerCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }

        // 6. Временные задержки и Позиция
        item {
            SettingsCard(title = "Временные задержки (Time Alignment)") {
                Text(text = "Позиция прослушивания (Listening Position):", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ListeningPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = audioState.listeningPosition == pos,
                            onClick = { PioneerSettingsManager.setListeningPosition(pos) },
                            label = { Text(pos.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Расстояние до динамиков (см):", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Фронт Л: ${audioState.timeAlignmentFlCm.toInt()} см", fontSize = 12.sp, color = TextPrimary)
                    Text("Фронт П: ${audioState.timeAlignmentFrCm.toInt()} см", fontSize = 12.sp, color = TextPrimary)
                    Text("Сабвуфер: ${audioState.timeAlignmentSwCm.toInt()} см", fontSize = 12.sp, color = PioneerCyan)
                }
            }
        }

        // 7. Тонкомпенсация, Басс-буст, SLA, Sound Retriever
        item {
            SettingsCard(title = "Улучшение звука и эффекты DSP") {
                // Loudness
                Text(text = "Тонкомпенсация (Loudness):", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoudnessSetting.entries.forEach { loud ->
                        FilterChip(
                            selected = audioState.loudness == loud,
                            onClick = { PioneerSettingsManager.setLoudness(loud) },
                            label = { Text(loud.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bass Booster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bass Booster (0 .. 6):", fontSize = 13.sp, color = TextPrimary)
                    Text("${audioState.bassBooster}", fontSize = 13.sp, color = PioneerAmber, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = audioState.bassBooster.toFloat(),
                    onValueChange = { PioneerSettingsManager.setBassBooster(it.toInt()) },
                    valueRange = 0f..6f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = PioneerAmber, activeTrackColor = PioneerAmber)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // SLA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SLA (Source Level Adjuster):", fontSize = 13.sp, color = TextPrimary)
                    val slaStr = if (audioState.sla > 0) "+${audioState.sla} dB" else "${audioState.sla} dB"
                    Text(slaStr, fontSize = 13.sp, color = PioneerCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = audioState.sla.toFloat(),
                    onValueChange = { PioneerSettingsManager.setSla(it.toInt()) },
                    valueRange = -4f..4f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = PioneerCyan, activeTrackColor = PioneerCyan)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sound Retriever
                Text(text = "Sound Retriever (Восстановление сжатого MP3):", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SoundRetrieverSetting.entries.forEach { sr ->
                        FilterChip(
                            selected = audioState.soundRetriever == sr,
                            onClick = { PioneerSettingsManager.setSoundRetriever(sr) },
                            label = { Text(sr.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 2. ВКЛАДКА: СИСТЕМА (SYSTEM SETTINGS)
// =============================================================================

@Composable
fun SystemSettingsTab() {
    val systemState by PioneerSettingsManager.systemState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(title = "Энергия и автозапуск") {
                SettingSwitch(
                    title = "Энергосбережение (Power Save Mode)",
                    subtitle = "Отключение подсветки и дисплея магнитолы при OFF для защиты АКБ",
                    checked = systemState.powerSaveMode,
                    onCheckedChange = { PioneerSettingsManager.setPowerSaveMode(it) }
                )
                SettingSwitch(
                    title = "Автозапуск USB (USB Auto)",
                    subtitle = "Автопереключение на USB при подключении флешки",
                    checked = systemState.usbAuto,
                    onCheckedChange = { PioneerSettingsManager.setUsbAuto(it) }
                )
                SettingSwitch(
                    title = "Автозапуск приложения (App Auto Launch)",
                    subtitle = "Автостарт Pioneer Buddy при подключении к SPH-10BT по Bluetooth",
                    checked = systemState.appAutoLaunch,
                    onCheckedChange = { PioneerSettingsManager.setAppAutoLaunch(it) }
                )
                SettingSwitch(
                    title = "Звуковой сигнал кнопок (Beep Tone)",
                    subtitle = "Звуковой сигнал при нажатии физических кнопок магнитолы",
                    checked = systemState.beepTone,
                    onCheckedChange = { PioneerSettingsManager.setBeepTone(it) }
                )
            }
        }

        item {
            SettingsCard(title = "Аппаратные режимы магнитолы") {
                Text(text = "Режим приглушения звука (ATT / MUTE):", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AttMuteSetting.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { PioneerSettingsManager.setAttMute(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = systemState.attMute == mode,
                                onClick = { PioneerSettingsManager.setAttMute(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = PioneerCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.title, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Конфигурация выходов RCA (Preout Output):", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RearPreoutSetting.entries.forEach { preout ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { PioneerSettingsManager.setRearPreout(preout) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = systemState.rearPreout == preout,
                                onClick = { PioneerSettingsManager.setRearPreout(preout) },
                                colors = RadioButtonDefaults.colors(selectedColor = PioneerCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(preout.title, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Источники и единицы измерения") {
                SettingSwitch(
                    title = "Вход AUX",
                    subtitle = "Отображать источник AUX в кольце SRC",
                    checked = systemState.auxEnabled,
                    onCheckedChange = { PioneerSettingsManager.setAuxEnabled(it) }
                )
                SettingSwitch(
                    title = "BT Audio источник",
                    subtitle = "Отображать беспроводное BT Audio в кольце SRC",
                    checked = systemState.btAudioEnabled,
                    onCheckedChange = { PioneerSettingsManager.setBtAudioEnabled(it) }
                )
                SettingSwitch(
                    title = "Auto-PI поиск",
                    subtitle = "Автопоиск той же радиостанции с сигналом PI при помехах",
                    checked = systemState.autoPi,
                    onCheckedChange = { PioneerSettingsManager.setAutoPi(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Шаг частот тюнера FM и AM:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FmStep.entries.forEach { step ->
                        FilterChip(
                            selected = systemState.fmStep == step,
                            onClick = { PioneerSettingsManager.setFmStep(step) },
                            label = { Text("FM: ${step.title}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmStep.entries.forEach { step ->
                        FilterChip(
                            selected = systemState.amStep == step,
                            onClick = { PioneerSettingsManager.setAmStep(step) },
                            label = { Text("AM: ${step.title}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Единицы измерения скорости и дистанции:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DistanceUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = systemState.distanceUnit == unit,
                            onClick = { PioneerSettingsManager.setDistanceUnit(unit) },
                            label = { Text(unit.title, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PioneerCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. ВКЛАДКА: ПОДСВЕТКА И ДИММЕР (ILLUMINATION)
// =============================================================================

@Composable
fun IlluminationSettingsTab() {
    val illumiState by PioneerSettingsManager.illuminationState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(title = "Аппаратный диммер SPH-10BT") {
                Text(text = "Режим приглушения подсветки:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DimmerMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { PioneerSettingsManager.setDimmerMode(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = illumiState.dimmerMode == mode,
                                onClick = { PioneerSettingsManager.setDimmerMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = PioneerCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.title, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Яркость подсветки (1 .. 10)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Яркость кнопок:", fontSize = 13.sp, color = TextPrimary)
                    Text("${illumiState.keyBrightness} / 10", fontSize = 13.sp, color = PioneerCyan, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = illumiState.keyBrightness.toFloat(),
                    onValueChange = { PioneerSettingsManager.setBrightness(1, it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = PioneerCyan, activeTrackColor = PioneerCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Яркость дисплея:", fontSize = 13.sp, color = TextPrimary)
                    Text("${illumiState.displayBrightness} / 10", fontSize = 13.sp, color = PioneerAmber, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = illumiState.displayBrightness.toFloat(),
                    onValueChange = { PioneerSettingsManager.setBrightness(2, it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = PioneerAmber, activeTrackColor = PioneerAmber)
                )
            }
        }

        item {
            SettingsCard(title = "Цвет подсветки кнопок и дисплея (Pioneer RGB)") {
                Text(text = "14 фирменных предустановленных цветов:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                // Палитра цветных кругов
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PioneerPresetColor.entries.forEach { color ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { PioneerSettingsManager.setPresetColor(3, color) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(color.hex))
                                    .border(
                                        width = if (illumiState.presetColor == color) 3.dp else 1.dp,
                                        color = if (illumiState.presetColor == color) Color.White else CardBorder,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = color.title.substringBefore(" "),
                                fontSize = 10.sp,
                                color = if (illumiState.presetColor == color) PioneerCyan else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // Custom RGB sliders (0..60 scale)
                Text(
                    text = "Пользовательский цвет RGB (Шкала Pioneer 0 .. 60):",
                    fontSize = 12.sp,
                    color = PioneerCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Красный (Red):", fontSize = 12.sp, color = Color(0xFFFF5555))
                    Text("${illumiState.customRed} / 60", fontSize = 12.sp, color = Color(0xFFFF5555), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = illumiState.customRed.toFloat(),
                    onValueChange = { PioneerSettingsManager.setCustomRgb(3, it.toInt(), illumiState.customGreen, illumiState.customBlue) },
                    valueRange = 0f..60f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5555), activeTrackColor = Color(0xFFFF5555))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Зеленый (Green):", fontSize = 12.sp, color = Color(0xFF55FF55))
                    Text("${illumiState.customGreen} / 60", fontSize = 12.sp, color = Color(0xFF55FF55), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = illumiState.customGreen.toFloat(),
                    onValueChange = { PioneerSettingsManager.setCustomRgb(3, illumiState.customRed, it.toInt(), illumiState.customBlue) },
                    valueRange = 0f..60f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF55FF55), activeTrackColor = Color(0xFF55FF55))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Синий (Blue):", fontSize = 12.sp, color = Color(0xFF55AAFF))
                    Text("${illumiState.customBlue} / 60", fontSize = 12.sp, color = Color(0xFF55AAFF), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = illumiState.customBlue.toFloat(),
                    onValueChange = { PioneerSettingsManager.setCustomRgb(3, illumiState.customRed, illumiState.customGreen, it.toInt()) },
                    valueRange = 0f..60f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF55AAFF), activeTrackColor = Color(0xFF55AAFF))
                )

                Spacer(modifier = Modifier.height(10.dp))
                SettingSwitch(
                    title = "Светомузыка (Audio Level Meter)",
                    subtitle = "Мигание и пульсация подсветки кнопок в такт музыке",
                    checked = illumiState.audioLevelMeter,
                    onCheckedChange = { PioneerSettingsManager.setAudioLevelMeter(it) }
                )
            }
        }
    }
}

// =============================================================================
// 4. ВКЛАДКА: ПАРКТРОНИК PIONEER ND-PS1 (CAR SAFETY)
// =============================================================================

@Composable
fun ParkingSensorSettingsTab() {
    val parkingState by PioneerSettingsManager.parkingState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(title = "Парковочный радар Pioneer ND-PS1") {
                SettingSwitch(
                    title = "Парктроник активен",
                    subtitle = "Поддержка фирменного ультразвукового датчика парковки SPH-10BT",
                    checked = parkingState.enabled,
                    onCheckedChange = {
                        PioneerSettingsManager.setParkingSensor(it, parkingState.alarmVolume, parkingState.outputDestination, parkingState.backPolarity)
                    }
                )

                if (parkingState.enabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Громкость звукового сигнала (1 .. 3):", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..3).forEach { vol ->
                            FilterChip(
                                selected = parkingState.alarmVolume == vol,
                                onClick = {
                                    PioneerSettingsManager.setParkingSensor(true, vol, parkingState.outputDestination, parkingState.backPolarity)
                                },
                                label = { Text("Уровень $vol", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PioneerCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Вывод звукового зуммера через динамики авто:", fontSize = 12.sp, color = TextSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AlarmOutput.entries.forEach { dest ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        PioneerSettingsManager.setParkingSensor(true, parkingState.alarmVolume, dest, parkingState.backPolarity)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = parkingState.outputDestination == dest,
                                    onClick = {
                                        PioneerSettingsManager.setParkingSensor(true, parkingState.alarmVolume, dest, parkingState.backPolarity)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = PioneerCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dest.title, fontSize = 13.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Полярность сигнала задней передачи (Reverse Wire):", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackPolarity.entries.forEach { pol ->
                            FilterChip(
                                selected = parkingState.backPolarity == pol,
                                onClick = {
                                    PioneerSettingsManager.setParkingSensor(true, parkingState.alarmVolume, parkingState.outputDestination, pol)
                                },
                                label = { Text(pol.title, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PioneerCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 5. ВКЛАДКА: ТЮНЕР (RADIO TUNER FUNCTIONS)
// =============================================================================

@Composable
fun RadioFunctionSettingsTab() {
    val radioState by PioneerSettingsManager.radioFuncState.collectAsState()
    var bsmTriggered by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(title = "Автопоиск BSM (Best Stations Memory)") {
                Text(
                    text = "Функция BSM сканирует весь диапазон FM и автоматически сохраняет 6 сильнейших радиостанций на кнопки 1..6.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        PioneerSettingsManager.triggerBsm()
                        bsmTriggered = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBorder),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PioneerCyan)
                ) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = PioneerCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Запустить автопоиск BSM", color = TextPrimary)
                }

                if (bsmTriggered) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Команда BSM отправлена в тюнер магнитолы...", fontSize = 12.sp, color = PioneerCyan)
                }
            }
        }

        item {
            SettingsCard(title = "Чувствительность местного приёма (Local Seek)") {
                Text(text = "Порог отсечки слабых станций при автопоиске:", fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LocalSeekLevel.entries.forEach { level ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { PioneerSettingsManager.setLocalSeek(level) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = radioState.localSeek == level,
                                onClick = { PioneerSettingsManager.setLocalSeek(level) },
                                colors = RadioButtonDefaults.colors(selectedColor = PioneerCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(level.title, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Функции RDS (Radio Data System)") {
                SettingSwitch(
                    title = "Дорожные сводки (TA - Traffic Announcement)",
                    subtitle = "Автоматическое прерывание передачи для важных сводок о пробках",
                    checked = radioState.ta,
                    onCheckedChange = { PioneerSettingsManager.setTa(it) }
                )
                SettingSwitch(
                    title = "Альтернативные частоты (AF - Alternative Frequencies)",
                    subtitle = "Автоподстройка частоты передатчика при ухудшении сигнала станции",
                    checked = radioState.af,
                    onCheckedChange = { PioneerSettingsManager.setAf(it) }
                )
                SettingSwitch(
                    title = "Региональные программы (REG - Regional)",
                    subtitle = "Ограничение переключения AF только региональными вещателями",
                    checked = radioState.reg,
                    onCheckedChange = { PioneerSettingsManager.setReg(it) }
                )
                SettingSwitch(
                    title = "Сводки новостей (NEWS)",
                    subtitle = "Автоматический перехват экстренных новостных программ",
                    checked = radioState.news,
                    onCheckedChange = { PioneerSettingsManager.setNews(it) }
                )
            }
        }
    }
}

// =============================================================================
// 6. ВКЛАДКА: КНОПКИ И ПЕРЕНАЗНАЧЕНИЕ PIONEER BUDDY (REMAPPER & TTS)
// =============================================================================

@Composable
fun HardwareRemapperSettingsTab() {
    val remapperState by PioneerSettingsManager.remapperState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SettingsCard(title = "Физическая кнопка NAVI (Стрелка компаса)") {
                SettingSelector(label = "Короткое нажатие", value = remapperState.navShortAction)
                SettingSelector(label = "Долгое нажатие (удержание)", value = remapperState.navLongAction)
            }
        }

        item {
            SettingsCard(title = "Физическая кнопка MESSAGE (Облако сообщения)") {
                SettingSelector(label = "Короткое нажатие", value = remapperState.msgShortAction)
                SettingSelector(label = "Долгое нажатие (удержание)", value = remapperState.msgLongAction)
            }
        }

        item {
            SettingsCard(title = "Физическая кнопка TEL (Телефонная трубка)") {
                SettingSelector(label = "Короткое нажатие", value = remapperState.telShortAction)
                SettingSelector(label = "Долгое нажатие (удержание)", value = remapperState.telLongAction)
            }
        }

        item {
            SettingsCard(title = "Физическая кнопка MIC (Микрофон)") {
                SettingSelector(label = "Короткое нажатие", value = remapperState.micShortAction)
                SettingSelector(label = "Долгое нажатие (удержание)", value = remapperState.micLongAction)
            }
        }

        item {
            SettingsCard(title = "Улучшения Pioneer Buddy (Устранение багов родного софта)") {
                SettingSwitch(
                    title = "Плавная регулировка громкости (Continuous Ramping)",
                    subtitle = "При удержании VOL+/VOL- громкость плавно нарастает (шаг 120мс), а не глушит звук (MUTE)",
                    checked = remapperState.continuousVolumeRamping,
                    onCheckedChange = { chk ->
                        PioneerSettingsManager.updateRemapper { it.copy(continuousVolumeRamping = chk) }
                    }
                )
                SettingSwitch(
                    title = "Бесконечная бегущая строка дисплея (Looping Marquee)",
                    subtitle = "Повтор прокрутки длинного названия трека на 1-строчном экране вместо замирания на 1-й строке",
                    checked = remapperState.infiniteMarqueeScroll,
                    onCheckedChange = { chk ->
                        PioneerSettingsManager.updateRemapper { it.copy(infiniteMarqueeScroll = chk) }
                    }
                )
            }
        }

        item {
            SettingsCard(title = "Голосовое чтение входящих сообщений (TTS)") {
                Text(
                    text = "Выберите мессенджеры для автоматического зачитывания вслух через динамики магнитолы:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingSwitch(
                    title = "Telegram",
                    subtitle = "Личные сообщения и рабочие чаты",
                    checked = remapperState.ttsTelegram,
                    onCheckedChange = { chk -> PioneerSettingsManager.updateRemapper { it.copy(ttsTelegram = chk) } }
                )
                SettingSwitch(
                    title = "WhatsApp",
                    subtitle = "Сообщения WhatsApp",
                    checked = remapperState.ttsWhatsApp,
                    onCheckedChange = { chk -> PioneerSettingsManager.updateRemapper { it.copy(ttsWhatsApp = chk) } }
                )
                SettingSwitch(
                    title = "VK Мессенджер / ВКонтакте",
                    subtitle = "Личные диалоги VK",
                    checked = remapperState.ttsVk,
                    onCheckedChange = { chk -> PioneerSettingsManager.updateRemapper { it.copy(ttsVk = chk) } }
                )
                SettingSwitch(
                    title = "MAX (Корпоративный мессенджер)",
                    subtitle = "Сообщения системы MAX",
                    checked = remapperState.ttsMax,
                    onCheckedChange = { chk -> PioneerSettingsManager.updateRemapper { it.copy(ttsMax = chk) } }
                )
                SettingSwitch(
                    title = "SMS и Системные уведомления",
                    subtitle = "Входящие СМС оператора связи",
                    checked = remapperState.ttsSms,
                    onCheckedChange = { chk -> PioneerSettingsManager.updateRemapper { it.copy(ttsSms = chk) } }
                )
            }
        }
    }
}

// =============================================================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ (HELPER WIDGETS)
// =============================================================================

@Composable
fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PioneerCyan
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = PioneerCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CardBorder
            )
        )
    }
}

@Composable
fun SettingSelector(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextPrimary)
        Text(value, fontSize = 13.sp, color = PioneerCyan, fontWeight = FontWeight.SemiBold)
    }
}
