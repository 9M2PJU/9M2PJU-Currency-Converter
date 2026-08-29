package my.hamradio.currencyconverter.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hamradio.currencyconverter.R
import my.hamradio.currencyconverter.data.model.ExpenseCategory
import my.hamradio.currencyconverter.data.model.ShoppingItem
import my.hamradio.currencyconverter.ui.CurrencyPickerMode
import my.hamradio.currencyconverter.ui.MainUiState
import my.hamradio.currencyconverter.ui.MainViewModel
import kotlin.math.ceil

enum class TravelTab {
    BUDGET_CART,
    TIP_SPLIT
}

@Composable
fun TravelCalculatorScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(TravelTab.BUDGET_CART) }

    // Shopping / Budget inputs
    var itemName by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    var taxPercent by remember { mutableDoubleStateOf(0.0) }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.GENERAL) }
    var showBudgetDialog by remember { mutableStateOf(false) }

    // Tip & Split inputs
    var billInput by remember { mutableStateOf("") }
    var tipPercent by remember { mutableDoubleStateOf(10.0) }
    var splitCount by remember { mutableIntStateOf(2) }
    var roundUp by remember { mutableStateOf(false) }

    val foreignCurrency = uiState.baseCurrency
    val homeCurrency = uiState.targetCurrency

    val rawPrice = priceInput.toDoubleOrNull() ?: 0.0
    val discountedPrice = rawPrice * (1.0 - (discountPercent / 100.0))
    val finalForeignPrice = discountedPrice * (1.0 + (taxPercent / 100.0))
    val finalHomePrice = viewModel.convertAmount(finalForeignPrice, foreignCurrency.code, homeCurrency.code)

    val totalTripHome = uiState.shoppingItems.sumOf { it.homePrice }
    val budgetLimit = uiState.tripBudgetLimit
    val budgetRatio = if (budgetLimit > 0) (totalTripHome / budgetLimit).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOverBudget = budgetLimit > 0 && totalTripHome > budgetLimit

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Selector Row
        TabRow(
            selectedTabIndex = currentTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = currentTab == TravelTab.BUDGET_CART,
                onClick = { currentTab = TravelTab.BUDGET_CART },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.tab_shopping_budget), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = currentTab == TravelTab.TIP_SPLIT,
                onClick = { currentTab = TravelTab.TIP_SPLIT },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.tab_tip_split), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        when (currentTab) {
            TravelTab.BUDGET_CART -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Budget Progress Target Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.trip_budget_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TextButton(onClick = { showBudgetDialog = true }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (budgetLimit > 0) "${homeCurrency.symbol} ${viewModel.formatValue(budgetLimit)}"
                                            else stringResource(R.string.set_budget_limit),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                if (budgetLimit > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { budgetRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = String.format(stringResource(R.string.budget_spent), "${homeCurrency.symbol} ${viewModel.formatValue(totalTripHome)}"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (isOverBudget) String.format(stringResource(R.string.budget_over), "${homeCurrency.symbol} ${viewModel.formatValue(totalTripHome - budgetLimit)}")
                                            else String.format(stringResource(R.string.budget_remaining), "${homeCurrency.symbol} ${viewModel.formatValue(budgetLimit - totalTripHome)}"),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Expense Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Currency Selectors
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

                                Spacer(modifier = Modifier.height(10.dp))

                                // Category Selector Chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(ExpenseCategory.entries.toTypedArray()) { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { selectedCategory = cat },
                                            label = { Text(getCategoryLabel(cat), fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

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

                                Spacer(modifier = Modifier.height(10.dp))

                                // Discount & Tax Sliders
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.discount_percent)}: ${discountPercent.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${stringResource(R.string.tax_percent)}: ${taxPercent.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Slider(
                                        value = discountPercent.toFloat(),
                                        onValueChange = { discountPercent = it.toDouble() },
                                        valueRange = 0f..70f,
                                        steps = 13,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Slider(
                                        value = taxPercent.toFloat(),
                                        onValueChange = { taxPercent = it.toDouble() },
                                        valueRange = 0f..30f,
                                        steps = 5,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Live Computed Preview Box
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
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
                                                text = "Foreign: ${foreignCurrency.symbol} ${viewModel.formatValue(finalForeignPrice)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(text = "Home Value:", style = MaterialTheme.typography.labelSmall)
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

                                Button(
                                    onClick = {
                                        if (rawPrice > 0) {
                                            viewModel.addShoppingItem(
                                                name = itemName,
                                                foreignPrice = rawPrice,
                                                discountPercent = discountPercent,
                                                taxPercent = taxPercent,
                                                foreignCode = foreignCurrency.code,
                                                homeCode = homeCurrency.code,
                                                category = selectedCategory
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

                    // Trip Expenses History Header & Action Buttons
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
                                Row {
                                    IconButton(
                                        onClick = {
                                            shareTripCsv(context, uiState.shoppingItems, homeCurrency.code, totalTripHome)
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.clearShoppingItems() }) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error)
                                    }
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
                                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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

            TravelTab.TIP_SPLIT -> {
                // Tip & Split Bill Calculator View
                val rawBill = billInput.toDoubleOrNull() ?: 0.0
                val tipAmountForeign = rawBill * (tipPercent / 100.0)
                val totalWithTipForeign = rawBill + tipAmountForeign
                val totalWithTipHome = viewModel.convertAmount(totalWithTipForeign, foreignCurrency.code, homeCurrency.code)

                val perPersonRawForeign = if (splitCount > 0) totalWithTipForeign / splitCount else 0.0
                val perPersonForeign = if (roundUp) ceil(perPersonRawForeign) else perPersonRawForeign
                val perPersonHome = viewModel.convertAmount(perPersonForeign, foreignCurrency.code, homeCurrency.code)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
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
                                    text = stringResource(R.string.tab_tip_split),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = billInput,
                                    onValueChange = { billInput = it },
                                    placeholder = { Text(stringResource(R.string.bill_amount)) },
                                    prefix = { Text("${foreignCurrency.symbol} ", fontWeight = FontWeight.SemiBold) },
                                    suffix = { Text(foreignCurrency.code, fontWeight = FontWeight.Medium) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Tip % Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.tip_percent),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${tipPercent.toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0.0, 5.0, 10.0, 15.0, 18.0, 20.0).forEach { preset ->
                                        FilterChip(
                                            selected = tipPercent == preset,
                                            onClick = { tipPercent = preset },
                                            label = { Text("${preset.toInt()}%", fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                Slider(
                                    value = tipPercent.toFloat(),
                                    onValueChange = { tipPercent = it.toDouble() },
                                    valueRange = 0f..30f,
                                    steps = 29
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Split between People Stepper
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.split_between),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (splitCount > 1) splitCount-- },
                                            enabled = splitCount > 1
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                        }

                                        Text(
                                            text = String.format(stringResource(R.string.people_count), splitCount),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { if (splitCount < 30) splitCount++ }
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Round Up Option
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.round_up_person),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Switch(
                                        checked = roundUp,
                                        onCheckedChange = { roundUp = it }
                                    )
                                }
                            }
                        }
                    }

                    // Calculation Breakdown Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.tip_amount), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${foreignCurrency.symbol} ${viewModel.formatValue(tipAmountForeign)}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = stringResource(R.string.total_with_tip), style = MaterialTheme.typography.bodyMedium)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${foreignCurrency.symbol} ${viewModel.formatValue(totalWithTipForeign)}",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "≈ ${homeCurrency.symbol} ${viewModel.formatValue(totalWithTipHome)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.per_person),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "(${splitCount}x)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${foreignCurrency.symbol} ${viewModel.formatValue(perPersonForeign)}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "≈ ${homeCurrency.symbol} ${viewModel.formatValue(perPersonHome)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Budget Limit Editor Dialog
        if (showBudgetDialog) {
            var budgetInput by remember { mutableStateOf(if (budgetLimit > 0) budgetLimit.toInt().toString() else "") }
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
                title = { Text(stringResource(R.string.set_budget_limit)) },
                text = {
                    Column {
                        Text(
                            text = "Enter overall trip budget limit in home currency (${homeCurrency.code})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = budgetInput,
                            onValueChange = { budgetInput = it },
                            prefix = { Text("${homeCurrency.symbol} ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val limit = budgetInput.toDoubleOrNull() ?: 0.0
                            viewModel.setTripBudgetLimit(limit)
                            showBudgetDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBudgetDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
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
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.category) {
                        ExpenseCategory.FOOD -> Icons.Default.Restaurant
                        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
                        ExpenseCategory.LODGING -> Icons.Default.Hotel
                        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
                        ExpenseCategory.ENTERTAINMENT -> Icons.Default.ConfirmationNumber
                        else -> Icons.Default.Tag
                    },
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

@Composable
private fun getCategoryLabel(category: ExpenseCategory): String {
    return when (category) {
        ExpenseCategory.GENERAL -> stringResource(R.string.category_general)
        ExpenseCategory.FOOD -> stringResource(R.string.category_food)
        ExpenseCategory.TRANSPORT -> stringResource(R.string.category_transport)
        ExpenseCategory.LODGING -> stringResource(R.string.category_lodging)
        ExpenseCategory.SHOPPING -> stringResource(R.string.category_shopping)
        ExpenseCategory.ENTERTAINMENT -> stringResource(R.string.category_entertainment)
    }
}

private fun shareTripCsv(context: android.content.Context, items: List<ShoppingItem>, homeCode: String, totalHome: Double) {
    val sb = StringBuilder()
    sb.append("9M2PJU Currency App - Trip Expense Report\n")
    sb.append("Item,Category,Foreign Price,Currency,Discount%,Tax%,Home Value ($homeCode)\n")
    items.forEach { item ->
        sb.append("\"${item.name}\",\"${item.category.name}\",${item.finalForeignPrice},${item.foreignCurrencyCode},${item.discountPercent},${item.taxPercent},${item.homePrice}\n")
    }
    sb.append("\nTotal Expenses ($homeCode): $totalHome\n")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "9M2PJU Currency App - Trip Expenses")
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Trip Expenses"))
}
