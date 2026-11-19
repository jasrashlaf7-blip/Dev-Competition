package com.olympiades.firststep.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.olympiades.firststep.FirstStepContainer
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.ui.i18n.LocalAppStrings
import com.olympiades.firststep.ui.i18n.stringsFor
import com.olympiades.firststep.ui.screens.ApplicationFormScreen
import com.olympiades.firststep.ui.screens.ApplicationsScreen
import com.olympiades.firststep.ui.screens.FavoritesScreen
import com.olympiades.firststep.ui.screens.OfferDetailScreen
import com.olympiades.firststep.ui.screens.OffersScreen
import com.olympiades.firststep.ui.screens.ProfileScreen
import com.olympiades.firststep.ui.theme.FirstStepTheme
import com.olympiades.firststep.ui.viewmodel.AppPreferencesViewModel
import com.olympiades.firststep.ui.viewmodel.ApplicationFormViewModel
import com.olympiades.firststep.ui.viewmodel.ApplicationsViewModel
import com.olympiades.firststep.ui.viewmodel.FavoritesViewModel
import com.olympiades.firststep.ui.viewmodel.FirstStepViewModelFactory
import com.olympiades.firststep.ui.viewmodel.OfferDetailViewModel
import com.olympiades.firststep.ui.viewmodel.OffersViewModel
import com.olympiades.firststep.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

object FirstStepRoutes {
    const val OFFERS = "offers"
    const val FAVORITES = "favorites"
    const val APPLICATIONS = "applications"
    const val PROFILE = "profile"
    const val OFFER_DETAIL = "offer_detail/{offerId}"
    const val APPLICATION_FORM = "application_form/{offerId}"

    fun offerDetail(offerId: Long): String = "offer_detail/$offerId"
    fun applicationForm(offerId: Long): String = "application_form/$offerId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
    val testTag: String
)

@Composable
fun FirstStepAppRoot(container: FirstStepContainer) {
    val factory = remember(container) { FirstStepViewModelFactory(container) }
    val appPrefsViewModel: AppPreferencesViewModel = viewModel(factory = factory)
    val prefs by appPrefsViewModel.preferences.collectAsStateWithLifecycle()
    val favoriteCount by appPrefsViewModel.favoriteCount.collectAsStateWithLifecycle()

    val layoutDirection = if (prefs.language == AppLanguage.AR) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    val strings = stringsFor(prefs.language)

    FirstStepTheme(
        themeMode = prefs.themeMode,
        language = prefs.language
    ) {
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalAppStrings provides strings
        ) {
            FirstStepNavScaffold(
                container = container,
                favoriteCount = favoriteCount
            )
        }
    }
}

