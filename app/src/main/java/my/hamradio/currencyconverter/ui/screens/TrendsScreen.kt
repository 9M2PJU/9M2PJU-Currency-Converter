package my.hamradio.currencyconverter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.hamradio.currencyconverter.R
import my.hamradio.currencyconverter.data.model.TimePeriod
import my.hamradio.currencyconverter.ui.CurrencyPickerMode
import my.hamradio.currencyconverter.ui.MainUiState
import my.hamradio.currencyconverter.ui.MainViewModel
import my.hamradio.currencyconverter.ui.components.TrendChart

@Composable
fun TrendsScreen(
    viewModel: MainViewModel,
    uiState: MainUiState,
    modifier: Modifier = Modifier
) {
    val fromCurrency = uiState.baseCurrency
    val toCurrency = uiState.targetCurrency
    val points = uiState.trendPoints

    val high = points.maxOfOrNull { it.value } ?: 0.0
    val low = points.minOfOrNull { it.value } ?: 0.0
    val avg = if (points.isNotEmpty()) points.map { it.value }.average() else 0.0
    val change = if (points.size >= 2) {
        val first = points.first().value
        val last = points.last().value
        ((last - first) / first) * 100.0
    } else 0.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Currency Pair Header
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { viewModel.openCurrencyPicker(CurrencyPickerMode.BASE) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = fromCurrency.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = fromCurrency.code, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    IconButton(onClick = { viewModel.swapBaseAndTarget() }) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        onClick = { viewModel.openCurrencyPicker(CurrencyPickerMode.TARGET) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = toCurrency.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = toCurrency.code, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                }
            }
        }

        // Timeframe selector pills
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimePeriod.values().forEach { period ->
                    val isSelected = uiState.trendPeriod == period
                    val label = when (period) {
                        TimePeriod.PERIOD_7D -> stringResource(R.string.period_7d)
                        TimePeriod.PERIOD_30D -> stringResource(R.string.period_30d)
                        TimePeriod.PERIOD_90D -> stringResource(R.string.period_90d)
                        TimePeriod.PERIOD_1Y -> stringResource(R.string.period_1y)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setTrendPeriod(period) },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Trend Chart Card
        item {
            TrendChart(
                points = points,
                lineColor = if (change >= 0) MaterialTheme.colorScheme.primary else Color(0xFFF43F5E)
            )
        }

        // Summary Statistics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Key Rate Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.stat_high),
                        value = String.format(java.util.Locale.US, "%.4f", high),
                        suffix = toCurrency.code,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.stat_low),
                        value = String.format(java.util.Locale.US, "%.4f", low),
                        suffix = toCurrency.code,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = stringResource(R.string.stat_average),
                        value = String.format(java.util.Locale.US, "%.4f", avg),
                        suffix = toCurrency.code,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = stringResource(R.string.stat_change),
                        value = (if (change >= 0) "+" else "") + String.format(java.util.Locale.US, "%.2f%%", change),
                        valueColor = if (change >= 0) Color(0xFF10B981) else Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    suffix: String = "",
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                if (suffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = suffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
