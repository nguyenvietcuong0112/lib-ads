# ==============================================================================
# MOBI ADS LIBRARY - CONSUMER PROGUARD RULES
# Tự động gộp vào cấu hình ProGuard / R8 của ứng dụng tích hợp (Client App)
# ==============================================================================

# 1. Bỏ qua cảnh báo missing classes cho các SDK tracking tùy chọn (compileOnly)
# Giúp client app không dùng Adjust hoặc AppsFlyer vẫn build Release bình thường
-dontwarn com.adjust.sdk.**
-dontwarn com.appsflyer.**

# 2. Giữ lại các data class, callback, config và model của thư viện Mobi Ads
-keep class com.mobi.libraryads.data.** { *; }
-keep interface com.mobi.libraryads.data.** { *; }
-keep class com.mobi.libraryads.commons.remote.** { *; }
-keep class com.mobi.libraryads.commons.tracking.model.** { *; }
-keep class com.mobi.libraryads.ads.inter_ads.InterAdModel { *; }
-keep class com.mobi.libraryads.ads.native_ads.model.** { *; }
-keep class com.mobi.libraryads.ads.native_ads.callback.** { *; }
-keep class com.mobi.libraryads.ads.utils.** { *; }

# 3. GSM Client & Retrofit Service (nếu có sử dụng)
-keep class com.mobi.libraryads.commons.GSM.** { *; }
-keep interface com.mobi.libraryads.commons.GSM.** { *; }

# 4. Google Play Install Referrer
-keep class com.android.installreferrer.** { *; }
-dontwarn com.android.installreferrer.**

# 5. Google Mobile Ads & UMP SDK
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.ump.**
