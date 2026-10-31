package br.ufmg.morabus.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.ufmg.morabus.app.data.CampusNotice
import br.ufmg.morabus.app.data.CampusRadarEngine
import br.ufmg.morabus.app.data.NoticeSeverity
import br.ufmg.morabus.app.data.NoticeSource
import br.ufmg.morabus.app.data.UserPreferencesRepository
import br.ufmg.morabus.app.ui.theme.MorabusIcons
import br.ufmg.morabus.app.ui.theme.StatusAmber
import br.ufmg.morabus.app.ui.theme.StatusGreen
import br.ufmg.morabus.app.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvisosScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allNotices by CampusRadarEngine.allNotices.collectAsState()
    val retentionPeriod by UserPreferencesRepository.noticesRetention.collectAsState()

    // Atualiza imediatamente ao abrir a aba
    LaunchedEffect(Unit) {
        CampusRadarEngine.triggerImmediateCheck(context)
    }

    // Filtra pelo período de retenção (mantendo Urgentes/Vigentes no topo)
    val tabNotices = remember(allNotices, retentionPeriod) {
        CampusRadarEngine.getNoticesForTab(retentionPeriod)
    }

    val rawFilter by UserPreferencesRepository.avisosSourceFilter.collectAsState()
    val selectedFilter = when (rawFilter) {
        "FUMP" -> NoticeSource.FUMP
        "UFMG" -> NoticeSource.UFMG
        "CLIMA" -> NoticeSource.CLIMA
        else -> null
    }

    val filteredList = remember(tabNotices, selectedFilter) {
        if (selectedFilter == null) tabNotices else tabNotices.filter { it.source == selectedFilter }
    }

    val pinnedNotices = remember(filteredList) {
        filteredList.filter { it.isValidTodayOrFuture || it.severity == NoticeSeverity.URGENTE }
    }

    val regularNotices = remember(filteredList) {
        filteredList.filter { !it.isValidTodayOrFuture && it.severity != NoticeSeverity.URGENTE }
    }

    val retentionLabel = when (retentionPeriod) {
        "day" -> "Último dia (24h)"
        "month" -> "Último mês (30 dias)"
        else -> "Última semana (7 dias)"
    }

    val chipsScrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // CABEÇALHO DA TELA
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mural de Avisos",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = retentionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = "FUMP Oficial • Reitoria da UFMG • Radar Clima Pampulha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // CHIPS DE FILTRO POR FONTE COM SCROLL HORIZONTAL (sem esmagar o botão Clima na vertical)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipsScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { UserPreferencesRepository.setAvisosSourceFilter(context, null) },
                    label = { Text("Todos (${tabNotices.size})") }
                )
                FilterChip(
                    selected = selectedFilter == NoticeSource.FUMP,
                    onClick = { UserPreferencesRepository.setAvisosSourceFilter(context, if (selectedFilter == NoticeSource.FUMP) null else "FUMP") },
                    label = { Text("🏛️ FUMP") }
                )
                FilterChip(
                    selected = selectedFilter == NoticeSource.UFMG,
                    onClick = { UserPreferencesRepository.setAvisosSourceFilter(context, if (selectedFilter == NoticeSource.UFMG) null else "UFMG") },
                    label = { Text("🎓 Reitoria") }
                )
                FilterChip(
                    selected = selectedFilter == NoticeSource.CLIMA,
                    onClick = { UserPreferencesRepository.setAvisosSourceFilter(context, if (selectedFilter == NoticeSource.CLIMA) null else "CLIMA") },
                    label = { Text("🌧️ Clima") }
                )
            }
        }

        // SEÇÃO: DESTAQUES / VIGENTES HOJE
        if (pinnedNotices.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = "📌 Em Destaque & Vigentes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(pinnedNotices, key = { "pinned_" + it.id }) { notice ->
                AvisoDetailCard(
                    notice = notice,
                    isPinned = true,
                    onOpenUrl = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }
        }

        // SEÇÃO: HISTÓRICO GERAL
        if (regularNotices.isNotEmpty()) {
            item {
                Text(
                    text = "Histórico de Comunicados",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )
            }

            items(regularNotices, key = { "reg_" + it.id }) { notice ->
                AvisoDetailCard(
                    notice = notice,
                    isPinned = false,
                    onOpenUrl = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }
        }

        // EMPTY STATE
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = MorabusIcons.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum comunicado no período",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Você pode alterar o período de histórico na aba Mais",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun AvisoDetailCard(
    notice: CampusNotice,
    isPinned: Boolean,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeText) = when (notice.source) {
        NoticeSource.FUMP -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        NoticeSource.UFMG -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        NoticeSource.CLIMA -> Pair(StatusAmber.copy(alpha = 0.2f), StatusAmber)
        NoticeSource.COMUNIDADE -> Pair(StatusRed.copy(alpha = 0.15f), StatusRed)
    }

    val (sevBg, sevText) = when (notice.severity) {
        NoticeSeverity.URGENTE -> Pair(StatusRed.copy(alpha = 0.16f), StatusRed)
        NoticeSeverity.IMPORTANTE -> Pair(StatusAmber.copy(alpha = 0.18f), StatusAmber)
        NoticeSeverity.NORMAL -> Pair(StatusGreen.copy(alpha = 0.15f), StatusGreen)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        border = if (notice.severity == NoticeSeverity.URGENTE) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(StatusRed.copy(alpha = 0.6f))
            )
        } else if (isPinned) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        } else CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badges e Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${notice.source.emoji} ${notice.source.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = sevBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${notice.severity.emoji} ${notice.severity.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sevText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = notice.dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Resumo
            Text(
                text = notice.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!notice.url.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = { onOpenUrl(notice.url) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Ler nota oficial na íntegra",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = MorabusIcons.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
