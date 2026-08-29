package my.hamradio.currencyconverter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickAmountChips(
    onAmountSelected: (Double) -> Unit,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val presetAmounts = listOf(1.0, 5.0, 10.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 5000.0)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(presetAmounts) { amount ->
            val formatted = if (amount >= 1000) "${(amount / 1000).toInt()}k" else amount.toInt().toString()
            Surface(
                onClick = { onAmountSelected(amount) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                tonalElevation = 1.dp
            ) {
                Text(
                    text = "$currencySymbol$formatted",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
