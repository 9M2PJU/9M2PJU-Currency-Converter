package my.hamradio.currencyconverter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import my.hamradio.currencyconverter.data.model.AppThemeSetting
import my.hamradio.currencyconverter.data.model.ChartPoint
import my.hamradio.currencyconverter.data.model.Currency
import my.hamradio.currencyconverter.data.model.ExpenseCategory
import my.hamradio.currencyconverter.data.model.ShoppingItem
import my.hamradio.currencyconverter.data.model.TimePeriod
import my.hamradio.currencyconverter.data.repository.CurrencyRepository
import my.hamradio.currencyconverter.data.repository.PreferencesRepository

data class MainUiState(
    val currencies: List<Currency> = emptyList(),
    val baseCurrency: Currency = Currency("USD", "US Dollar", "$", "🇺🇸", "Americas", 1.0),
    val targetCurrency: Currency = Currency("MYR", "Malaysian Ringgit", "RM", "🇲🇾", "Southeast Asia", 4.425),
    val inputExpression: String = "1",
    val evaluatedAmount: Double = 1.0,
    val isNumpadVisible: Boolean = true,
    val appTheme: AppThemeSetting = AppThemeSetting.SYSTEM,
    val decimalPrecision: Int = 2,
    val isAutoUpdateEnabled: Boolean = true,
    val tripBudgetLimit: Double = 0.0,
    val lastUpdatedText: String = "",
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val shoppingItems: List<ShoppingItem> = emptyList(),
    val pairMarkupPercent: Double = 0.0,
    val trendPeriod: TimePeriod = TimePeriod.PERIOD_30D,
    val trendPoints: List<ChartPoint> = emptyList(),
    val currencyToEditRate: Currency? = null,
    val currencyPickerMode: CurrencyPickerMode? = null
)

