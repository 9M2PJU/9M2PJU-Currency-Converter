package my.hamradio.currencyconverter.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hamradio.currencyconverter.R
import my.hamradio.currencyconverter.data.model.Currency
import my.hamradio.currencyconverter.ui.CurrencyPickerMode
import my.hamradio.currencyconverter.ui.MainUiState
import my.hamradio.currencyconverter.ui.MainViewModel
import my.hamradio.currencyconverter.ui.components.QuickAmountChips

@Composable
fun PairConverterScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val fromCurrency = uiState.baseCurrency
    val toCurrency = uiState.targetCurrency
    val amount = uiState.evaluatedAmount

    val standardRate = viewModel.getExchangeRate(fromCurrency.code, toCurrency.code)
    val inverseRate = if (standardRate > 0) 1.0 / standardRate else 0.0

    val markupFee = uiState.pairMarkupPercent
    val effectiveRate = standardRate * (1.0 + (markupFee / 100.0))
    val convertedTotal = amount * effectiveRate

    var rotationAngle by remember { mutableStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = rotationAngle, label = "swap_rotate")

    val denominationList = listOf(1.0, 5.0, 10.0, 20.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 5000.0, 10000.0)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Conversion Dual Card
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
                    // Source Currency Box
                    CurrencyInputBox(
                        title = "You Pay",
                        currency = fromCurrency,
                        amountText = viewModel.formatValue(amount),
                        onSelectCurrency = { viewModel.openCurrencyPicker(CurrencyPickerMode.BASE) }
                    )

                    // Middle Swap Button Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        IconButton(
                            onClick = {
                                rotationAngle += 180f
                                viewModel.swapBaseAndTarget()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = stringResource(R.string.swap_currencies),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.rotate(animatedRotation)
                            )
                        }
                    }

                    // Target Currency Box
                    CurrencyInputBox(
                        title = "You Get",
                        currency = toCurrency,
                        amountText = viewModel.formatValue(convertedTotal),
                        onSelectCurrency = { viewModel.openCurrencyPicker(CurrencyPickerMode.TARGET) },
                        isTarget = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Chips
                    QuickAmountChips(
                        onAmountSelected = { viewModel.setQuickAmount(it) },
                        currencySymbol = fromCurrency.symbol
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inverse Rate breakdown pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "1 ${fromCurrency.code} = ${String.format(java.util.Locale.US, "%.4f", standardRate)} ${toCurrency.code}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "1 ${toCurrency.code} = ${String.format(java.util.Locale.US, "%.4f", inverseRate)} ${fromCurrency.code}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bank / Card Fee Markup Calculator
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
                        Text(
                            text = stringResource(R.string.fee_calculator_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+${String.format(java.util.Locale.US, "%.1f", markupFee)}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (markupFee > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = markupFee.toFloat(),
                        onValueChange = { viewModel.setPairMarkupPercent(it.toDouble()) },
                        valueRange = 0f..10f,
                        steps = 19
                    )

                    // Preset Fee Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.0, 1.0, 1.5, 2.5, 3.5, 5.0).forEach { feePreset ->
                            FilterChip(
                                selected = markupFee == feePreset,
                                onClick = { viewModel.setPairMarkupPercent(feePreset) },
                                label = { Text("${feePreset}%", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    if (markupFee > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val extraCost = (amount * standardRate) * (markupFee / 100.0)
                        Text(
                            text = "Additional Fee Cost: ${toCurrency.symbol} ${viewModel.formatValue(extraCost)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Denomination Matrix
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
                    Text(
                        text = stringResource(R.string.denomination_matrix),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    denominationList.forEachIndexed { index, denom ->
                        val res = denom * effectiveRate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${fromCurrency.symbol} ${viewModel.formatValue(denom)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${toCurrency.symbol} ${viewModel.formatValue(res)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyInputBox(
    title: String,
    currency: Currency,
    amountText: String,
    onSelectCurrency: () -> Unit,
    isTarget: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onSelectCurrency,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = currency.flag, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = currency.code,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
