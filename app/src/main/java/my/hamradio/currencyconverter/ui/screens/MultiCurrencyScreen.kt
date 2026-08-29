package my.hamradio.currencyconverter.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hamradio.currencyconverter.R
import my.hamradio.currencyconverter.data.model.Currency
import my.hamradio.currencyconverter.ui.CurrencyPickerMode
import my.hamradio.currencyconverter.ui.MainUiState
import my.hamradio.currencyconverter.ui.MainViewModel
import my.hamradio.currencyconverter.ui.components.CameraPriceScannerDialog
import my.hamradio.currencyconverter.ui.components.QuickAmountChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiCurrencyScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }
    var showCameraScanner by remember { mutableStateOf(false) }

    val regions = listOf("All", "Favorites", "Southeast Asia", "Asia", "Europe", "Americas", "Middle East", "Africa", "Oceania", "Crypto", "Commodities")

    val filteredList = remember(searchQuery, selectedRegion, uiState.currencies, uiState.baseCurrency) {
        uiState.currencies.filter { currency ->
            if (currency.code == uiState.baseCurrency.code) return@filter false

            val matchesSearch = searchQuery.isBlank() ||
                    currency.code.contains(searchQuery, ignoreCase = true) ||
                    currency.name.contains(searchQuery, ignoreCase = true)

            val matchesRegion = when (selectedRegion) {
                "All" -> true
                "Favorites" -> currency.isFavorite
                else -> currency.region.equals(selectedRegion, ignoreCase = true)
            }

            matchesSearch && matchesRegion
        }.sortedWith(compareByDescending<Currency> { it.isFavorite }.thenBy { it.code })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Base Currency Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Row: Base Currency Selector Button & Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { viewModel.openCurrencyPicker(CurrencyPickerMode.BASE) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = uiState.baseCurrency.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.baseCurrency.code,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Base",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = uiState.baseCurrency.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Editable Amount Input Field with system numeric keyboard
                OutlinedTextField(
                    value = uiState.inputExpression,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() || it == '.' || it == ',' || it in "+-*/ " }) {
                            viewModel.setInputExpression(newVal.replace(',', '.'))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    leadingIcon = {
                        Text(
                            text = uiState.baseCurrency.symbol,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.inputExpression.isNotEmpty() && uiState.inputExpression != "0") {
                                IconButton(onClick = { viewModel.setInputExpression("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Amount",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            IconButton(onClick = { showCameraScanner = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.DocumentScanner,
                                    contentDescription = "Scan Price Tag",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                )

                if (uiState.inputExpression.contains("+") ||
                    uiState.inputExpression.contains("-") ||
                    uiState.inputExpression.contains("×") ||
                    uiState.inputExpression.contains("÷") ||
                    uiState.inputExpression.contains("*") ||
                    uiState.inputExpression.contains("/")
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "= ${uiState.baseCurrency.symbol} ${viewModel.formatValue(uiState.evaluatedAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Amount Chips
                QuickAmountChips(
                    onAmountSelected = { viewModel.setQuickAmount(it) },
                    currencySymbol = uiState.baseCurrency.symbol
                )
            }
        }

        // Region Filter Chips & Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_currencies),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(regions) { region ->
                val isSelected = selectedRegion == region
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedRegion = region },
                    label = {
                        Text(
                            when (region) {
                                "All" -> stringResource(R.string.all_regions)
                                "Favorites" -> stringResource(R.string.favorites_only)
                                "Southeast Asia" -> stringResource(R.string.region_sea)
                                "Asia" -> stringResource(R.string.region_asia)
                                "Europe" -> stringResource(R.string.region_europe)
                                "Americas" -> stringResource(R.string.region_americas)
                                "Middle East" -> stringResource(R.string.region_middle_east)
                                "Africa" -> stringResource(R.string.region_africa)
                                "Oceania" -> stringResource(R.string.region_oceania)
                                "Crypto" -> stringResource(R.string.region_crypto)
                                "Commodities" -> stringResource(R.string.region_commodities)
                                else -> region
                            },
                            fontSize = 12.sp
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Converted Currencies List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.code }) { currency ->
                val convertedValue = viewModel.convertAmount(
                    uiState.evaluatedAmount,
                    uiState.baseCurrency.code,
                    currency.code
                )
                val unitRate = viewModel.getExchangeRate(
                    uiState.baseCurrency.code,
                    currency.code
                )

                CurrencyRateCard(
                    currency = currency,
                    convertedValue = convertedValue,
                    unitRate = unitRate,
                    baseCode = uiState.baseCurrency.code,
                    formattedValue = viewModel.formatValue(convertedValue),
                    onClick = {
                        // Switch base currency on tap
                        viewModel.setBaseCurrency(currency)
                    },
                    onFavoriteToggle = { viewModel.toggleFavorite(currency.code) },
                    onEditRate = { viewModel.openRateEditor(currency) },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Currency Amount", "${viewModel.formatValue(convertedValue)} ${currency.code}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        if (showCameraScanner) {
            CameraPriceScannerDialog(
                foreignCurrency = uiState.baseCurrency,
                homeCurrency = uiState.targetCurrency,
                convertAmount = { amt, from, to -> viewModel.convertAmount(amt, from, to) },
                formatValue = { amt -> viewModel.formatValue(amt) },
                onPriceSelected = { detectedPrice ->
                    val formatted = viewModel.formatValue(detectedPrice).replace(",", "")
                    viewModel.setInputExpression(formatted)
                    showCameraScanner = false
                },
                onDismiss = { showCameraScanner = false }
            )
        }
    }
}

@Composable
private fun CurrencyRateCard(
    currency: Currency,
    convertedValue: Double,
    unitRate: Double,
    baseCode: String,
    formattedValue: String,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onEditRate: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag & Base switch hint
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = currency.flag, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Code & Name
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currency.code,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currency.symbol,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (currency.isCustomRate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "CUSTOM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "1 $baseCode = ${String.format(java.util.Locale.US, "%.4f", unitRate)} ${currency.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Converted Big Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedValue,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onEditRate, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Rate",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (currency.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (currency.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
