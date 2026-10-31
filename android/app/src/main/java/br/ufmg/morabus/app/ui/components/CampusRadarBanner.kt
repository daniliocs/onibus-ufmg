package br.ufmg.morabus.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.ufmg.morabus.app.data.CampusNotice
import br.ufmg.morabus.app.data.NoticeSeverity
import br.ufmg.morabus.app.data.NoticeSource
import br.ufmg.morabus.app.ui.theme.MorabusIcons
import br.ufmg.morabus.app.ui.theme.StatusAmber
import br.ufmg.morabus.app.ui.theme.StatusGreen
import br.ufmg.morabus.app.ui.theme.StatusRed

@Composable
fun CampusRadarBannerList(
    notices: List<CampusNotice>,
    onDismissNotice: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onNavigateToNoticesTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (notices.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Se não houver avisos urgentes, mostra estritamente no máximo 1 card.
        // Se houver aviso urgente, mostra no máximo 2 (1 urgente compacto + 1 padrão).
        val hasUrgent = notices.any { it.severity == NoticeSeverity.URGENTE || it.source == NoticeSource.CLIMA }
        val displayNotices = if (hasUrgent) notices.take(2) else notices.take(1)

        val firstIsCompact = displayNotices.firstOrNull()?.let {
            it.severity == NoticeSeverity.URGENTE || it.source == NoticeSource.CLIMA
        } == true

        displayNotices.forEachIndexed { index, notice ->
            if (index == 0 && firstIsCompact) {
                CampusRadarCompactAlertBanner(
                    notice = notice,
                    onDismiss = { onDismissNotice(notice.id) },
                    onClick = {
                        if (!notice.url.isNullOrBlank()) {
                            onOpenUrl(notice.url)
                        } else {
                            onNavigateToNoticesTab()
                        }
                    }
                )
            } else {
                CampusNoticeCard(
                    notice = notice,
                    onDismiss = { onDismissNotice(notice.id) },
                    onOpenUrl = { notice.url?.let(onOpenUrl) }
                )
            }
        }
    }
}

/**
 * Card compacto e discreto para alertas urgentes na Home (ex: Chuva, Alagamento, Cancelamento de Aulas)
 */
@Composable
fun CampusRadarCompactAlertBanner(
    notice: CampusNotice,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bannerBg, contentColor) = when {
        notice.severity == NoticeSeverity.URGENTE -> Pair(StatusRed.copy(alpha = 0.14f), StatusRed)
        notice.source == NoticeSource.CLIMA -> Pair(StatusAmber.copy(alpha = 0.16f), StatusAmber)
        else -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }

    Surface(
        color = bannerBg,
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(contentColor.copy(alpha = 0.4f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (notice.source == NoticeSource.CLIMA) MorabusIcons.WaterDrop else MorabusIcons.Warning,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notice.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = notice.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dispensar da tela inicial",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Card padrão de aviso da comunidade / FUMP / Reitoria na tela inicial
 */
@Composable
fun CampusNoticeCard(
    notice: CampusNotice,
    onDismiss: () -> Unit,
    onOpenUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeText) = when (notice.source) {
        NoticeSource.FUMP -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        NoticeSource.UFMG -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        NoticeSource.CLIMA -> Pair(StatusAmber.copy(alpha = 0.2f), StatusAmber)
        NoticeSource.COMUNIDADE -> Pair(StatusRed.copy(alpha = 0.15f), StatusRed)
    }

    val (sevBg, sevText) = when (notice.severity) {
        NoticeSeverity.URGENTE -> Pair(StatusRed.copy(alpha = 0.15f), StatusRed)
        NoticeSeverity.IMPORTANTE -> Pair(StatusAmber.copy(alpha = 0.18f), StatusAmber)
        NoticeSeverity.NORMAL -> Pair(StatusGreen.copy(alpha = 0.15f), StatusGreen)
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Cabeçalho: Selo da Fonte + Selo de Severidade + Botão Fechar (X)
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
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${notice.source.emoji} ${notice.source.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                            Text(
                                text = "• ${notice.dateFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeText.copy(alpha = 0.8f)
                            )
                        }
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
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dispensar da tela inicial",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Título
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Resumo
            Text(
                text = notice.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!notice.url.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onOpenUrl,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ler nota oficial na íntegra",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = MorabusIcons.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
