-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class my.hamradio.currencyconverter.data.model.** { *; }

# CameraX & ML Kit Vision OCR
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn androidx.camera.**
