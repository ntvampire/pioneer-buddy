package com.pioneer.buddy.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pioneer.buddy.model.ConnectionStatus
import com.pioneer.buddy.model.DashboardState
import com.pioneer.buddy.model.MediaSource
import com.pioneer.buddy.service.PioneerConnectionManager
import com.pioneer.buddy.service.SmartNotificationListenerService
import com.pioneer.buddy.ui.theme.*

@Composable
fun DashboardScreen(
    state: DashboardState,
    onOpenSettings: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (isLandscape) {
            // Ландшафтный режим: Левая зона (Медиа/Радио ~62%) + Правая колонка (Инфо + 6 кнопок ~38%)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.6f)
                        .fillMaxHeight()
                ) {
                    MainHeroCard(state = state, isLandscape = true)
                }

                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoBar(state = state, isCompact = true)
                    SixButtonDock(onOpenSettings = onOpenSettings, isLandscape = true)
                }
            }
        } else {
            // Портретный режим: Вертикальный стек (Инфо-бар -> Основная зона -> 6 кнопок)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                InfoBar(state = state, isCompact = false)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    MainHeroCard(state = state, isLandscape = false)
                }

                SixButtonDock(onOpenSettings = onOpenSettings, isLandscape = false)
            }
        }
    }
}

@Composable
fun InfoBar(state: DashboardState, isCompact: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Время и статус
            Column {
                Text(
                    text = "12:45",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "+21°C",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Центр: Цифровой GPS спидометр
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${state.speedKmH}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = PioneerCyan
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "км/ч",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Статус подключения SPH-10BT и громкость
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (state.connectionStatus) {
                        ConnectionStatus.CONNECTED -> Color(0xFF00E676)
                        ConnectionStatus.LISTENING -> PioneerAmber
                        ConnectionStatus.DISCONNECTED -> Color(0xFFFF5252)
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.connectionStatus == ConnectionStatus.CONNECTED) "SPH-10BT" else "ПОИСК",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "VOL: ${state.volume}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun MainHeroCard(state: DashboardState, isLandscape: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = CardBackground
    ) {
        if (state.currentSource == MediaSource.TUNER_FM || state.currentSource == MediaSource.TUNER_AM) {
            RadioHeroContent(state = state)
        } else {
            PlayerHeroContent(state = state, isLandscape = isLandscape)
        }
    }
}

@Composable
fun PlayerHeroContent(state: DashboardState, isLandscape: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Обложка трека
        Box(
            modifier = Modifier
                .size(if (isLandscape) 140.dp else 190.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF20252D))
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = PioneerCyan,
                modifier = Modifier.size(64.dp)
            )
        }

        // Название и исполнитель
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.trackTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.artistName,
                fontSize = 16.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Полоса прогресса
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { 0.35f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PioneerCyan,
                trackColor = Color(0xFF222730)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "01:45", fontSize = 12.sp, color = TextSecondary)
                Text(text = "04:12", fontSize = 12.sp, color = TextSecondary)
            }
        }

        // Нижняя панель плеера: Название сервиса + PLAY/PAUSE + ЛАЙК
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Источник звука
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF222730),
                modifier = Modifier.clickable { PioneerConnectionManager.toggleSource() }
            ) {
                Text(
                    text = state.activePlayerName.ifBlank { "Медиаплеер" },
                    fontSize = 12.sp,
                    color = PioneerCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Сенсорная кнопка PLAY / PAUSE (так как на магнитоле её нет!)
            IconButton(
                onClick = { /* Вызов MediaSession Play/Pause */ },
                modifier = Modifier
                    .size(54.dp)
                    .background(PioneerCyan, CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Кнопка ЛАЙК (работает через шторку плееров)
            IconButton(
                onClick = { SmartNotificationListenerService.triggerLikeAction() },
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFF222730), CircleShape)
            ) {
                Icon(
                    imageVector = if (state.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Лайк",
                    tint = if (state.isLiked) PioneerRed else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RadioHeroContent(state: DashboardState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Переключатель источника (Tuner -> BT)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF222730),
                modifier = Modifier.clickable { PioneerConnectionManager.toggleSource() }
            ) {
                Text(
                    text = "FM ТЮНЕР SPH-10BT",
                    fontSize = 12.sp,
                    color = PioneerAmber,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(text = "STEREO", fontSize = 11.sp, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
        }

        // Частота и станция
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.radioFrequency,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = PioneerAmber
            )
            Text(
                text = state.stationName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.radioText,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        // Сетка 6 пресетов радио P1 .. P6
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    PresetButton(
                        presetNumber = i,
                        frequency = state.presets.getOrNull(i - 1) ?: "--.-",
                        isSelected = state.currentPreset == i,
                        modifier = Modifier.weight(1f)
                    ) {
                        PioneerConnectionManager.setPreset(i)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 4..6) {
                    PresetButton(
                        presetNumber = i,
                        frequency = state.presets.getOrNull(i - 1) ?: "--.-",
                        isSelected = state.currentPreset == i,
                        modifier = Modifier.weight(1f)
                    ) {
                        PioneerConnectionManager.setPreset(i)
                    }
                }
            }
        }
    }
}

@Composable
fun PresetButton(
    presetNumber: Int,
    frequency: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .border(
                1.dp,
                if (isSelected) PioneerAmber else CardBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF332512) else Color(0xFF1E222A)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "P$presetNumber",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PioneerAmber else TextSecondary
            )
            Text(
                text = frequency,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun SixButtonDock(
    onOpenSettings: () -> Unit,
    isLandscape: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопки 1 и 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DockButton("Яндекс Карты", Icons.Default.Navigation, Modifier.weight(1f)) {}
            DockButton("2ГИС", Icons.Default.Explore, Modifier.weight(1f)) {}
        }
        // Кнопки 3 и 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DockButton("Радио FM", Icons.Default.Radio, Modifier.weight(1f)) {
                PioneerConnectionManager.toggleSource()
            }
            DockButton("DSP Звук", Icons.Default.Tune, Modifier.weight(1f)) {
                onOpenSettings()
            }
        }
        // Кнопки 5 и 6 (6-я кнопка ЖЕСТКО привязана к Настройкам!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DockButton("Мессенджер", Icons.Default.Chat, Modifier.weight(1f)) {}
            DockButton(
                title = "Настройки",
                icon = Icons.Default.Settings,
                modifier = Modifier.weight(1f),
                isFixedSettings = true,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
fun DockButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    isFixedSettings: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .border(
                1.dp,
                if (isFixedSettings) PioneerCyan.copy(alpha = 0.5f) else CardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFixedSettings) PioneerCyan else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
