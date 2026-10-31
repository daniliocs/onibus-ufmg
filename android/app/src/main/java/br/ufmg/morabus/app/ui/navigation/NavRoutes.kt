package br.ufmg.morabus.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.ui.theme.MorabusIcons

sealed class NavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : NavItem("home", R.string.nav_home, MorabusIcons.DirectionsBus)
    data object Map : NavItem("map", R.string.nav_map, MorabusIcons.Map)
    data object Schedules : NavItem("schedules", R.string.nav_schedules, MorabusIcons.Schedule)
    data object Notices : NavItem("notices", R.string.nav_notices, MorabusIcons.NotificationsActive)
    data object More : NavItem("more", R.string.nav_more, MorabusIcons.MoreHoriz)

    companion object {
        val items: List<NavItem>
            get() = listOf(Home, Map, Schedules, Notices, More)
    }
}
