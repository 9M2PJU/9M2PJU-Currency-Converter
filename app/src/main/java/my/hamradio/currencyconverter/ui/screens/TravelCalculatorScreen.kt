package my.hamradio.currencyconverter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hamradio.currencyconverter.R
import my.hamradio.currencyconverter.data.model.ShoppingItem
import my.hamradio.currencyconverter.ui.CurrencyPickerMode
import my.hamradio.currencyconverter.ui.MainUiState
import my.hamradio.currencyconverter.ui.MainViewModel

@Composable
fun TravelCalculatorScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    var itemName by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    var taxPercent by remember { mutableDoubleStateOf(0.0) }

    val foreignCurrency = uiState.baseCurrency
    val homeCurrency = uiState.targetCurrency

    val rawPrice = priceInput.toDoubleOrNull() ?: 0.0
    val discountedPrice = rawPrice * (1.0 - (discountPercent / 100.0))
    val finalForeignPrice = discountedPrice * (1.0 + (taxPercent / 100.0))
    val finalHomePrice = viewModel.convertAmount(finalForeignPrice, foreignCurrency.code, homeCurrency.code)

    val totalTripHome = uiState.shoppingItems.sumOf { it.homePrice }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shopping Calculator Input Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.travel_calc_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Foreign & Home Currency Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { viewModel.openCurrencyPicker(CurrencyPickerMode.BASE) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = foreignCurrency.flag, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = "Foreign", style = MaterialTheme.typography.labelSmall)
                                    Text(text = foreignCurrency.code, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        Surface(
                            onClick = { viewModel.openCurrencyPicker(CurrencyPickerMode.TARGET) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = homeCurrency.flag, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(text = "Home", style = MaterialTheme.typography.labelSmall)
                                    Text(text = homeCurrency.code, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Item Description & Price Input
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text(stringResource(R.string.item_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        placeholder = { Text(stringResource(R.string.foreign_price)) },
                        prefix = { Text("${foreignCurrency.symbol} ", fontWeight = FontWeight.SemiBold) },
                        suffix = { Text(foreignCurrency.code, fontWeight = FontWeight.Medium) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Discount & Tax Sliders / Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.discount_percent)}: ${discountPercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${stringResource(R.string.tax_percent)}: ${taxPercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Slider(
                                value = discountPercent.toFloat(),
                                onValueChange = { discountPercent = it.toDouble() },
                                valueRange = 0f..70f,
                                steps = 13
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Slider(
                                value = taxPercent.toFloat(),
                                onValueChange = { taxPercent = it.toDouble() },
                                valueRange = 0f..30f,
                                steps = 5
                            )
                        }
                    }

                    // Result Preview Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Foreign Final: ${foreignCurrency.symbol} ${viewModel.formatValue(finalForeignPrice)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Home Value:",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            Text(
                                text = "${homeCurrency.symbol} ${viewModel.formatValue(finalHomePrice)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Add Button
                    Button(
                        onClick = {
                            if (rawPrice > 0) {
                                viewModel.addShoppingItem(
                                    name = itemName,
                                    foreignPrice = rawPrice,
                                    discountPercent = discountPercent,
                                    taxPercent = taxPercent,
                                    foreignCode = foreignCurrency.code,
                                    homeCode = homeCurrency.code
                                )
                                itemName = ""
                                priceInput = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.add_to_trip_list))
                    }
                }
            }
        }

        // Trip Expenses History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.trip_expenses),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.shoppingItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearShoppingItems() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.clear_trip_list))
                    }
                }
            }
        }

        // Grand Total Banner
        if (uiState.shoppingItems.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.grand_total),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "${uiState.shoppingItems.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${homeCurrency.symbol} ${viewModel.formatValue(totalTripHome)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // List of items
        items(uiState.shoppingItems, key = { it.id }) { item ->
            ShoppingItemCard(
                item = item,
                onDelete = { viewModel.removeShoppingItem(item.id) },
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItem,
    onDelete: () -> Unit,
    viewModel: MainViewModel
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${item.foreignCurrencyCode} ${viewModel.formatValue(item.finalForeignPrice)}" +
                            (if (item.discountPercent > 0) " (-${item.discountPercent.toInt()}%)" else "") +
                            (if (item.taxPercent > 0) " (+${item.taxPercent.toInt()}% tax)" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.homeCurrencyCode} ${viewModel.formatValue(item.homePrice)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
