package br.ufmg.morabus.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.ufmg.morabus.app.data.UserPreferencesRepository
import br.ufmg.morabus.app.ui.navigation.NavItem
import br.ufmg.morabus.app.ui.screens.AvisosScreen
import br.ufmg.morabus.app.ui.screens.HomeScreen
import br.ufmg.morabus.app.ui.screens.MapScreen
import br.ufmg.morabus.app.ui.screens.MoreScreen
import br.ufmg.morabus.app.ui.screens.SchedulesScreen
import br.ufmg.morabus.app.ui.theme.MorabusTheme

@Composable
fun MorabusMainApp(
    initialRoute: String = NavItem.Home.route,
    externalRouteOverride: String? = null,
    onExternalRouteConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UserPreferencesRepository.init(context)
    }

    val themeMode by UserPreferencesRepository.themeMode.collectAsState()

    MorabusTheme(themeMode = themeMode) {
        var currentRoute by remember(initialRoute) { mutableStateOf(initialRoute) }

        LaunchedEffect(externalRouteOverride) {
            if (!externalRouteOverride.isNullOrBlank()) {
                currentRoute = externalRouteOverride
                onExternalRouteConsumed()
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentRoute) {
                    NavItem.Home.route -> HomeScreen(
                        onNavigateToMap = { currentRoute = NavItem.Map.route },
                        onNavigateToSchedules = { currentRoute = NavItem.Schedules.route },
                        onNavigateToNotices = { currentRoute = NavItem.Notices.route }
                    )
                    NavItem.Map.route -> MapScreen()
                    NavItem.Schedules.route -> SchedulesScreen()
                    NavItem.Notices.route -> AvisosScreen()
                    NavItem.More.route -> MoreScreen()
                }
            }
        }
    }
}
