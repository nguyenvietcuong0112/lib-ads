# TÀI LIỆU HƯỚNG DẪN SỬ DỤNG THƯ VIỆN ads_lib

Thư viện `ads_lib` là bộ công cụ tối ưu hóa quảng cáo AdMob, quản lý luồng mở đầu ứng dụng (First Open - FO), theo dõi sự kiện (Tracking Adjust/Firebase), quản lý đồng thuận người dùng (UMP GDPR), đồng bộ cấu hình từ xa (Firebase Remote Config), và tích hợp hệ thống GSM của Mobi.

---

## MỤC LỤC
1. [Cài đặt & Cấu hình hệ thống](#1-cài-đặt--cấu-hình-hệ-thống)
2. [Khởi tạo Application & SDK](#2-khởi-tạo-application--sdk)
3. [Mô hình Cấu hình Luồng First Open (FO Configs)](#3-mô-hình-cấu-hình-luồng-first-open-fo-configs)
4. [Các Lớp Cơ Sở & Giao Diện Của Luồng FO](#4-các-lớp-cơ-sở--giao-diện-của-luồng-fo)
5. [Quản lý Quảng Cáo (Ad Managers)](#5-quản-lý-quảng-cáo-ad-managers)
6. [Quản lý Cấu hình Từ Xa (Remote Config)](#6-quản-lý-cấu-hình-từ-xa-remote-config)
7. [Lưu trữ Cục bộ (SharedPreferences - SPF)](#7-lưu-trữ-cục-bộ-sharedpreferences---SPF)
8. [Các Hàm Tiện Ích & Extension (ViewEx)](#8-các-hàm-tiện-ích--extension-viewex)
9. [Hệ thống Client GSM (Mobi GSM Client)](#9-hệ-thống-client-gsm-Mobi-gsm-client)

---

## 1. CÀI ĐẶT & CẤU HÌNH HỆ THỐNG

### Bước 1: Khai báo Repositories
Trong `settings.gradle` hoặc `settings.gradle.kts` ở thư mục gốc của dự án:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://android-sdk.is.com/") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle/") }
        // Repository của Mobi
        maven {
            url = uri("https://maven.pkg.github.com/appstudio/ads_lib")
            credentials {
                username = "YOUR_GITHUB_USERNAME"
                password = "YOUR_GITHUB_PERSONAL_ACCESS_TOKEN"
            }
        }
    }
}
```

### Bước 2: Thêm Dependency vào App
Trong tệp `build.gradle` (hoặc `build.gradle.kts`) của module `:app`:
```kotlin
dependencies {
    implementation("com.app:library:0.0.2") // Thay đổi phiên bản phù hợp
    
    // Thư viện bổ trợ bắt buộc
    implementation("com.google.android.gms:play-services-ads:24.5.0")
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-config")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.adjust.sdk:adjust-android:5.4.1")
    implementation("com.android.installreferrer:installreferrer:2.2")
}
```

### Bước 3: Cấu hình Java 17 & View Binding
```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}
```

### Bước 4: Khai báo AndroidManifest.xml
Thêm mã ID ứng dụng AdMob và các cờ tối ưu hóa:
```xml
<manifest ...>
    <application ...>
        <!-- Điền App ID AdMob thực tế của bạn -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="@string/app_id"/>

        <meta-data
            android:name="com.google.android.gms.ads.flag.OPTIMIZE_INITIALIZATION"
            android:value="true" />
        <meta-data
            android:name="com.google.android.gms.ads.flag.OPTIMIZE_AD_LOADING"
            android:value="true" />
        <meta-data
            android:name="com.google.android.gms.ads.flag.NATIVE_AD_DEBUGGER_ENABLED"
            android:value="false" />
            
        <service
            android:name="com.mobi.libraryads.commons.fcm.FCMService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

---

## 2. KHỞI TẠO APPLICATION & SDK

Lớp `AdsApplication` là điểm khởi đầu thiết lập của thư viện. Bạn cần tạo một lớp kế thừa từ `Application()` của hệ thống và khởi tạo `AdsApplication`.

```kotlin
package com.example.app

import android.app.Application
import com.mobi.libraryads.BuildConfig
import com.mobi.libraryads.AdsApplication

class App : Application() {
    
    companion object {
        lateinit var adsLibrary: AdsApplication
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Khởi tạo thực thể AdsApplication toàn cục
        adsLibrary = AdsApplication(
            globalClass = this,
            remoteKeys = null, // Có thể truyền Config Model tùy chỉnh kế thừa từ KonfigModel
            isDebug = BuildConfig.DEBUG
        )
        
        // Cấu hình SDK, luồng First Open và hệ thống Tracking
        adsLibrary.initSdk(
            splashConfig = splashConfig,
            languageConfig = languageConfig,
            obConfig = obConfig,
            loadAdsPerScreen = false,
            trackers = listOf(
                FirebaseAdTracker(),             // Mặc định luôn theo dõi qua Firebase Analytics
                AdjustAdTracker("YOUR_TOKEN")    // Hoặc AppsFlyerAdTracker("YOUR_DEV_KEY")
            )
        )
    }
}
```

### 2.1. Cấu hình Tracking Đa Nền Tảng (Multi-MMP / Firebase Only)
Thư viện áp dụng kiến trúc **Tracking Abstraction Layer** giúp tách rời hoàn toàn SDK bên ngoài (Adjust/AppsFlyer) thông qua `compileOnly`. Nhờ đó, ứng dụng không bị ép buộc phải kéo theo SDK thừa:

#### Kịch bản 1: Ứng dụng CHỈ DÙNG FIREBASE (Không tốn thêm byte nào cho Adjust/AppsFlyer)
Trong `app/build.gradle.kts`, không cần thêm dependency Adjust hay AppsFlyer nào:
```kotlin
adsLibrary.initSdk(
    splashConfig = splashConfig,
    languageConfig = languageConfig,
    obConfig = obConfig,
    trackers = listOf(FirebaseAdTracker()) // Chỉ log ad_impression và event lên Firebase
)
```

#### Kịch bản 2: Ứng dụng DÙNG APPSFLYER
Trong `app/build.gradle.kts`:
```kotlin
implementation(libs.appsflyer.android) // hoặc "com.appsflyer:af-android-sdk:6.15.1"
```
Khởi tạo trong `App.kt`:
```kotlin
adsLibrary.initSdk(
    splashConfig = splashConfig,
    languageConfig = languageConfig,
    obConfig = obConfig,
    trackers = listOf(
        FirebaseAdTracker(),
        AppsFlyerAdTracker(devKey = "YOUR_APPSFLYER_DEV_KEY")
    )
)
```

#### Kịch bản 3: Ứng dụng DÙNG ADJUST
Trong `app/build.gradle.kts`:
```kotlin
implementation(libs.adjust.android)
implementation(libs.installreferrer)
```
Khởi tạo trong `App.kt`:
```kotlin
adsLibrary.initSdk(
    splashConfig = splashConfig,
    languageConfig = languageConfig,
    obConfig = obConfig,
    trackers = listOf(
        FirebaseAdTracker(),
        AdjustAdTracker(appToken = "YOUR_ADJUST_TOKEN")
    )
)
```

#### Kịch bản 4: Tự Định Nghĩa Tracker Tùy Biến (Custom Analytics / Server)
```kotlin
adsLibrary.initSdk(
    ...
    trackers = listOf(
        FirebaseAdTracker(),
        CustomAdTracker { revenueData ->
            Log.d("CustomTracking", "Ad paid: ${revenueData.revenue} ${revenueData.currencyCode}")
            // Gửi dữ liệu doanh thu về server riêng của bạn
        }
    )
)
```

---

## 3. MÔ HÌNH CẤU HÌNH LUỒNG FIRST OPEN (FO CONFIGS)

Luồng First Open được định nghĩa thông qua đối tượng cấu hình toàn cục `FOConfigs` bao gồm 3 cấu hình chính: `SplashConfig`, `LanguageConfig`, và `OBConfig`.

```kotlin
object FOConfigs {
    lateinit var splashConfig: SplashConfig
    lateinit var languageConfig: LanguageConfig
    lateinit var obConfig: OBConfig
    var isOrganic = false
    var isLoadAdsPerScreen = false // Bật chế độ load quảng cáo tại từng màn hình
}
```

### 3.0. Chế độ Tải Quảng Cáo Tại Chỗ (Load Ads Per Screen / On-Demand)
Thư viện hỗ trợ 2 cơ chế tải quảng cáo trong luồng FO:
1. **Preload trước màn tiếp theo (Mặc định - `loadAdsPerScreen = false`)**:
   - `SplashActivity` sẽ preload quảng cáo Language (`NATIVE_LANGUAGE`, `NATIVE_LANGUAGE_CLICK`) và quảng cáo Onboarding (`NATIVE_ONBOARDING_1..4`, Full 1-2..3-4, Inter OB).
   - Nếu bật `isLoadNativeOBInLanguage = true` trong `AdsOBConfig`, việc preload Onboarding sẽ dời sang `LanguageActivity`.
2. **Ở màn nào load màn đấy (`loadAdsPerScreen = true`)**:
   - `SplashActivity`: Chỉ load và hiển thị quảng cáo của Splash (Inter Splash, Native Full Inter Splash, Banner Splash). Hoàn toàn không gọi preload sang Language hay Onboarding.
   - `LanguageActivity`: Tự động tải và hiển thị quảng cáo Language (`NATIVE_LANGUAGE`, `NATIVE_LANGUAGE_CLICK`) khi người dùng vào màn hình Language. Không preload Onboarding.
   - `OnboardingActivity`: Tự động tải tất cả quảng cáo của Onboarding khi người dùng vào màn hình Onboarding.
   - Ngoài công tắc tổng `loadAdsPerScreen`, bạn cũng có thể tinh chỉnh độc lập từng màn:
     - `AdsSplashConfig(preloadNextScreenAds = false)`: Splash không preload màn tiếp theo.
     - `AdsOBConfig(isLoadNativeOBInOnboarding = true)`: Onboarding tự tải quảng cáo tại màn Onboarding.
```

### 3.1. SplashConfig
```kotlin
data class SplashConfig(
    val uiSplashConfig: UiSplashConfig,
    val adsSplashConfig: AdsSplashConfig
)

data class UiSplashConfig(
    val resLayout: Int,                          // Layout XML của màn hình Splash
    val homeActivity: Class<out Activity>,       // Activity chính của ứng dụng
    val preHome: OnBeforeHome,                   // Lắng nghe sự kiện trước khi vào Home
    val showFOForever: Boolean = false,          // Luôn hiển thị luồng FO mỗi khi mở app
    val timeout: Long = 30_000,                  // Timeout tối đa để tải quảng cáo (ms)
    val dismissNativeFullOnAdClick: Boolean = true // Tự động đóng Native Full khi click ad và back lại app
)

data class AdsSplashConfig(
    val bannerId: String = "",
    val interHighId: String = "",
    val interAllId: String = "",
    val nativeFullId: String = "",
    val nativeFullHighId: String = "",
    val nativeFullLayout: Int = R.layout.layout_native_full_inter,
    val loadStrategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    val admobAOAId: String = "",
    val isCheckOrganicUser: Boolean = false       // Bỏ qua hiển thị quảng cáo với Organic User
)

interface OnBeforeHome {
    fun onPreloadAds() // Callback dùng để bắt đầu preload các tài nguyên/ads ở trang chủ
}
```

### 3.2. LanguageConfig
```kotlin
data class LanguageConfig(
    val uiConfig: UiLanguageConfig,
    val adsConfig: AdsLanguageConfig,
)

data class UiLanguageConfig(
    val resLayout: Int,                              // Layout XML chọn ngôn ngữ
    val itemLangDefault: Int,                        // Layout item ngôn ngữ bình thường
    val itemLangSelected: Int,                       // Layout item ngôn ngữ được chọn
    val listLanguage: ArrayList<LanguageModel>,      // Danh sách các ngôn ngữ hỗ trợ
    val languageSetting: LanguageSetting? = null     // Callback khi thay đổi ngôn ngữ từ Setting
)

data class AdsLanguageConfig(
    val nativeLangHighId: String = "",
    val nativeLangId: String = "",
    val nativeLangClickHighId: String = "",
    val nativeLangClickId: String = "",
    val layoutNative: Int,
    val layoutNativeClick: Int = layoutNative,
    val loadStrategy: LoadStrategy = LoadStrategy.SEQUENTIAL
)

data class LanguageModel(
    val txtLanguage: Int = 0,                        // String ID tên ngôn ngữ
    val icFlag: Int = 0,                             // Drawable ID cờ quốc gia
    val code: String = "",                           // Mã code (en, vi, fr...)
    var isSelect: Boolean = false                    // Trạng thái được chọn
)
```

### 3.3. OBConfig (Onboarding Config)
```kotlin
data class OBConfig(
    val uiConfig: UiConfig,
    val adsConfig: AdsConfig,
)

data class UiConfig(
    val resLayout: Int? = null,
    val resFragmentOB1: Int,
    val resFragmentOB2: Int,
    val resFragmentOB3: Int,
    val resFragmentOB4: Int = resFragmentOB3,
    val resFragmentOBAdFull: Int = R.layout.fragment_onboarding_ad_full,
    val colorButtonCTA: Int = R.color.main_color_background,
    val nextOBActivity: Class<out Activity>? = null, // Màn hình tùy chỉnh sau OB (ví dụ Permission)
    val afterOB: OnAfterOB? = null
)

data class AdsConfig(
    val nativeOB1Id: String = "",
    val nativeOB2Id: String = "",
    val nativeOB3Id: String = "",
    val nativeOB4Id: String = "",
    val nativeOBFull1Id: String = "",
    val nativeOBFull2Id: String = "",
    val layoutNativeOB1: Int,
    val layoutNativeOB2: Int = layoutNativeOB1,
    val layoutNativeOB3: Int = layoutNativeOB1,
    val layoutNativeOB4: Int = layoutNativeOB1,
    val layoutNativeFullOB: Int = R.layout.admob_native_full_onboarding,
    val interOBId: String = "",
    val nativeFullInterOBId: String = "",
    val nativeFullInterOBLayout: Int = R.layout.layout_native_full_inter,
    val isLoadNativeOBInLanguage: Boolean = false
)
```

---

## 4. CÁC LỚP CƠ SỞ & GIAO DIỆN CỦA LUỒNG FO

Thư viện thiết lập sẵn toàn bộ quy trình của luồng FO (Splash $\rightarrow$ Language $\rightarrow$ Onboarding). 

### 4.1. Lớp Cơ Sở Hoạt Động (BaseActivity, BaseFragment, BaseDialogFragment)
Các Activity/Fragment trong dự án nên kế thừa lớp cơ sở để tận dụng khả năng tự động quản lý vòng đời và ViewBinding:
```kotlin
// Ví dụ Activity
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {
    lateinit var binding: VB
    abstract fun inflateVB(inflater: LayoutInflater): VB
    open fun initView() {}
    open fun loadAds() {}
    open fun showAds() {}
    // Tự động kiểm tra mạng và khởi tạo
}
```

### 4.2. SplashActivity (Bắt buộc kế thừa)
Bạn cần tạo `SplashActivity` của dự án kế thừa từ `SplashActivity` và triển khai 3 nhà cung cấp cấu hình:
```kotlin
package com.example.app

import com.mobi.libraryads.data.*
import com.mobi.libraryads.views.splash.SplashActivity

class SplashActivity : SplashActivity() {

    override fun provideSplashConfig(): SplashConfig {
        return SplashConfig(
            uiSplashConfig = UiSplashConfig(
                resLayout = R.layout.activity_splash_custom,
                homeActivity = MainActivity::class.java,
                preHome = object : OnBeforeHome {
                    override fun onPreloadAds() {
                        // Tải trước ads cho Home tại đây
                    }
                }
            ),
            adsSplashConfig = AdsSplashConfig(
                interHighId = "ca-app-pub-3940256099942544/1033173712",
                interAllId = "ca-app-pub-3940256099942544/1033173712",
                bannerId = "ca-app-pub-3940256099942544/6300978111"
            )
        )
    }

    override fun provideLanguageConfig(): LanguageConfig {
        return LanguageConfig(
            uiConfig = UiLanguageConfig(
                resLayout = R.layout.activity_language_app,
                itemLangDefault = R.layout.item_select_language_default,
                itemLangSelected = R.layout.item_select_language_selected,
                listLanguage = arrayListOf(
                    LanguageModel(R.string.lang_english, R.drawable.ic_en, "en"),
                    LanguageModel(R.string.lang_vietnamese, R.drawable.ic_vi, "vi")
                )
            ),
            adsConfig = AdsLanguageConfig(
                nativeLangId = "ca-app-pub-3940256099942544/2247696110",
                layoutNative = R.layout.admob_layout_native_small
            )
        )
    }

    override fun provideOnboardConfig(): OBConfig {
        return OBConfig(
            uiConfig = UiConfig(
                resFragmentOB1 = R.layout.fragment_ob1,
                resFragmentOB2 = R.layout.fragment_ob2,
                resFragmentOB3 = R.layout.fragment_ob3
            ),
            adsConfig = AdsConfig(
                nativeOB1Id = "ca-app-pub-3940256099942544/2247696110",
                layoutNativeOB1 = R.layout.admob_layout_native_medium
            )
        )
    }
}
```

---

## 5. QUẢN LÝ QUẢNG CÁO (AD MANAGERS)

### 5.1. Banner Ads (`Banner`)
Hỗ trợ Banner Thường, Thích Ứng (Adaptive), và Banner Neo (Collapsible). Lớp tự động lắng nghe vòng đời để hủy banner khi Activity bị đóng (`ON_DESTROY`).

```kotlin
// Tải và hiển thị banner
Banner.requestBanner(
    activity = this,
    id = "ca-app-pub-3940256099942544/6300978111",
    typeAds = Banner.TypeAds.BANNER_COLLAPSIBLE_BOTTOM, // Cấu hình loại banner
    adFrame = binding.layoutBannerContainer,               // FrameLayout nhận quảng cáo
    canShowAd = true,                                     // Điều kiện cấu hình
    onResult = { adView ->
        if (adView != null) {
            // Tải quảng cáo thành công
        }
    },
    onShown = {
        // Quảng cáo đã hiển thị
    }
)

// Hủy banner thủ công (nếu cần)
Banner.destroyBannerAds(activity = this)
```

> [!TIP]
> **Chuẩn Khai Báo Shimmer Cho Banner (Client-included Shimmer):**
> Bạn nên đặt thẻ `<include layout="@layout/..." />` trực tiếp bên trong `FrameLayout` chứa banner trong XML màn hình. Thư viện sẽ tự động nhận diện, chạy animation shimmer trong lúc tải, và tự động ẩn shimmer khi banner hiển thị thành công hoặc lỗi (nếu không include, thư viện tự động dùng fallback shimmer).
> ```xml
> <FrameLayout
>     android:id="@+id/layoutAds"
>     android:layout_width="match_parent"
>     android:layout_height="wrap_content"
>     android:layout_alignParentBottom="true">
> 
>     <include layout="@layout/layout_shimmer_load_ads_native_banner" />
> </FrameLayout>
> ```

### 5.2. Interstitial Ads (`Inter`)
Quản lý các tiến trình tải trước, giới hạn số lần hiển thị (capping time) dựa trên cấu hình Remote Config.

```kotlin
// 1. Tải trước Interstitial Ad
Inter.preLoadInter(
    activity = this,
    adModel = InterAdModel(name = "inter_save", id = "ca-app-pub-3940256099942544/1033173712"),
    canShowAd = true
)

// 2. Hiển thị Interstitial Ad
Inter.showInter(
    activity = this,
    adName = "inter_save",
    nextAction = { onDismiss ->
        // Thực hiện hành động tiếp theo sau khi tắt quảng cáo
        navigateToNextScreen()
    },
    onShown = {
        // Callback khi quảng cáo bắt đầu hiển thị
    },
    preload = true, // Tự động load lại ad mới sau khi ad hiện tại bị đóng
    canShowAd = true
)

// 3. Load và Show trực tiếp (không thông qua preload)
Inter.loadAndShowInter(
    activity = this,
    adName = "inter_export",
    adId = "ca-app-pub-3940256099942544/1033173712",
    nextAction = { navigateToNextScreen() }
)
```

### 5.3. Native Ads (`NativeManager`)
Hệ thống tải, bộ đệm cache, kết hợp sàn giá cao/thấp (High Floor/All Price) và xử lý hiển thị quảng cáo dạng Native.

#### 5.3.1. Danh sách tham số cấu hình từ xa (Firebase Remote Config)
Các tham số có ký tự đầu hoặc tên dạng `canShowAd`, `canShowIdAll`, `canShowIdHigh` được thiết kế để truyền giá trị trực tiếp từ **Firebase Remote Config** (được định nghĩa trong `ValueRemoteConfigModule` hoặc Model của dự án):
* **`canShowAd`**: Bật/tắt quảng cáo native tại vị trí tương ứng (ví dụ: `ValueRemoteConfigModule.native_onboarding_1`).
* **`canShowIdHigh` / `canShowIdAll`**: Bật/tắt riêng biệt tầng giá cao (High Floor) hoặc toàn bộ giá (All Price) để tối ưu hóa doanh thu thác nước (Waterfall).

---

#### 5.3.2. Danh sách đầy đủ các thuộc tính kiểm tra trạng thái
* **`fun isAdReady(adName: String): Boolean`**: Kiểm tra xem quảng cáo native có sẵn sàng để hiển thị hay chưa (đã tải xong và có quảng cáo hợp lệ trong cache).
* **`fun isLoadDone(adName: String): Boolean`**: Kiểm tra xem quảng cáo đã hoàn thành tiến trình tải hay chưa (trả về `true` nếu không ở trạng thái `LOADING`).
* **`fun isFullscreenAdShowing(adName: String): Boolean`**: Kiểm tra xem quảng cáo Native toàn màn hình có đang hiển thị trên giao diện hay không.

---

#### 5.3.3. Chi tiết toàn bộ các hàm Load / Show của NativeManager

```kotlin
// ══════════════════════════════════════════
// 1. CÁC HÀM TẢI TRƯỚC QUẢNG CÁO (PRELOAD)
// ══════════════════════════════════════════

/**
 * Tải trước quảng cáo Native cơ bản (chỉ dùng 1 ID quảng cáo).
 * @param canShowAd: Điền giá trị Remote Config (ví dụ: ValueRemoteConfigModule.native_onboarding_1)
 */
fun preloadNative(
    context: Context,
    adName: String,
    adId: String = "",
    canShowAd: Boolean = true,
    callback: INativeAdCallback? = null
)

/**
 * Tải trước quảng cáo Native với sàn giá cao và thấp song song/nối tiếp.
 * @param canShowIdAll: Cấu hình Remote Config cho sàn giá thường (All Price)
 * @param canShowIdHigh: Cấu hình Remote Config cho sàn giá cao (High Floor)
 */
fun preloadNativeWithHigh(
    context: Context,
    adName: String,
    adId: String,
    idHigh: String,
    strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    canShowIdAll: Boolean = true,
    canShowIdHigh: Boolean = true,
    callback: INativeAdCallback? = null
)

// ══════════════════════════════════════════
// 2. CÁC HÀM HIỂN THỊ QUẢNG CÁO (SHOW)
// ══════════════════════════════════════════

/**
 * Hiển thị quảng cáo Native thông thường lên một FrameLayout.
 * @param canShowAd: Điều kiện từ Remote Config để cho phép hiển thị
 */
fun showNative(
    adFrame: FrameLayout,
    adName: String,
    adId: String = "",
    adLayout: Int = 0,
    isPreload: Boolean = false,
    canShowAd: Boolean = true,
    callback: INativeAdCallback? = null
)

/**
 * Hiển thị quảng cáo Native dạng tự động làm mới (Collapsible Native) theo chu kỳ.
 * @param refreshTime: Thời gian làm mới tự động (mặc định là 30.000ms)
 * @param canShowAd: Điều kiện từ Remote Config
 */
fun showCollapsibleNative(
    activity: AppCompatActivity,
    adFrame: FrameLayout,
    adName: String,
    adId: String = "",
    adLayout: Int,
    refreshTime: Long = 30_000L,
    isPreload: Boolean = true,
    canShowAd: Boolean = true,
    callback: INativeAdCallback? = null
)

/**
 * Dừng cơ chế tự động làm mới của quảng cáo Collapsible theo Activity.
 */
fun stopCollapsible(activity: Activity)

// ══════════════════════════════════════════
// 3. CÁC HÀM GỘP LOAD VÀ SHOW NGAY (LOAD & SHOW)
// ══════════════════════════════════════════

/**
 * Xóa cache cũ, lập tức tải và hiển thị quảng cáo ngay khi tải xong (1 ID).
 */
fun loadAndShowNative(
    adFrame: FrameLayout,
    adName: String,
    adId: String = "",
    adLayout: Int = 0,
    isPreload: Boolean = false,
    canShowAd: Boolean = true,
    callback: INativeAdCallback? = null
)

/**
 * Xóa cache cũ, lập tức tải và hiển thị quảng cáo với sàn giá cao/thấp (2 ID).
 */
fun loadAndShowNativeWithHigh(
    adFrame: FrameLayout,
    adName: String,
    adId: String = "",
    idHigh: String = "",
    strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    adLayout: Int = 0,
    isPreload: Boolean = false,
    canShowIdAll: Boolean = true,
    canShowIdHigh: Boolean = true,
    callback: INativeAdCallback? = null
)

// ══════════════════════════════════════════
// 4. HIỂN THỊ NATIVE DẠNG TOÀN MÀN HÌNH (COUNTDOWN OVERLAY)
// ══════════════════════════════════════════

/**
 * Hiển thị Native dưới dạng giao diện đè toàn màn hình kèm bộ đếm ngược (1 ID).
 * @param countdownSeconds: Thời gian đếm ngược hiển thị nút bỏ qua (mặc định 3s)
 * @param startCountdownImmediately: Nếu false, bộ đếm ngược sẽ chờ gọi startFullscreenCountdown
 */
fun showFullscreenNativeWithCountdown(
    activity: AppCompatActivity,
    adName: String,
    adId: String = "",
    adLayoutRes: Int,
    countdownSeconds: Int = 3,
    isPreload: Boolean = true,
    canShowAd: Boolean = true,
    startCountdownImmediately: Boolean = true,
    layoutContainerId: Int = android.R.id.content,
    callback: INativeAdCallback? = null,
    onDismissed: (() -> Unit)? = null
)

/**
 * Hiển thị Native đè toàn màn hình kèm bộ đếm ngược hỗ trợ sàn giá cao/thấp (2 ID).
 */
fun showFullscreenNativeWithCountdownWithHigh(
    activity: AppCompatActivity,
    adName: String,
    adId: String = "",
    idHigh: String = "",
    strategy: LoadStrategy = LoadStrategy.SEQUENTIAL,
    adLayoutRes: Int,
    countdownSeconds: Int = 3,
    isPreload: Boolean = true,
    canShowIdAll: Boolean = true,
    canShowIdHigh: Boolean = true,
    startCountdownImmediately: Boolean = true,
    layoutContainerId: Int = android.R.id.content,
    callback: INativeAdCallback? = null,
    onDismissed: (() -> Unit)? = null
)

/**
 * Kích hoạt đếm ngược cho quảng cáo Native Fullscreen (khi tham số startCountdownImmediately đặt là false)
 */
fun startFullscreenCountdown(adName: String)

// ══════════════════════════════════════════
// 5. CÁC HÀM TIỆN ÍCH QUẢN LÝ THỦ CÔNG
// ══════════════════════════════════════════

/**
 * Giải phóng và hủy toàn bộ cache của quảng cáo (nativeAd, activeAd) cùng các bộ làm mới quảng cáo.
 */
fun clearAll()

/**
 * Trích xuất và tiêu thụ quảng cáo đã tải (sau khi gọi, trạng thái quảng cáo chuyển về NOT_LOADED).
 */
fun consumeAd(adName: String): NativeAd?

/**
 * Tiêu thụ quảng cáo đã tải mà không thay đổi hay hủy bỏ quảng cáo active hiện tại.
 */
fun consumeAdWithoutActive(adName: String): NativeAd?

/**
 * Kiểm tra xem quảng cáo preloaded có sẵn và chưa bị tiêu thụ hay không.
 */
fun isPreloadedAdReady(adName: String): Boolean

/**
 * Trực tiếp vẽ (render) đối tượng NativeAd của Google vào FrameLayout đích.
 */
fun renderAd(adFrame: FrameLayout, nativeAd: NativeAd, adLayoutRes: Int, adapterClassName: String = "")

/**
 * Hiển thị giao diện shimmer loading lên khung chứa quảng cáo.
 */
fun showLoading(adFrame: FrameLayout, adLayoutRes: Int)
```

> [!IMPORTANT]
> **Cơ chế tự động dọn dẹp quảng cáo (Mới)**: Khi gọi bất kỳ phương thức hiển thị nào (`showNative`, `showCollapsibleNative`, `loadAndShowNative`...), `NativeManager` sẽ tự động tìm kiếm `LifecycleOwner` từ `Context` của `FrameLayout` nhận quảng cáo. Khi Activity/Fragment bị hủy (`ON_DESTROY`), quảng cáo hoạt động tương ứng sẽ tự động giải phóng tài nguyên (`destroy()`) và đặt cache về `null` (`activeAd = null`). Điều này giải phóng RAM, khắc phục lỗi rò rỉ bộ nhớ, và đảm bảo quảng cáo mới luôn được tải khi người dùng quay lại hoặc chuyển màn hình.

---

#### 5.3.4. Chuẩn Shimmer Loading Trong XML Màn Hình (Client-included Shimmer)

Để loại bỏ hiện tượng nhấp nháy hoặc giật layout (Cumulative Layout Shift - CLS), phía client gọi quảng cáo được khuyến khích đặt thẻ `<include layout="@layout/..." />` trực tiếp bên trong `FrameLayout` chứa quảng cáo tại XML của màn hình:

```xml
<FrameLayout
    android:id="@+id/layoutAds"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_alignParentBottom="true">

    <!-- Shimmer hiển thị tức thì ngay khi mở màn hình -->
    <include layout="@layout/layout_shimmer_load_ads_native_banner" />
</FrameLayout>
```

**Cách hoạt động tối ưu của thư viện:**
1. **Khi mở màn hình**: Shimmer có sẵn trong XML hiển thị tức thì, không có độ trễ do inflate runtime.
2. **Khi gọi `NativeManager.showNative(...)`**:
   - Nếu ad đang tải hoặc chưa tải xong: Thư viện tự động giữ nguyên view shimmer, bật hiệu ứng `startShimmer()`, đồng thời ẩn các ad view cũ (nếu có từ lần reload trước).
   - Nếu ad tải thành công: Thư viện tự động dừng shimmer (`stopShimmer()`), đặt shimmer về `View.GONE`, và hiển thị `NativeAdView`.
   - Nếu ad lỗi hoặc không đủ điều kiện (PRO / mất mạng / Remote Config tắt): Thư viện tự động dừng shimmer, ẩn toàn bộ view con và ẩn `layoutAds` (`setGone()`).
   - **Tương thích ngược**: Nếu client không include shimmer trong XML (`adFrame.childCount == 0`), thư viện tự động kích hoạt fallback shimmer như trước.


### 5.4. App Open Ads (`OpenAds`)
Tự động lắng nghe vòng đời chuyển đổi ứng dụng từ Background lên Foreground để kích hoạt hiển thị quảng cáo App Open.

```kotlin
// Bật/tắt nhanh hiển thị quảng cáo khi chuyển đổi màn hình (ví dụ lúc chọn file hoặc thanh toán IAP)
OpenAds?.disableShowAOA() // Tạm khóa
OpenAds?.enableShowAOA()  // Mở khóa trở lại
```

### 5.5. Rewarded Ads (`Reward`)
Quản lý luồng hiển thị quảng cáo video nhận thưởng.

```kotlin
// 1. Tải trước quảng cáo thưởng
Reward.loadRewardAd(
    activity = this,
    rewardId = "ca-app-pub-3940256099942544/5224354917"
)

// 2. Hiển thị quảng cáo thưởng
Reward.showRewardAd(
    activity = this,
    nextAction = {
        // Thực thi sau khi hoàn thành xem quảng cáo hoặc tắt đi
    },
    onShowSuccess = {
        // Gọi thành công
    },
    onUserEarnedReward = {
        // Trao phần quà cho người dùng tại đây
        unlockFeature()
    }
)
```

---

## 6. QUẢN LÝ CẤU HÌNH TỪ XA (REMOTE CONFIG)

Thư viện sử dụng đại diện ủy quyền (Delegates) để lấy trực tiếp các cấu hình từ Firebase Remote Config dựa trên lớp định danh `KonfigModel`.

### Cấu hình Remote Config mặc định trong hệ thống (`ValueRemoteConfigModule`)
| Tên thuộc tính | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `resume_open_app` | Boolean | Bật/tắt quảng cáo App Open khi vào lại app |
| `banner_splash` | Boolean | Bật/tắt banner tại màn Splash |
| `inter_splash` | Boolean | Bật/tắt Interstitial tại màn Splash |
| `native_language` | Boolean | Bật/tắt Native tại màn hình chọn ngôn ngữ |
| `native_onboarding_1` | Boolean | Bật/tắt Native tại màn Onboarding 1 |
| `reward_ad` | Boolean | Cấu hình bật/tắt toàn bộ quảng cáo thưởng |
| `ad_full_capping_time` | Int (ms) | Khoảng thời gian giãn cách giữa hai lần hiển thị ad full screen |
| `countdown_native_full_inter`| Int (seconds) | Thời gian đếm ngược của Native Full |

### Thêm Remote Config mới
```kotlin
object AppRemoteConfig : KonfigModel {
    val my_custom_feature_flag by konfig("my_custom_feature_flag", false)
    val custom_reward_coins by konfig("custom_reward_coins", 10)
}
```

---

## 7. LƯU TRỮ CỤC BỘ (SHAREDPREFERENCES - SPF)

Lớp `SPF` cung cấp cách truy xuất và ghi dữ liệu nhanh vào bộ nhớ cấu hình SharedPreferences của ứng dụng.

```kotlin
val sp = SPF(context)

// Thiết lập thuộc tính
sp.is_app_pro = true // Chuyển đổi trạng thái Premium (Ẩn toàn bộ ads)
sp.language_code_selected = "vi" // Lưu mã ngôn ngữ người dùng lựa chọn

// Truy xuất thuộc tính
val isPro = sp.is_app_pro
val secondOpen = sp.is_second_time_open_app
val countSession = sp.count_session_app
```

---

## 8. CẤU HÌNH TIỆN ÍCH & EXTENSION (VIEWEX)

Thư viện tích hợp sẵn các hàm mở rộng hữu ích cho lập trình Android:

### 8.1. Kiểm tra Mạng & Đổi màu UI
```kotlin
context.isInternetConnected() // Trả về true/false trạng thái mạng tức thời
activity.changeStatusBarColor(R.color.black, lightStatusBar = false) // Đổi màu status bar
activity.transparentStatusBar() // Làm trong suốt status bar
activity.hideStatusBar() // Ẩn status bar
activity.hideNavigationBar() // Ẩn thanh điều hướng dưới cùng
```

### 8.2. Hiển thị & Ẩn View nhanh
```kotlin
view.setVisible() // View.VISIBLE
view.setInVisible() // View.INVISIBLE
view.setGone() // View.GONE
```

### 8.3. Mở nhanh Activity
```kotlin
context.openActivity(MainActivity::class.java) {
    // Truyền dữ liệu Bundle qua DSL
    putString("EXTRA_MODE", "EDIT")
    putInt("EXTRA_ID", 102)
}
context.openActivityAndClearApp(SplashActivity::class.java) // Mở và xóa toàn bộ ngăn xếp
```

### 8.4. Ngăn chặn Double Click & Đóng bàn phím
```kotlin
// Cho phép click 1 lần và giới hạn click kế tiếp sau 1000ms
view.clickOnce(threshold = 1000L) {
    doAction()
}

activity.hideKeyboardEdt() // Ẩn bàn phím ảo
```

### 8.5. Xử lý Nút Back của Hệ thống
```kotlin
// Thay thế hàm onBackPressed truyền thống
activity.addOnBackPressedDispatcher {
    // Sự kiện xử lý nút BACK
    showExitDialog()
}
```

### 8.6. Hiển thị Toast thông minh
```kotlin
context.showToastOnceEvery15Seconds("Vui lòng đợi...") // Giới hạn tần suất hiển thị toast
context.showToastShort("Hoàn thành!")
```

---

## 9. HỆ THỐNG CLIENT GSM (Mobi GSM CLIENT)

Mobi cung cấp hệ thống GSM tích hợp để quản lý và kiểm tra giao dịch mua hàng (IAP) hoặc cấu hình sản phẩm từ server.

```kotlin
// Kiểm tra trạng thái đăng nhập
GSMUtil.login(context, gsmAppId, appVersion)

// Xác thực giao dịch mua hàng qua IAP GSM
GSMUtil.verifyIAP(
    context = context,
    gsmAppId = "YOUR_GSM_APP_ID",
    productId = "iap_premium_package",
    purchaseToken = "PURCHASE_TOKEN",
    callback = object : VerifyIAPGSMCallback {
        override fun onSuccess(p0: String?) {
            // Xác thực thành công giao dịch mua hàng
            sp.is_app_pro = true
        }
        override fun onFail(p0: String?) {
            // Xác thực thất bại
        }
    }
)
```

---

## 10. HỆ THỐNG TRACKING DOANH THU & XÁC ĐỊNH NGƯỜI DÙNG ORGANIC

Thư viện tích hợp hệ thống tracking doanh thu quảng cáo AdMob đa nền tảng theo mô hình **Composite Pattern (`AdTrackingManager`)** kết hợp khả năng tự động xác định người dùng **Organic** ngay cả khi chỉ sử dụng Firebase (không cần cài đặt Adjust hay AppsFlyer).

### 10.1. Danh sách các Tracker hỗ trợ

1. **`FirebaseAdTracker` (Mặc định)**:
   - Tự động ghi nhận sự kiện `ad_impression` lên Firebase Analytics với đầy đủ doanh thu, ad unit, format và platform.
   - **Tự động kiểm tra Organic User**: Sử dụng trực tiếp **Google Play Install Referrer API** (`com.android.installreferrer:installreferrer:2.2`) để phân loại người dùng ngay khi khởi chạy app:
     - Nếu referrer chứa `gclid` (Google Ads UAC/Search), `utm_medium=cpc`, `utm_medium=paid` $\rightarrow$ `isOrganic = false` (Người dùng trả phí / Non-organic).
     - Nếu referrer chứa `utm_medium=organic` hoặc cài đặt trực tiếp không chứa tham số quảng cáo $\rightarrow$ `isOrganic = true` (Người dùng Organic).
   - Tự động lưu cache vào `SPF.is_organic` và `SPF.is_tracked_organic` để các lần mở app sau có kết quả tức thì.
   - Bắn sự kiện Firebase `"user_organic_session_${session}"` và đặt User Property `traffic_channel = "organic" | "paid"`.

2. **`AdjustAdTracker(appToken)`**:
   - Tích hợp Adjust SDK, ghi nhận `AdjustAdRevenue` khi có hiển thị ad.
   - Lắng nghe `setOnAttributionChangedListener` để cập nhật trạng thái `isOrganic` dựa trên mạng lưới phân bổ (`network`).

3. **`AppsFlyerAdTracker(devKey)`**:
   - Tích hợp AppsFlyer SDK, ghi nhận sự kiện `af_ad_revenue`.
   - Lắng nghe `AppsFlyerConversionListener.onConversionDataSuccess` (`af_status`, `media_source`) để xác định `isOrganic`.

### 10.2. Cách sử dụng trong Application

```kotlin
adsLibrary.initSdk(
    // ... config splash, language, ob ...
    trackers = listOf(
        FirebaseAdTracker(), // Luôn bật Firebase + Google Play Install Referrer
        AdjustAdTracker("YOUR_ADJUST_TOKEN") // Tuỳ chọn: truyền Adjust nếu dự án dùng Adjust
        // AppsFlyerAdTracker("YOUR_DEV_KEY") // Tuỳ chọn: truyền AppsFlyer nếu dự án dùng AppsFlyer
    )
)
```

---
> Hướng dẫn này được xây dựng chi tiết cho phiên bản thư viện hiện tại để phát triển ứng dụng và cho các Trợ lý AI có thể hiểu rõ cách thiết kế của mã nguồn.