enum class CurrencyPickerMode {
    BASE,
    TARGET,
    PAIR_FROM,
    PAIR_TO,
    SHOPPING_FOREIGN,
    SHOPPING_HOME
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application)
    private val currencyRepository = CurrencyRepository(application, preferencesRepository)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        val allCurrencies = currencyRepository.getAllCurrencies()
        val baseCode = preferencesRepository.baseCurrencyCode
        val targetCode = preferencesRepository.targetCurrencyCode
        val base = currencyRepository.getCurrency(baseCode) ?: allCurrencies.firstOrNull { it.code == "USD" } ?: allCurrencies.first()
        val target = currencyRepository.getCurrency(targetCode) ?: allCurrencies.firstOrNull { it.code == "MYR" } ?: allCurrencies.first()

        val theme = preferencesRepository.appTheme
        val precision = preferencesRepository.decimalPrecision
        val lastUpdated = preferencesRepository.lastUpdatedText
        val shopping = preferencesRepository.getShoppingItems()
        val autoUpdate = preferencesRepository.isAutoUpdateEnabled
        val budget = preferencesRepository.tripBudgetLimit

        _uiState.update { current ->
            current.copy(
                currencies = allCurrencies,
                baseCurrency = base,
                targetCurrency = target,
                appTheme = theme,
                decimalPrecision = precision,
                lastUpdatedText = lastUpdated,
                shoppingItems = shopping,
                isAutoUpdateEnabled = autoUpdate,
                tripBudgetLimit = budget
            )
        }
        updateTrends()

        if (autoUpdate) {
            syncLiveRates(isSilent = true)
        }
    }

    fun setInputExpression(expr: String) {
        val evaluated = evaluateMathExpression(expr)
        _uiState.update { it.copy(inputExpression = expr, evaluatedAmount = evaluated) }
    }

    fun onDigitPressed(digit: String) {
        val current = _uiState.value.inputExpression
        val newExpr = if (current == "0" && digit != ".") digit else current + digit
        setInputExpression(newExpr)
    }

    fun onOperatorPressed(op: String) {
        val current = _uiState.value.inputExpression
        if (op == "+/-") {
            val evaluated = _uiState.value.evaluatedAmount
            val toggled = if (evaluated > 0) "-$current" else current.removePrefix("-")
            setInputExpression(toggled)
            return
        }
        if (current.isEmpty()) return
        val lastChar = current.last()
        val newExpr = if (lastChar in listOf('+', '-', '*', '/')) {
            current.dropLast(1) + op
        } else {
            current + op
        }
        _uiState.update { it.copy(inputExpression = newExpr) }
    }

    fun onClearPressed() {
        setInputExpression("0")
    }

    fun onBackspacePressed() {
        val current = _uiState.value.inputExpression
        val newExpr = if (current.length <= 1) "0" else current.dropLast(1)
        setInputExpression(newExpr)
    }

    fun onEqualsPressed() {
        val evaluated = _uiState.value.evaluatedAmount
        val formatted = if (evaluated % 1.0 == 0.0) evaluated.toLong().toString() else evaluated.toString()
        setInputExpression(formatted)
    }

    fun setQuickAmount(amount: Double) {
        val formatted = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
        setInputExpression(formatted)
    }

    fun toggleNumpadVisibility() {
        _uiState.update { it.copy(isNumpadVisible = !it.isNumpadVisible) }
    }

    fun setBaseCurrency(currency: Currency) {
        preferencesRepository.baseCurrencyCode = currency.code
        _uiState.update { it.copy(baseCurrency = currency) }
        updateTrends()
    }

    fun setTargetCurrency(currency: Currency) {
        preferencesRepository.targetCurrencyCode = currency.code
        _uiState.update { it.copy(targetCurrency = currency) }
        updateTrends()
    }

    fun swapBaseAndTarget() {
        val currentBase = _uiState.value.baseCurrency
        val currentTarget = _uiState.value.targetCurrency
        setBaseCurrency(currentTarget)
        setTargetCurrency(currentBase)
    }

    fun toggleFavorite(code: String) {
        currencyRepository.toggleFavorite(code)
        val all = currencyRepository.getAllCurrencies()
        _uiState.update { it.copy(currencies = all) }
    }

    fun setCustomRate(code: String, rate: Double) {
        currencyRepository.setCustomRate(code, rate)
        val all = currencyRepository.getAllCurrencies()
        val base = currencyRepository.getCurrency(_uiState.value.baseCurrency.code) ?: _uiState.value.baseCurrency
        val target = currencyRepository.getCurrency(_uiState.value.targetCurrency.code) ?: _uiState.value.targetCurrency
        _uiState.update { it.copy(currencies = all, baseCurrency = base, targetCurrency = target) }
        updateTrends()
    }

    fun resetCustomRate(code: String) {
        currencyRepository.resetCustomRate(code)
        val all = currencyRepository.getAllCurrencies()
        val base = currencyRepository.getCurrency(_uiState.value.baseCurrency.code) ?: _uiState.value.baseCurrency
        val target = currencyRepository.getCurrency(_uiState.value.targetCurrency.code) ?: _uiState.value.targetCurrency
        _uiState.update { it.copy(currencies = all, baseCurrency = base, targetCurrency = target) }
        updateTrends()
    }

    fun resetAllRates() {
        currencyRepository.resetAllRates()
        val all = currencyRepository.getAllCurrencies()
        val base = currencyRepository.getCurrency(_uiState.value.baseCurrency.code) ?: _uiState.value.baseCurrency
        val target = currencyRepository.getCurrency(_uiState.value.targetCurrency.code) ?: _uiState.value.targetCurrency
        _uiState.update { it.copy(currencies = all, baseCurrency = base, targetCurrency = target) }
        updateTrends()
    }

    fun setPairMarkupPercent(percent: Double) {
        _uiState.update { it.copy(pairMarkupPercent = percent) }
    }

    fun setTrendPeriod(period: TimePeriod) {
        _uiState.update { it.copy(trendPeriod = period) }
        updateTrends()
    }

    private fun updateTrends() {
        val state = _uiState.value
        val points = currencyRepository.getTrendData(state.baseCurrency.code, state.targetCurrency.code, state.trendPeriod)
        _uiState.update { it.copy(trendPoints = points) }
    }

    fun setAutoUpdate(enabled: Boolean) {
        preferencesRepository.isAutoUpdateEnabled = enabled
        _uiState.update { it.copy(isAutoUpdateEnabled = enabled) }
    }

    fun syncLiveRates(isSilent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncMessage = null) }
            val result = currencyRepository.syncLiveRates()
            result.onSuccess { count ->
                val all = currencyRepository.getAllCurrencies()
                val lastUpdated = preferencesRepository.lastUpdatedText
                _uiState.update {
                    it.copy(
                        currencies = all,
                        lastUpdatedText = lastUpdated,
                        isSyncing = false,
                        syncMessage = if (!isSilent) "Updated $count rates successfully!" else null
                    )
                }
                updateTrends()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncMessage = if (!isSilent) "Offline mode. Cached rates are active." else null
                    )
                }
            }
        }
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncMessage = null) }
    }

    fun setAppTheme(theme: AppThemeSetting) {
        preferencesRepository.appTheme = theme
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun setDecimalPrecision(precision: Int) {
        preferencesRepository.decimalPrecision = precision
        _uiState.update { it.copy(decimalPrecision = precision) }
    }

    fun setTripBudgetLimit(limit: Double) {
        preferencesRepository.tripBudgetLimit = limit
        _uiState.update { it.copy(tripBudgetLimit = limit) }
    }

    fun addShoppingItem(
        name: String,
        foreignPrice: Double,
        discountPercent: Double,
        taxPercent: Double,
        foreignCode: String,
        homeCode: String,
        category: ExpenseCategory = ExpenseCategory.GENERAL
    ) {
        val discounted = foreignPrice * (1.0 - (discountPercent / 100.0))
        val finalForeign = discounted * (1.0 + (taxPercent / 100.0))
        val homeVal = currencyRepository.convert(finalForeign, foreignCode, homeCode)
        val item = ShoppingItem(
            name = if (name.isBlank()) "${category.defaultTitle} #${_uiState.value.shoppingItems.size + 1}" else name,
            foreignPrice = foreignPrice,
            foreignCurrencyCode = foreignCode,
            discountPercent = discountPercent,
            taxPercent = taxPercent,
            finalForeignPrice = finalForeign,
            homeCurrencyCode = homeCode,
            homePrice = homeVal,
            category = category
        )
        val updated = listOf(item) + _uiState.value.shoppingItems
        preferencesRepository.saveShoppingItems(updated)
        _uiState.update { it.copy(shoppingItems = updated) }
    }

    fun removeShoppingItem(id: String) {
        val updated = _uiState.value.shoppingItems.filter { it.id != id }
        preferencesRepository.saveShoppingItems(updated)
        _uiState.update { it.copy(shoppingItems = updated) }
    }

    fun clearShoppingItems() {
        preferencesRepository.saveShoppingItems(emptyList())
        _uiState.update { it.copy(shoppingItems = emptyList()) }
    }

    fun exportBackup(): String {
        return preferencesRepository.exportBackupJson()
    }

    fun importBackup(jsonString: String): Boolean {
        val success = preferencesRepository.importBackupJson(jsonString)
        if (success) {
            loadInitialData()
        }
        return success
    }

    fun openCurrencyPicker(mode: CurrencyPickerMode) {
        _uiState.update { it.copy(currencyPickerMode = mode) }
    }

    fun closeCurrencyPicker() {
        _uiState.update { it.copy(currencyPickerMode = null) }
    }

    fun openRateEditor(currency: Currency) {
        _uiState.update { it.copy(currencyToEditRate = currency) }
    }

    fun closeRateEditor() {
        _uiState.update { it.copy(currencyToEditRate = null) }
    }

    fun convertAmount(amount: Double, fromCode: String, toCode: String): Double {
        return currencyRepository.convert(amount, fromCode, toCode)
    }

    fun getExchangeRate(fromCode: String, toCode: String): Double {
        return currencyRepository.getExchangeRate(fromCode, toCode)
    }

    fun formatValue(value: Double): String {
        val precision = _uiState.value.decimalPrecision
        return when {
            value >= 1_000_000 -> String.format(java.util.Locale.US, "%,.${precision}f", value)
            value < 0.0001 && value > 0 -> String.format(java.util.Locale.US, "%.8f", value)
            else -> String.format(java.util.Locale.US, "%,.${precision}f", value)
        }
    }

    private fun evaluateMathExpression(expr: String): Double {
        if (expr.isBlank()) return 0.0
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/")
            evaluateSimpleMath(sanitized)
        } catch (e: Exception) {
            0.0
        }
    }

    private fun evaluateSimpleMath(expr: String): Double {
        // Simple token evaluation supporting +, -, *, /
        var clean = expr.trim()
        if (clean.isEmpty()) return 0.0
        while (clean.endsWith("+") || clean.endsWith("-") || clean.endsWith("*") || clean.endsWith("/")) {
            clean = clean.dropLast(1).trim()
        }
        if (clean.isEmpty()) return 0.0

        val tokens = mutableListOf<String>()
        var buffer = StringBuilder()
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            if (c in listOf('+', '-', '*', '/')) {
                if (buffer.isNotEmpty()) {
                    tokens.add(buffer.toString())
                    buffer = StringBuilder()
                } else if (c == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    buffer.append(c)
                    i++
                    continue
                }
                tokens.add(c.toString())
            } else if (c.isDigit() || c == '.') {
                buffer.append(c)
            }
            i++
        }
        if (buffer.isNotEmpty()) {
            tokens.add(buffer.toString())
        }

        if (tokens.isEmpty()) return 0.0

        // Step 1: Handle * and /
        val pass1 = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "*" || token == "/") {
                val prev = pass1.removeAt(pass1.size - 1).toDoubleOrNull() ?: 0.0
                val next = tokens.getOrNull(idx + 1)?.toDoubleOrNull() ?: 1.0
                val res = if (token == "*") prev * next else if (next != 0.0) prev / next else 0.0
                pass1.add(res.toString())
                idx += 2
            } else {
                pass1.add(token)
                idx++
            }
        }

        // Step 2: Handle + and -
        var result = pass1.firstOrNull()?.toDoubleOrNull() ?: 0.0
        var pIdx = 1
        while (pIdx < pass1.size) {
            val op = pass1[pIdx]
            val nextVal = pass1.getOrNull(pIdx + 1)?.toDoubleOrNull() ?: 0.0
            if (op == "+") result += nextVal
            else if (op == "-") result -= nextVal
            pIdx += 2
        }
        return result
    }
}
