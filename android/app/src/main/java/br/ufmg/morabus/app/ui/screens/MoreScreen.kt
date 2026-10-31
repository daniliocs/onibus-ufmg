package br.ufmg.morabus.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.data.UserPreferencesRepository
import br.ufmg.morabus.app.ui.theme.MorabusIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UserPreferencesRepository.init(context)
    }

    val radarEnabled by UserPreferencesRepository.radarEnabled.collectAsState()
    val themeMode by UserPreferencesRepository.themeMode.collectAsState()
    val vibrationEnabled by UserPreferencesRepository.vibrationEnabled.collectAsState()

    // Configurações da v3.0: Avisos, Severidade e Canais
    val noticesRetention by UserPreferencesRepository.noticesRetention.collectAsState()
    val noticesMinSeverity by UserPreferencesRepository.noticesMinSeverity.collectAsState()
    val notifyFump by UserPreferencesRepository.notifyFump.collectAsState()
    val notifyUfmg by UserPreferencesRepository.notifyUfmg.collectAsState()
    val notifyWeather by UserPreferencesRepository.notifyWeather.collectAsState()
    val communityUsers by UserPreferencesRepository.communityUserCount.collectAsState()

    val isCheckingUpdate by br.ufmg.morabus.app.data.AppUpdateManager.isChecking.collectAsState()
    val availableUpdate by br.ufmg.morabus.app.data.AppUpdateManager.availableUpdate.collectAsState()
    var updateStatusMsg by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Text(
            text = stringResource(R.string.nav_more),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // CARD DE CONFIGURAÇÕES DE ÔNIBUS
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Preferências de Viagem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // 1. RADAR EM SEGUNDO PLANO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            MorabusIcons.Radar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text("Radar de Horários de Pico", fontWeight = FontWeight.Medium)
                            Text(
                                "Alerta prévio antes dos ônibus nos horários de maior fluxo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = radarEnabled,
                        onCheckedChange = { UserPreferencesRepository.setRadarEnabled(context, it) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // 2. VIBRAÇÃO NAS NOTIFICAÇÕES E AVISOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            MorabusIcons.Vibration,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text("Vibração nas Notificações", fontWeight = FontWeight.Medium)
                            Text(
                                "Vibrar ao receber alertas de partida, comunicados e chuvas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { UserPreferencesRepository.setVibrationEnabled(context, it) }
                    )
                }
            }
        }

        // NOVO: CARD DE CONFIGURAÇÕES DE AVISOS E NOTIFICAÇÕES (v3.0)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Avisos & Notificações Automáticas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure que tipo de comunicados você quer receber e o nível de importância das notificações.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. NÍVEL MÍNIMO PARA DISPARO DE NOTIFICAÇÕES
                Text(
                    text = "Nível Mínimo para Notificar no Android:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = noticesMinSeverity == "urgente",
                        onClick = { UserPreferencesRepository.setNoticesMinSeverity(context, "urgente") },
                        label = {
                            Text(
                                "🔴 Urgentes",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = noticesMinSeverity == "importante",
                        onClick = { UserPreferencesRepository.setNoticesMinSeverity(context, "importante") },
                        label = {
                            Text(
                                "🟡 Padrão",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = noticesMinSeverity == "all",
                        onClick = { UserPreferencesRepository.setNoticesMinSeverity(context, "all") },
                        label = {
                            Text(
                                "🟢 Todos",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // 2. PERÍODO DE HISTÓRICO NA ABA AVISOS
                Text(
                    text = "Histórico Exibido na Aba Avisos:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Avisos urgentes ou vigentes continuam fixados no topo independentemente da escolha.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = noticesRetention == "day",
                        onClick = { UserPreferencesRepository.setNoticesRetention(context, "day") },
                        label = {
                            Text(
                                "Último dia",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = noticesRetention == "week",
                        onClick = { UserPreferencesRepository.setNoticesRetention(context, "week") },
                        label = {
                            Text(
                                "1 semana",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = noticesRetention == "month",
                        onClick = { UserPreferencesRepository.setNoticesRetention(context, "month") },
                        label = {
                            Text(
                                "1 mês",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // 3. CANAIS DE NOTIFICAÇÃO (SWITCHES)
                Text(
                    text = "Canais Oficiais Ativos:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // FUMP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🏛️ FUMP Oficial", fontWeight = FontWeight.Medium)
                        Text(
                            "Editais de bolsas, cardápios e recesso de atendimento",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyFump,
                        onCheckedChange = { UserPreferencesRepository.setNotifyFump(context, it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // UFMG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🎓 Reitoria da UFMG", fontWeight = FontWeight.Medium)
                        Text(
                            "Suspensão de aulas, feriados transferidos e comunicados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyUfmg,
                        onCheckedChange = { UserPreferencesRepository.setNotifyUfmg(context, it) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CLIMA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🌧️ Radar Clima & Pampulha", fontWeight = FontWeight.Medium)
                        Text(
                            "Alagamentos na Av. Fleming e tempestades severas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyWeather,
                        onCheckedChange = { UserPreferencesRepository.setNotifyWeather(context, it) }
                    )
                }
            }
        }

        // CARD DE SELEÇÃO DE TEMA (Material 3 Segmented / FilterChip)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Aparência e Tema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Escolha o esquema de cores que melhor se adapta à sua visualização.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = themeMode == "system",
                        onClick = { UserPreferencesRepository.setThemeMode(context, "system") },
                        label = { Text("Sistema") },
                        leadingIcon = {
                            Icon(MorabusIcons.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = themeMode == "dark",
                        onClick = { UserPreferencesRepository.setThemeMode(context, "dark") },
                        label = { Text("Escuro") },
                        leadingIcon = {
                            Icon(MorabusIcons.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = themeMode == "light",
                        onClick = { UserPreferencesRepository.setThemeMode(context, "light") },
                        label = { Text("Claro") },
                        leadingIcon = {
                            Icon(MorabusIcons.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // CARD DA COMUNIDADE MORABUS (Contador de Moradores Conectados)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        MorabusIcons.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Comunidade Morabus",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("👥", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Column {
                        Text(
                            text = if (communityUsers == 1) "1 morador conectado" else "$communityUsers moradores conectados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instalações registradas em todas as versões",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // CARD SOBRE O MORABUS UFMG (v3.1)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sobre o Morabus UFMG",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aplicativo independente e colaborativo desenvolvido para atender os estudantes das Moradias Universitárias da UFMG (Ouro Preto / Av. Fleming) em deslocamento para o Campus Pampulha.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val currentVersionName = remember {
                            try {
                                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "3.1.6"
                            } catch (_: Exception) {
                                "3.1.6"
                            }
                        }
                        Text(
                            text = "Versão $currentVersionName Nativa",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Android 16/17 Live Updates Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            updateStatusMsg = null
                            br.ufmg.morabus.app.data.AppUpdateManager.checkForUpdates(context) { hasUpdate, update ->
                                updateStatusMsg = if (hasUpdate && update != null) {
                                    "Nova versão ${update.versionName} disponível!"
                                } else {
                                    "Você está na versão mais recente!"
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isCheckingUpdate) "Checando..." else "Verificar Atualização",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!updateStatusMsg.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateStatusMsg!!,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
}