@Composable
private fun FirstStepNavScaffold(
    container: FirstStepContainer,
    favoriteCount: Int
) {
    val strings = LocalAppStrings.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val rootFactory = remember(container) { FirstStepViewModelFactory(container) }
    val offersViewModel: OffersViewModel = viewModel(factory = rootFactory)
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = rootFactory)
    val applicationsViewModel: ApplicationsViewModel = viewModel(factory = rootFactory)
    val profileViewModel: ProfileViewModel = viewModel(factory = rootFactory)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        BottomNavTab(
            route = FirstStepRoutes.OFFERS,
            label = strings.tabHome,
            selectedIcon = Icons.Filled.Work,
            unselectedIcon = Icons.Outlined.WorkOutline,
            testTag = "nav_tab_offers"
        ),
        BottomNavTab(
            route = FirstStepRoutes.FAVORITES,
            label = strings.tabFavorites,
            selectedIcon = Icons.Filled.Favorite,
            unselectedIcon = Icons.Outlined.FavoriteBorder,
            badgeCount = favoriteCount,
            testTag = "nav_tab_favorites"
        ),
        BottomNavTab(
            route = FirstStepRoutes.APPLICATIONS,
            label = strings.tabApplications,
            selectedIcon = Icons.AutoMirrored.Filled.Assignment,
            unselectedIcon = Icons.AutoMirrored.Outlined.Assignment,
            testTag = "nav_tab_applications"
        ),
        BottomNavTab(
            route = FirstStepRoutes.PROFILE,
            label = strings.tabProfile,
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            testTag = "nav_tab_profile"
        )
    )

    val showTopLevelNav = currentRoute in setOf(
        FirstStepRoutes.OFFERS,
        FirstStepRoutes.FAVORITES,
        FirstStepRoutes.APPLICATIONS,
        FirstStepRoutes.PROFILE
    )

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == tab.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == tab.route
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    TabIconWithBadge(tab = tab, selected = selected)
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = FirstStepRoutes.OFFERS,
                        enterTransition = {
                            fadeIn(animationSpec = tween(220)) +
                                scaleIn(initialScale = 0.98f, animationSpec = tween(220))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(180))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(220))
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(180))
                        }
                    ) {
                        composable(FirstStepRoutes.OFFERS) {
                            OffersScreen(
                                viewModel = offersViewModel,
                                onOfferClick = { offerId ->
                                    navController.navigate(FirstStepRoutes.offerDetail(offerId))
                                }
                            )
                        }

                        composable(FirstStepRoutes.FAVORITES) {
                            FavoritesScreen(
                                viewModel = favoritesViewModel,
                                onOfferClick = { offerId ->
                                    navController.navigate(FirstStepRoutes.offerDetail(offerId))
                                }
                            )
                        }

                        composable(FirstStepRoutes.APPLICATIONS) {
                            ApplicationsScreen(
                                viewModel = applicationsViewModel,
                                onOfferClick = { offerId ->
                                    navController.navigate(FirstStepRoutes.offerDetail(offerId))
                                }
                            )
                        }

                        composable(FirstStepRoutes.PROFILE) {
                            ProfileScreen(viewModel = profileViewModel)
                        }

                        composable(
                            route = FirstStepRoutes.OFFER_DETAIL,
                            arguments = listOf(
                                navArgument("offerId") { type = NavType.LongType }
                            ),
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it / 5 },
                                    animationSpec = tween(240)
                                ) + fadeIn(animationSpec = tween(240))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(180))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(220))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 5 },
                                    animationSpec = tween(220)
                                ) + fadeOut(animationSpec = tween(200))
                            }
                        ) { backStackEntry ->
                            val offerId = backStackEntry.arguments?.getLong("offerId") ?: return@composable
                            val detailFactory = remember(container, offerId) {
                                FirstStepViewModelFactory(container, offerId = offerId)
                            }
                            val detailViewModel: OfferDetailViewModel = viewModel(factory = detailFactory)
                            OfferDetailScreen(
                                viewModel = detailViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onApplyClick = { id ->
                                    navController.navigate(FirstStepRoutes.applicationForm(id))
                                }
                            )
                        }

                        composable(
                            route = FirstStepRoutes.APPLICATION_FORM,
                            arguments = listOf(
                                navArgument("offerId") { type = NavType.LongType }
                            ),
                            enterTransition = {
                                slideInHorizontally(
                                    initialOffsetX = { it / 5 },
                                    animationSpec = tween(240)
                                ) + fadeIn(animationSpec = tween(240))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(180))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(220))
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    targetOffsetX = { it / 5 },
                                    animationSpec = tween(220)
                                ) + fadeOut(animationSpec = tween(200))
                            }
                        ) { backStackEntry ->
                            val offerId = backStackEntry.arguments?.getLong("offerId") ?: return@composable
                            val formFactory = remember(container, offerId) {
                                FirstStepViewModelFactory(container, offerId = offerId)
                            }
                            val formViewModel: ApplicationFormViewModel = viewModel(factory = formFactory)
                            ApplicationFormScreen(
                                viewModel = formViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSubmittedSuccess = { reference ->
                                    navController.navigate(FirstStepRoutes.APPLICATIONS) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            strings.submitSuccessSnackbar(reference)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: BottomNavTab,
    selected: Boolean
) {
    val iconVector = if (selected) tab.selectedIcon else tab.unselectedIcon
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tab_icon_scale"
    )
    BadgedBox(
        badge = {
            AnimatedVisibility(
                visible = tab.badgeCount > 0,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(tween(150)),
                exit = scaleOut(tween(150)) + fadeOut(tween(150))
            ) {
                Badge {
                    AnimatedContent(
                        targetState = tab.badgeCount,
                        transitionSpec = {
                            (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)))
                                .togetherWith(fadeOut(tween(100)))
                        },
                        label = "tab_badge_count"
                    ) { count ->
                        Text(text = count.toString())
                    }
                }
            }
        }
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = tab.label,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            }
        )
    }
}
