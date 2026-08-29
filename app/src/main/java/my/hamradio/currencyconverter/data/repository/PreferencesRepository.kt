package my.hamradio.currencyconverter.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import my.hamradio.currencyconverter.data.model.AppThemeSetting
import my.hamradio.currencyconverter.data.model.ShoppingItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_PRECISION = "decimal_precision"
        private const val KEY_BASE_CURRENCY = "base_currency"
        private const val KEY_TARGET_CURRENCY = "target_currency"
        private const val KEY_FAVORITES = "favorite_currencies"
        private const val KEY_CUSTOM_RATES = "custom_rates"
        private const val KEY_SHOPPING_ITEMS = "shopping_items"
        private const val KEY_LAST_UPDATED = "last_updated_timestamp"
        private const val KEY_ONLINE_RATES = "online_rates"
        private const val KEY_AUTO_UPDATE = "auto_update_rates"
    }

    var isAutoUpdateEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

    var appTheme: AppThemeSetting
        get() {
            val name = prefs.getString(KEY_THEME, AppThemeSetting.SYSTEM.name) ?: AppThemeSetting.SYSTEM.name
            return try {
                AppThemeSetting.valueOf(name)
            } catch (e: Exception) {
                AppThemeSetting.SYSTEM
            }
        }
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var decimalPrecision: Int
        get() = prefs.getInt(KEY_PRECISION, 2)
        set(value) = prefs.edit().putInt(KEY_PRECISION, value).apply()

    var baseCurrencyCode: String
        get() = prefs.getString(KEY_BASE_CURRENCY, "USD") ?: "USD"
        set(value) = prefs.edit().putString(KEY_BASE_CURRENCY, value).apply()

    var targetCurrencyCode: String
        get() = prefs.getString(KEY_TARGET_CURRENCY, "MYR") ?: "MYR"
        set(value) = prefs.edit().putString(KEY_TARGET_CURRENCY, value).apply()

    var lastUpdatedText: String
        get() {
            val defaultTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
            return prefs.getString(KEY_LAST_UPDATED, defaultTime) ?: defaultTime
        }
        set(value) = prefs.edit().putString(KEY_LAST_UPDATED, value).apply()

    fun getFavorites(): Set<String> {
        val defaultSet = setOf("USD", "EUR", "MYR", "SGD", "GBP", "JPY", "AUD", "CAD", "THB", "IDR", "CNY", "BTC", "XAU")
        return prefs.getStringSet(KEY_FAVORITES, defaultSet) ?: defaultSet
    }

    fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }

    fun getCustomRates(): Map<String, Double> {
        val json = prefs.getString(KEY_CUSTOM_RATES, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveCustomRate(code: String, rate: Double) {
        val current = getCustomRates().toMutableMap()
        current[code] = rate
        prefs.edit().putString(KEY_CUSTOM_RATES, gson.toJson(current)).apply()
    }

    fun removeCustomRate(code: String) {
        val current = getCustomRates().toMutableMap()
        current.remove(code)
        prefs.edit().putString(KEY_CUSTOM_RATES, gson.toJson(current)).apply()
    }

    fun clearAllCustomRates() {
        prefs.edit().remove(KEY_CUSTOM_RATES).apply()
    }

    fun getOnlineRates(): Map<String, Double> {
        val json = prefs.getString(KEY_ONLINE_RATES, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveOnlineRates(rates: Map<String, Double>, timestamp: String) {
        prefs.edit()
            .putString(KEY_ONLINE_RATES, gson.toJson(rates))
            .putString(KEY_LAST_UPDATED, timestamp)
            .apply()
    }

    fun getShoppingItems(): List<ShoppingItem> {
        val json = prefs.getString(KEY_SHOPPING_ITEMS, null) ?: return emptyList()
        val type = object : TypeToken<List<ShoppingItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveShoppingItems(items: List<ShoppingItem>) {
        prefs.edit().putString(KEY_SHOPPING_ITEMS, gson.toJson(items)).apply()
    }
}
