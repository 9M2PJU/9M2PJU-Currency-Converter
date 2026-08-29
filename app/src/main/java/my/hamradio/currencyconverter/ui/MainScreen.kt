package my.hamradio.currencyconverter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import my.hamradio.currencyconverter.R
import my.hamradio.currencyconverter.ui.components.CurrencySelectionDialog
import my.hamradio.currencyconverter.ui.components.CustomRateDialog
import my.hamradio.currencyconverter.ui.screens.*

sealed class Screen(val route: String, val titleRes: Int, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Converter : Screen("converter", R.string.nav_converter, Icons.Filled.CurrencyExchange, Icons.Outlined.CurrencyExchange)
    data object Pair : Screen("pair", R.string.nav_pair, Icons.Filled.CompareArrows, Icons.Outlined.CompareArrows)
    data object Travel : Screen("travel", R.string.nav_travel, Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag)
    data object Trends : Screen("trends", R.string.nav_trends, Icons.Filled.ShowChart, Icons.Outlined.ShowChart)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val screens = listOf(
        Screen.Converter,
        Screen.Pair,
        Screen.Travel,
        Screen.Trends,
        Screen.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                screens.forEach { screen ->
                    val selected = currentDestination?.route == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = stringResource(screen.titleRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(screen.titleRes),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Converter.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Converter.route) {
                MultiCurrencyScreen(viewModel = viewModel, uiState = uiState)
            }
            composable(Screen.Pair.route) {
                PairConverterScreen(viewModel = viewModel, uiState = uiState)
            }
            composable(Screen.Travel.route) {
                TravelCalculatorScreen(viewModel = viewModel, uiState = uiState)
            }
            composable(Screen.Trends.route) {
                TrendsScreen(viewModel = viewModel, uiState = uiState)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel, uiState = uiState)
            }
        }

        // Global Currency Selection Dialog
        uiState.currencyPickerMode?.let { mode ->
            val selectedCode = when (mode) {
                CurrencyPickerMode.BASE -> uiState.baseCurrency.code
                CurrencyPickerMode.TARGET -> uiState.targetCurrency.code
                CurrencyPickerMode.PAIR_FROM -> uiState.baseCurrency.code
                CurrencyPickerMode.PAIR_TO -> uiState.targetCurrency.code
                CurrencyPickerMode.SHOPPING_FOREIGN -> uiState.baseCurrency.code
                CurrencyPickerMode.SHOPPING_HOME -> uiState.targetCurrency.code
            }

            CurrencySelectionDialog(
                currencies = uiState.currencies,
                selectedCode = selectedCode,
                onCurrencySelected = { currency ->
                    when (mode) {
                        CurrencyPickerMode.BASE, CurrencyPickerMode.PAIR_FROM, CurrencyPickerMode.SHOPPING_FOREIGN -> {
                            viewModel.setBaseCurrency(currency)
                        }
                        CurrencyPickerMode.TARGET, CurrencyPickerMode.PAIR_TO, CurrencyPickerMode.SHOPPING_HOME -> {
                            viewModel.setTargetCurrency(currency)
                        }
                    }
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onDismiss = { viewModel.closeCurrencyPicker() }
            )
        }

        // Global Custom Rate Editor Dialog
        uiState.currencyToEditRate?.let { curr ->
            CustomRateDialog(
                currency = curr,
                onSaveRate = { rate -> viewModel.setCustomRate(curr.code, rate) },
                onResetRate = { viewModel.resetCustomRate(curr.code) },
                onDismiss = { viewModel.closeRateEditor() }
            )
        }
    }
}
