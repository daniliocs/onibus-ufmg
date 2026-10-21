package br.ufmg.morabus.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import br.ufmg.morabus.app.R

sealed class NavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Home : NavItem("home", R.string.nav_home, Icons.Default.DirectionsBus)
    object Map : NavItem("map", R.string.nav_map, Icons.Default.Map)
    object Schedules : NavItem("schedules", R.string.nav_schedules, Icons.Default.Schedule)
    object More : NavItem("more", R.string.nav_more, Icons.Default.MoreHoriz)

    companion object {
        val items = listOf(Home, Map, Schedules, More)
    }
}
