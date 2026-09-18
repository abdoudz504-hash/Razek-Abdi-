package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.ui.util.*
import kotlinx.coroutines.delay

@Composable
fun TopStatusBar(
    modifier: Modifier = Modifier,
    onLanguageClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var showLangDialog by remember { mutableStateOf(false) }

    // Real-time ticking time and date
    LaunchedEffect(Unit) {
        while (true) {
            SystemStatusManager.updateDateTime()
            SystemStatusManager.refreshWeather(context, LanguageManager.currentLanguage.code)
            delay(1000)
        }
    }

    val timeStr = SystemStatusManager.currentTimeString
    val dateStr = SystemStatusManager.currentDateString
    val weather = SystemStatusManager.currentWeather
    val currentLang = LanguageManager.currentLanguage

    Surface(
        color = Color(0xFF04130D),
        border = BorderStroke(0.5.dp, BrandEmeraldDarker),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Row 1: Weather + Date & Clock + Language selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Weather & Temperature Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0B261B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = weather.conditionIcon,
                        contentDescription = "الطقس",
                        tint = BrandGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${weather.temperatureC}°C",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                    Text(
                        text = weather.conditionText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }

                // Clock & Date Box
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = BrandEmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = timeStr.ifEmpty { "--:--:--" },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = BrandEmeraldPrimary
                    )
                    Text(
                        text = "•",
                        color = Color(0xFF334155),
                        fontSize = 10.sp
                    )
                    Text(
                        text = dateStr.ifEmpty { "اليوم" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                // Multi-Language Switcher Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, BrandGold.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { showLangDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(currentLang.flagEmoji, fontSize = 12.sp)
                        Text(
                            text = currentLang.nativeName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "اللغات",
                            tint = BrandGold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showLangDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLang,
            onDismiss = { showLangDialog = false },
            onSelect = { lang ->
                LanguageManager.setLanguage(context, lang)
                showLangDialog = false
            }
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSelect: (AppLanguage) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurfaceCardDark),
            border = BorderStroke(1.5.dp, BrandGold),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                    Text(
                        text = "🌐 اختر لغة التطبيق / Select Language",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Box(modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "يدعم التطبيق كبرى لغات العالم مع التوافق الكامل:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = lang == currentLanguage
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BrandEmeraldDarker else Color(0xFF0F172A),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) BrandEmeraldPrimary else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(lang) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(lang.flagEmoji, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = lang.nativeName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = lang.englishName,
                                            fontSize = 11.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BrandEmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
