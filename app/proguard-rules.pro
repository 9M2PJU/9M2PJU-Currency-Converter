-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class my.hamradio.currencyconverter.data.model.** { *; }
