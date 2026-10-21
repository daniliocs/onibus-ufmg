package br.ufmg.morabus.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import br.ufmg.morabus.app.ui.navigation.NavItem
import br.ufmg.morabus.app.ui.screens.HomeScreen
import br.ufmg.morabus.app.ui.screens.MapScreen
import br.ufmg.morabus.app.ui.screens.MoreScreen
import br.ufmg.morabus.app.ui.screens.SchedulesScreen
import br.ufmg.morabus.app.ui.theme.MorabusTheme

@Composable
fun MorabusMainApp() {
    MorabusTheme {
        var currentRoute by remember { mutableStateOf(NavItem.Home.route) }

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = NavigationBarDefaults.Elevation
                ) {
                    NavItem.items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentRoute = item.route },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.titleRes)
                                )
                            },
                            label = { Text(stringResource(item.titleRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Crossfade(
                targetState = currentRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                label = "NavigationTransition"
            ) { route ->
                when (route) {
                    NavItem.Home.route -> HomeScreen(
                        onNavigateToMap = { currentRoute = NavItem.Map.route },
                        onNavigateToSchedules = { currentRoute = NavItem.Schedules.route }
                    )
                    NavItem.Map.route -> MapScreen()
                    NavItem.Schedules.route -> SchedulesScreen()
                    NavItem.More.route -> MoreScreen()
                }
            }
        }
    }
}
