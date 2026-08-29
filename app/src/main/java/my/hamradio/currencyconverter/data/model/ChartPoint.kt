package my.hamradio.currencyconverter.data.model

data class ChartPoint(
    val label: String,
    val value: Double,
    val timestamp: Long
)

enum class TimePeriod(val labelResName: String, val days: Int) {
    PERIOD_7D("7D", 7),
    PERIOD_30D("30D", 30),
    PERIOD_90D("90D", 90),
    PERIOD_1Y("1Y", 365)
}

enum class AppThemeSetting {
    SYSTEM,
    DYNAMIC,
    LIGHT,
    DARK,
    OLED
}

enum class ExpenseCategory(val iconName: String, val defaultTitle: String) {
    GENERAL("Tag", "General"),
    FOOD("Restaurant", "Food & Dining"),
    TRANSPORT("DirectionsCar", "Transport"),
    LODGING("Hotel", "Stay & Hotel"),
    SHOPPING("ShoppingBag", "Shopping"),
    ENTERTAINMENT("ConfirmationNumber", "Entertainment")
}
