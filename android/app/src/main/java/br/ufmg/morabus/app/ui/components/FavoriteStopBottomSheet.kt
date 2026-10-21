package br.ufmg.morabus.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.ufmg.morabus.app.R

data class BusStop(
    val name: String,
    val isHousing: Boolean
)

val allStops = listOf(
    // Moradias
    BusStop("Moradia 1", isHousing = true),
    BusStop("Moradias 2 e 3", isHousing = true),
    // Campus Pampulha
    BusStop("Escola de Música", isHousing = false),
    BusStop("Belas Artes", isHousing = false),
    BusStop("FaE", isHousing = false),
    BusStop("RU 1", isHousing = false),
    BusStop("FACE", isHousing = false),
    BusStop("Reitoria", isHousing = false),
    BusStop("CAD 1", isHousing = false),
    BusStop("Farmácia", isHousing = false),
    BusStop("Veterinária", isHousing = false),
    BusStop("Odonto", isHousing = false),
    BusStop("Educação Física", isHousing = false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteStopBottomSheet(
    currentFavorite: String?,
    onSelectStop: (String) -> Unit,
    onRemoveFavorite: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var searchQuery by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val maxHeight = (configuration.screenHeightDp * 0.85).dp

    val filteredStops = remember(searchQuery) {
        if (searchQuery.isBlank()) allStops
        else allStops.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val housingStops = remember(filteredStops) { filteredStops.filter { it.isHousing } }
    val campusStops = remember(filteredStops) { filteredStops.filter { !it.isHousing } }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Cabeçalho com Título e Botão Fechar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.favorite_stop_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Campo de Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.favorite_stop_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Chip "Usar o mais próximo de mim"
            AssistChip(
                onClick = {
                    // Seleciona Moradias 2 e 3 ou o ponto mais próximo estimado
                    onSelectStop("Moradias 2 e 3")
                },
                label = {
                    Text(
                        stringResource(R.string.favorite_stop_use_nearest),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .heightIn(min = 40.dp),
                shape = RoundedCornerShape(10.dp)
            )

            // Lista Rolável com Seções
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (housingStops.isNotEmpty()) {
                    item {
                        SectionHeader(stringResource(R.string.favorite_stop_section_housing))
                    }
                    items(housingStops) { stop ->
                        StopRowItem(
                            stopName = stop.name,
                            isSelected = stop.name == currentFavorite,
                            onSelect = { onSelectStop(stop.name) }
                        )
                    }
                }

                if (campusStops.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader(stringResource(R.string.favorite_stop_section_campus))
                    }
                    items(campusStops) { stop ->
                        StopRowItem(
                            stopName = stop.name,
                            isSelected = stop.name == currentFavorite,
                            onSelect = { onSelectStop(stop.name) }
                        )
                    }
                }
            }

            // Botão "Remover favorito" se existir algum selecionado
            if (!currentFavorite.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onRemoveFavorite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.favorite_stop_remove),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
private fun StopRowItem(
    stopName: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() },
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stopName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selecionado",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
