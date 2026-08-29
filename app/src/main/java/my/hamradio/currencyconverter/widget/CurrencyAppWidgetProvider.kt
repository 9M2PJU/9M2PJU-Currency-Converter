package my.hamradio.currencyconverter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import my.hamradio.currencyconverter.MainActivity
import my.hamradio.currencyconverter.R
import my.hamradio.currencyconverter.data.repository.CurrencyRepository
import my.hamradio.currencyconverter.data.repository.PreferencesRepository
import java.util.Locale

class CurrencyAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = PreferencesRepository(context)
        val repo = CurrencyRepository(context, prefs)
        val allCurrencies = repo.getAllCurrencies()

        val baseCode = prefs.baseCurrencyCode
        val baseCurrency = allCurrencies.firstOrNull { it.code == baseCode } ?: allCurrencies.first()
        val favorites = prefs.getFavorites()
        val favCurrencies = allCurrencies.filter { it.code != baseCode && favorites.contains(it.code) }.take(3)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_currency_glance)

            // Setup click to open App
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            views.setTextViewText(R.id.widget_base_text, "1 ${baseCurrency.code} (${baseCurrency.flag})")

            // Populate rows
            if (favCurrencies.isNotEmpty()) {
                val c1 = favCurrencies[0]
                val rate1 = repo.convert(1.0, baseCurrency.code, c1.code)
                views.setTextViewText(R.id.rate_label_1, "${c1.flag} ${c1.code}")
                views.setTextViewText(R.id.rate_val_1, String.format(Locale.US, "%.3f", rate1))
            }

            if (favCurrencies.size > 1) {
                val c2 = favCurrencies[1]
                val rate2 = repo.convert(1.0, baseCurrency.code, c2.code)
                views.setTextViewText(R.id.rate_label_2, "${c2.flag} ${c2.code}")
                views.setTextViewText(R.id.rate_val_2, String.format(Locale.US, "%.3f", rate2))
            }

            if (favCurrencies.size > 2) {
                val c3 = favCurrencies[2]
                val rate3 = repo.convert(1.0, baseCurrency.code, c3.code)
                views.setTextViewText(R.id.rate_label_3, "${c3.flag} ${c3.code}")
                views.setTextViewText(R.id.rate_val_3, String.format(Locale.US, "%.3f", rate3))
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
