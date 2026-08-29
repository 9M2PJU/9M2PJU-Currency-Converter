package my.hamradio.currencyconverter.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorKeypad(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    fun haptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    val keys = listOf(
        listOf(KeyItem.Text("C", isAction = true), KeyItem.Text("÷", isOperator = true), KeyItem.Text("×", isOperator = true), KeyItem.Icon(Icons.AutoMirrored.Filled.Backspace, isAction = true)),
        listOf(KeyItem.Text("7"), KeyItem.Text("8"), KeyItem.Text("9"), KeyItem.Text("-", isOperator = true)),
        listOf(KeyItem.Text("4"), KeyItem.Text("5"), KeyItem.Text("6"), KeyItem.Text("+", isOperator = true)),
        listOf(KeyItem.Text("1"), KeyItem.Text("2"), KeyItem.Text("3"), KeyItem.Text("=", isPrimary = true)),
        listOf(KeyItem.Text("00"), KeyItem.Text("0"), KeyItem.Text("."), KeyItem.Text("+/-", isAction = true))
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { item ->
                        KeyButton(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptic()
                                when (item) {
                                    is KeyItem.Text -> {
                                        when (item.text) {
                                            "C" -> onClear()
                                            "÷" -> onOperator("/")
                                            "×" -> onOperator("*")
                                            "-" -> onOperator("-")
                                            "+" -> onOperator("+")
                                            "=" -> onEquals()
                                            "+/-" -> onOperator("+/-")
                                            else -> onDigit(item.text)
                                        }
                                    }
                                    is KeyItem.Icon -> onBackspace()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed class KeyItem {
    data class Text(
        val text: String,
        val isOperator: Boolean = false,
        val isAction: Boolean = false,
        val isPrimary: Boolean = false
    ) : KeyItem()

    data class Icon(
        val icon: ImageVector,
        val isAction: Boolean = true
    ) : KeyItem()
}

@Composable
private fun KeyButton(
    item: KeyItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor) = when (item) {
        is KeyItem.Text -> when {
            item.isPrimary -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            item.isOperator -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            item.isAction -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.error)
            else -> Pair(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.onSurface)
        }
        is KeyItem.Icon -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
    }

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (item) {
            is KeyItem.Text -> {
                Text(
                    text = item.text,
                    fontSize = if (item.text.length > 2) 16.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
            is KeyItem.Icon -> {
                Icon(
                    imageVector = item.icon,
                    contentDescription = "Backspace",
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
