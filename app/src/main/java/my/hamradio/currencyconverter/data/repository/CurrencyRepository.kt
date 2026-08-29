package my.hamradio.currencyconverter.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.hamradio.currencyconverter.data.model.ChartPoint
import my.hamradio.currencyconverter.data.model.Currency
import my.hamradio.currencyconverter.data.model.CurrencyDataSet
import my.hamradio.currencyconverter.data.model.TimePeriod
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

class CurrencyRepository(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    private val gson = Gson()
    private val cachedCurrencies = mutableListOf<Currency>()
    private var baseCurrencyCode: String = "USD"

    init {
        loadCurrenciesFromAssets()
    }

    private fun loadCurrenciesFromAssets() {
        try {
            val inputStream = context.assets.open("currencies_data.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val dataSet = gson.fromJson(reader, CurrencyDataSet::class.java)
            reader.close()

            val favorites = preferencesRepository.getFavorites()
            val customRates = preferencesRepository.getCustomRates()
            val onlineRates = preferencesRepository.getOnlineRates()

            cachedCurrencies.clear()
            dataSet.currencies.forEach { item ->
                val currency = item.copy()
                currency.defaultRate = item.rate

                // Apply online rate if exists
                if (onlineRates.containsKey(item.code)) {
                    val rate = onlineRates[item.code] ?: item.rate
                    if (rate > 0) {
                        currency.rate = rate
                    }
                }

                // Apply user custom rate if exists
                if (customRates.containsKey(item.code)) {
                    val custom = customRates[item.code] ?: item.rate
                    currency.rate = custom
                    currency.isCustomRate = true
                }

                currency.isFavorite = favorites.contains(item.code)
                cachedCurrencies.add(currency)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAllCurrencies(): List<Currency> = cachedCurrencies.toList()

    fun getCurrency(code: String): Currency? = cachedCurrencies.find { it.code.equals(code, ignoreCase = true) }

    fun convert(amount: Double, fromCode: String, toCode: String): Double {
        if (fromCode.equals(toCode, ignoreCase = true)) return amount
        val from = getCurrency(fromCode) ?: return amount
        val to = getCurrency(toCode) ?: return amount
        if (from.rate <= 0 || to.rate <= 0) return 0.0
        // Rates are stored relative to USD (1 USD = X Currency)
        // Rate from USD to Target = to.rate
        // 1 FromCurrency = (1 / from.rate) USD = (to.rate / from.rate) ToCurrency
        val usdAmount = amount / from.rate
        return usdAmount * to.rate
    }

    fun getExchangeRate(fromCode: String, toCode: String): Double {
        return convert(1.0, fromCode, toCode)
    }

    fun toggleFavorite(code: String): Boolean {
        val curr = getCurrency(code) ?: return false
        curr.isFavorite = !curr.isFavorite
        val favs = cachedCurrencies.filter { it.isFavorite }.map { it.code }.toSet()
        preferencesRepository.saveFavorites(favs)
        return curr.isFavorite
    }

    fun setCustomRate(code: String, rate: Double) {
        val curr = getCurrency(code) ?: return
        curr.rate = rate
        curr.isCustomRate = true
        preferencesRepository.saveCustomRate(code, rate)
    }

    fun resetCustomRate(code: String) {
        val curr = getCurrency(code) ?: return
        curr.rate = curr.defaultRate
        val onlineRates = preferencesRepository.getOnlineRates()
        if (onlineRates.containsKey(code)) {
            curr.rate = onlineRates[code] ?: curr.defaultRate
        }
        curr.isCustomRate = false
        preferencesRepository.removeCustomRate(code)
    }

    fun resetAllRates() {
        preferencesRepository.clearAllCustomRates()
        val onlineRates = preferencesRepository.getOnlineRates()
        cachedCurrencies.forEach { curr ->
            curr.isCustomRate = false
            curr.rate = onlineRates[curr.code] ?: curr.defaultRate
        }
    }

    fun getTrendData(fromCode: String, toCode: String, period: TimePeriod): List<ChartPoint> {
        val currentRate = getExchangeRate(fromCode, toCode)
        val points = mutableListOf<ChartPoint>()
        val count = when (period) {
            TimePeriod.PERIOD_7D -> 7
            TimePeriod.PERIOD_30D -> 30
            TimePeriod.PERIOD_90D -> 45
            TimePeriod.PERIOD_1Y -> 52
        }

        // Seeded pseudorandom walk to provide smooth, realistic offline forex trajectory
        val seed = (fromCode.hashCode() * 31 + toCode.hashCode() * 17).toLong()
        val random = Random(seed)
        val volatility = when {
            fromCode in listOf("BTC", "ETH", "SOL", "DOGE") || toCode in listOf("BTC", "ETH", "SOL", "DOGE") -> 0.035
            fromCode in listOf("TRY", "ARS", "EGP", "NGN") || toCode in listOf("TRY", "ARS", "EGP", "NGN") -> 0.015
            else -> 0.005
        }

        val cal = Calendar.getInstance()
        val sdf = when (period) {
            TimePeriod.PERIOD_7D -> SimpleDateFormat("EEE", Locale.getDefault())
            TimePeriod.PERIOD_30D -> SimpleDateFormat("d MMM", Locale.getDefault())
            TimePeriod.PERIOD_90D -> SimpleDateFormat("MMM d", Locale.getDefault())
            TimePeriod.PERIOD_1Y -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
        }

        val stepDays = (period.days.toDouble() / count).coerceAtLeast(1.0)
        var simulatedRate = currentRate

        // Generate backwards from today
        val rawValues = mutableListOf<Pair<Long, Double>>()
        rawValues.add(Pair(System.currentTimeMillis(), currentRate))

        for (i in 1 until count) {
            val wave = sin(i.toDouble() * 0.4) * volatility * 0.5
            val drift = (random.nextDouble() - 0.49) * volatility
            simulatedRate *= (1.0 - drift - wave)
            val time = System.currentTimeMillis() - (i * stepDays * 86400000L).toLong()
            rawValues.add(Pair(time, simulatedRate))
        }

        rawValues.reverse()
        rawValues.forEach { (time, rateVal) ->
            cal.timeInMillis = time
            points.add(ChartPoint(label = sdf.format(cal.time), value = rateVal, timestamp = time))
        }

        return points
    }

    suspend fun syncLiveRates(): Result<Int> = withContext(Dispatchers.IO) {
        val endpoints = listOf(
            "https://open.er-api.com/v6/latest/USD",
            "https://api.frankfurter.app/latest?from=USD",
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"
        )

        for (urlString in endpoints) {
            try {
                val url = URL(urlString)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "9M2PJU-Currency-App/1.0")
                }

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonObject = gson.fromJson(reader, JsonObject::class.java)
                    reader.close()

                    // Handle different JSON structures (er-api "rates", frankfurter "rates", fawazahmed "usd")
                    val ratesObj = when {
                        jsonObject.has("rates") -> jsonObject.getAsJsonObject("rates")
                        jsonObject.has("usd") -> jsonObject.getAsJsonObject("usd")
                        else -> null
                    }

                    if (ratesObj != null) {
                        val updatedRates = mutableMapOf<String, Double>()
                        cachedCurrencies.forEach { curr ->
                            val lookupKeyUpper = curr.code.uppercase()
                            val lookupKeyLower = curr.code.lowercase()
                            val rateElem = if (ratesObj.has(lookupKeyUpper)) {
                                ratesObj.get(lookupKeyUpper)
                            } else if (ratesObj.has(lookupKeyLower)) {
                                ratesObj.get(lookupKeyLower)
                            } else null

                            if (rateElem != null && rateElem.isJsonPrimitive) {
                                val newRate = rateElem.asDouble
                                if (newRate > 0) {
                                    updatedRates[curr.code] = newRate
                                    if (!curr.isCustomRate) {
                                        curr.rate = newRate
                                    }
                                }
                            }
                        }

                        if (updatedRates.isNotEmpty()) {
                            val nowFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                            preferencesRepository.saveOnlineRates(updatedRates, nowFormatted)
                            return@withContext Result.success(updatedRates.size)
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next fallback endpoint
            }
        }
        Result.failure(Exception("All exchange rate servers are currently unreachable. Using offline rates."))
    }
}
