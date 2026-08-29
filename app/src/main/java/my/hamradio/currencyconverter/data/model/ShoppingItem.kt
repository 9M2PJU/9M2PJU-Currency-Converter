package my.hamradio.currencyconverter.data.model

import java.util.UUID

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val foreignPrice: Double,
    val foreignCurrencyCode: String,
    val discountPercent: Double = 0.0,
    val taxPercent: Double = 0.0,
    val finalForeignPrice: Double,
    val homeCurrencyCode: String,
    val homePrice: Double,
    val category: ExpenseCategory = ExpenseCategory.GENERAL,
    val timestamp: Long = System.currentTimeMillis()
)
