package my.hamradio.currencyconverter.data.model

import com.google.gson.annotations.SerializedName

data class Currency(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("flag") val flag: String,
    @SerializedName("region") val region: String,
    @SerializedName("rate") var rate: Double, // Rate relative to 1 USD
    @SerializedName("popular") val isPopular: Boolean = false,
    var defaultRate: Double = rate,
    var isCustomRate: Boolean = false,
    var isFavorite: Boolean = false
)

data class CurrencyDataSet(
    @SerializedName("base") val base: String,
    @SerializedName("updatedAt") val updatedAt: String,
    @SerializedName("currencies") val currencies: List<Currency>
)
