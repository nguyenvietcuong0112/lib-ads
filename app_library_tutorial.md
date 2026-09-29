# Hướng Dẫn Tích Hợp và Sử Dụng Toàn Diện Thư Viện Quảng Cáo `mobilibraryads`

> [!NOTE]
> Tài liệu này cung cấp hướng dẫn chi tiết và đầy đủ nhất dành cho các lập trình viên để tích hợp thư viện quảng cáo [mobilibraryads] (dưới dạng dependency Maven `com.app:library` hoặc module local) vào bất kỳ dự án Android nào. Thư viện tích hợp sẵn Google AdMob, Adjust SDK, Firebase Remote Config, luồng Onboarding chuyên nghiệp cùng hàng loạt tiện ích tối ưu.

---

## 1. Cấu Hình Dự Án (Setup & Dependencies)

### Bước 1: Khai báo Repository trong `settings.gradle` hoặc `settings.gradle.kts`
Thư viện được phân phối qua hệ thống **GitHub Packages** của App Studio. Cần khai báo repository với thông tin xác thực để gradle có thể tải về:

```groovy
// settings.gradle (Groovy)
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven {
            url = uri("https://maven.pkg.github.com/appstudio/ads_lib")
            credentials {
                username = "USERNAME" // Thay thế bằng GitHub username của bạn hoặc tài khoản chung
                password = "GITHUB_ACCESS_TOKEN" // GitHub Personal Access Token (PAT) có quyền read packages
            }
        }
    }
}
```

### Bước 2: Thêm Dependency vào `app/build.gradle` hoặc `app/build.gradle.kts`
Khai báo sử dụng thư viện và các thư viện bổ trợ cần thiết khác:

```groovy
// app/build.gradle
dependencies {
    // === App Studio Ads Library ===
    // Dùng bản maven package:
    implementation("com.app:library:0.0.7") 

    // === Các thư viện AdMob & Mediation ===
    implementation(libs.play.services.ads)

    // === Adjust SDK & Referrer ===
    implementation(libs.adjust.android)
    implementation(libs.installreferrer)
    implementation(libs.adjust.android.webbridge)

    // === Retrofit & Gson (Dùng cho GSM SDK) ===
    implementation(libs.okhttp)
    implementation(libs.converter.gson)

    // === Firebase Services ===
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // === UI Libraries ===
    implementation(libs.shimmer) // Hiệu ứng shimmer loading cho Native Ads
    implementation(libs.glide)   // Load hình ảnh trong Native Ads Custom
    implementation(libs.lottie)  // Hiển thị Lottie Animation

    //Thêm mediation cho admob (trong thư viện đã khởi tạo, không cần khởi tạo trên app)
}
```

### Bước 3: Cấu hình Version Catalog (`libs.versions.toml`)
Đảm bảo định nghĩa chính xác phiên bản các thư viện trong [libs.versions.toml] để tránh xung đột phiên bản:

```toml
[versions]
agp = "8.13.2"
kotlin = "2.2.20"
coreKtx = "1.18.0"
appcompat = "1.7.1"
material = "1.14.0"
activity = "1.13.0"
constraintlayout = "2.2.1"

adjustAndroidWebbridge = "5.7.0"
converterGson = "3.0.0"
firebaseBom = "34.15.0"
firebaseMessaging = "25.1.0"
fragmentKtx = "1.8.9"
glide = "5.0.7"
installreferrer = "2.2"
lottie = "6.7.1"
okhttp = "5.4.0"
playServicesAds = "25.4.0"
shimmer = "0.5.0"
googleServices = "4.5.0"

[libraries]
androidx-core = { group = "androidx.core", name = "core", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-activity = { group = "androidx.activity", name = "activity", version.ref = "activity" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }

adjust-android = { module = "com.adjust.sdk:adjust-android", version.ref = "adjustAndroidWebbridge" }
adjust-android-webbridge = { module = "com.adjust.sdk:adjust-android-webbridge", version.ref = "adjustAndroidWebbridge" }
androidx-fragment-ktx = { module = "androidx.fragment:fragment-ktx", version.ref = "fragmentKtx" }
converter-gson = { module = "com.squareup.retrofit2:converter-gson", version.ref = "converterGson" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics" }
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-config = { module = "com.google.firebase:firebase-config" }
firebase-messaging = { module = "com.google.firebase:firebase-messaging", version.ref = "firebaseMessaging" }
glide = { module = "com.github.bumptech.glide:glide", version.ref = "glide" }
installreferrer = { module = "com.android.installreferrer:installreferrer", version.ref = "installreferrer" }
lottie = { module = "com.airbnb.android:lottie", version.ref = "lottie" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
play-services-ads = { module = "com.google.android.gms:play-services-ads", version.ref = "playServicesAds" }
shimmer = { module = "com.facebook.shimmer:shimmer", version.ref = "shimmer" }
```

### Bước 4: Khai báo Metadata AdMob App ID trong `AndroidManifest.xml`
Bạn phải thêm App ID của AdMob vào file manifest của ứng dụng:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application>
        <!-- AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="@string/gsm_app_id" /> <!-- Hoặc chuỗi ID AdMob của bạn -->
    </application>
</manifest>
```

---

## 2. Khởi Tạo Thư Viện (Application Initialization)

Để khởi tạo SDK và các dịch vụ đi kèm (AdMob, Firebase Remote Config, Adjust, Network Observer), bạn cần cấu hình trong class `Application` của dự án sử dụng [AdsApplication.kt]:

```kotlin
import android.app.Application
import com.mobi.libraryads.AdsApplication
import com.mobi.libraryads.commons.utils.AdsLog

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initLibrary()
    }

    private fun initLibrary() {
        // 1. Khởi tạo instance AdsApplication và liên kết Remote Config của App
        val adsLibrary = AdsApplication(
            globalClass = this,
            remoteKeys = RemoteConfigValueApp, // Cấu hình các key Remote Config trong ứng dụng của bạn (kế thừa KonfigModel)
            isDebug = BuildConfig.DEBUG,       // Bật chế độ debug (Sandbox cho Adjust, hiển thị logs)
        )

        // 2. Khởi chạy SDK với các token tương ứng
        adsLibrary.initSdk(
            adjustAppToken = getString(R.string.token_adjust),
            gsmAppId = getString(R.string.gsm_app_id) // ID đăng nhập GSM
        )
        
        // Optional: Bật logcat ngay cả khi debug = false
        // AdsLog.turnOnLogcat(this)
    }
}
```

---

## 3. SharedPreferences Helper (`SPF`)

Lớp [SPF.kt] là một wrapper hỗ trợ đọc/ghi dữ liệu cấu hình & trạng thái người dùng một cách đơn giản và an sau vào `SharedPreferences`:

### Các trường dữ liệu hỗ trợ sẵn:
| Thuộc tính (Property) | Kiểu dữ liệu | Ý nghĩa |
| :--- | :--- | :--- |
| `is_app_pro` | `Boolean` | Trạng thái Premium/VIP của App. **Nếu bằng `true`, thư viện tự động ẩn toàn bộ quảng cáo.** |
| `language_code_selected` | `String` | Mã ngôn ngữ (language code) được người dùng chọn (ví dụ: `"vi"`, `"en"`). |
| `is_second_time_open_app`| `Boolean` | Đánh dấu người dùng đã mở app từ lần thứ 2 trở đi. |
| `is_start_ob` | `Boolean` | Đánh dấu người dùng đã bắt đầu vào màn Onboarding. |
| `count_session_app` | `Int` | Bộ đếm số lần mở app/session. |
| `is_organic` | `Boolean` | Xác định người dùng Organic từ Adjust Attribution. |
| `is_click_native_collapsible` | `Boolean` | Trạng thái người dùng click vào ad Native Collapsible. |

### Cách sử dụng:
```kotlin
import com.mobi.libraryads.commons.sharepreference.SPF

val spf = SPF(context)

// Ghi nhận User mua gói VIP/Pro
spf.is_app_pro = true

// Đọc trạng thái VIP/Pro
if (spf.is_app_pro) {
    // Ẩn các chức năng/giao diện mua hàng, nâng cấp VIP
}

// Lưu ngôn ngữ được chọn
spf.language_code_selected = "en"
```

---

## 4. Quản Lý Firebase Remote Config (`KonfigModel`)

Thư viện cung cấp lớp [KonfigModel.kt] để quản lý tập trung và an toàn các biến cấu hình từ Firebase Remote Config bằng cơ chế delegate `by konfig`.

### 4.1 Cấu hình Remote Config mặc định của Thư viện
Thư viện đã định nghĩa sẵn bộ Remote Config mặc định trong [ValueRemoteConfigModule.kt] nhằm quản lý bật tắt toàn bộ các vị trí quảng cáo trong các màn Splash, Language, Onboarding:

*   `resume_open_app` (Boolean): Bật/Tắt quảng cáo AOA (App Open Ads) khi resume.
*   `banner_splash` (Boolean): Bật/Tắt banner ở màn Splash.
*   `inter_splash` & `inter_splash_high` (Boolean): Điều khiển Interstitial Splash.
*   `native_language` & `native_language_click` (Boolean): Cấu hình hiển thị Native màn Chọn Ngôn Ngữ.
*   `native_onboarding_1` đến `4` (Boolean): Bật/Tắt Native ad trên từng slide Onboarding.
*   `forceUpdate`, `showUpdate`, `lastVersionCode` (Boolean/Long): Cấu hình kiểm tra cập nhật ứng dụng tự động.

### 4.2 Tự định nghĩa Remote Config cho riêng App của bạn
Chỉ cần tạo một `object` kế thừa từ `KonfigModel` và định nghĩa các biến bằng delegate `by konfig(key, defaultValue)`:

```kotlin
import com.mobi.libraryads.commons.remote.KonfigModel
import com.mobi.libraryads.commons.remote.konfig

object RemoteConfigValueApp : KonfigModel {
    // Kiểu dữ liệu Boolean
    val banner_home by konfig("banner_home", true)
    val native_detail by konfig("native_detail", true)
    val inter_export_video by konfig("inter_export_video", true)

    // Kiểu dữ liệu String
    val url_more_app by konfig("url_more_app", "https://play.google.com/store/apps")

    // Kiểu dữ liệu Int / Long / Double
    val inter_capping_time by konfig("inter_capping_time", 30_000L)
}
```
*Lưu ý: Truyền `RemoteConfigValueApp` vào constructor `AdsApplication` ở bước khởi tạo.*

---

## 5. Luồng Khởi Động Tự Động (Splash -> Language -> Onboarding)

> [!IMPORTANT]
> Thư viện tích hợp sẵn bộ khung quản lý Splash screen, Chọn Ngôn ngữ (Language Selection) và Giới thiệu (Onboarding) rất chuyên nghiệp. Ứng dụng chỉ cần kế thừa các Activity có sẵn dưới đây.

### 5.1 Kế thừa [SplashActivity]
Tạo `SplashActivity` trong app của bạn kế thừa `SplashActivity` và triển khai 3 hàm cung cấp cấu hình:

```kotlin


class SplashActivity : SplashActivity() {

    // 1. Cấu hình màn Splash và Ads đi kèm
    override fun provideSplashConfig(): SplashConfig {
        return SplashConfig(
            uiSplashConfig = UiSplashConfig(
                resLayout = R.layout.activity_splash_screen, // Layout Splash tùy biến của App
                homeActivity = MainActivity::class.java,     // Activity đích sau khi kết thúc luồng onboarding
                showFOForever = false,                       // true nếu muốn luôn luôn hiện màn Language khi mở app
                timeout = 30_000,                            // Timeout tối đa chờ tải Ads (mili-giây)
                activityCallBack = object : OnActivityCallBack {
                    override fun onStartLoadAds(inSession2: Boolean) {
                        // Tải trước quảng cáo cho màn Home/Main khi bắt đầu vào Splash
                        preloadHomeAds()
                    }
                }
            ),
            adsSplashConfig = AdsSplashConfig(
                bannerId = getString(R.string.banner_splash),
                interHighId = getString(R.string.inter_splash_high),
                interAllId = getString(R.string.inter_splash),
                nativeFullId = "",
                nativeFullHighId = "",
                nativeFullLayout = R.layout.admob_layout_native_full,
                loadStrategy = LoadStrategy.PARALLEL, // Tải native full splash song song hoặc tuần tự
                admobAOAId = getString(R.string.resume_open_app) // ID quảng cáo App Open Ads
            )
        )
    }

    // 2. Cấu hình màn chọn Ngôn Ngữ
    override fun provideLanguageConfig(): LanguageConfig {
        return LanguageConfig(
            uiLanguageConfig = UiLanguageConfig(
                resLayout = R.layout.activity_language, // Layout XML chứa rcvLanguage, btnDone, layoutAds
                itemLangDefault = R.layout.item_language_default,
                itemLangSelected = R.layout.item_language_checked,
                listLanguage = getSupportedLanguages(), // Danh sách ngôn ngữ hỗ trợ
                languageSetting = object : LanguageSetting {
                    override fun onDone() {
                        // Gọi khi người dùng đổi ngôn ngữ từ màn Setting
                        recreateApp()
                    }
                }
            ),
            adsLanguageConfig = AdsLanguageConfig(
                nativeLangHighId = getString(R.string.native_language_high),
                nativeLangId = getString(R.string.native_language),
                nativeLangClickHighId = getString(R.string.native_language_high_alt),
                nativeLangClickId = getString(R.string.native_language_alt),
                layoutNative = R.layout.admob_layout_native_language_normal,
                layoutNativeClick = R.layout.admob_layout_native_language_click,
                loadStrategy = LoadStrategy.PARALLEL
            )
        )
    }

    // 3. Cấu hình màn Onboarding (Giới thiệu)
    override fun provideOnboardConfig(): OBConfig {
        return OBConfig(
            uiOBConfig = UiOBConfig(
                resLayout = R.layout.activity_onboarding, // Layout chứa viewPager2
                resFragmentOB1 = R.layout.fragment_onboarding_1,
                resFragmentOB2 = R.layout.fragment_onboarding_2,
                resFragmentOB3 = R.layout.fragment_onboarding_3,
                resFragmentOB4 = R.layout.fragment_onboarding_4, // Truyền trùng OB3 nếu chỉ có 3 slide
                resFragmentOBAdFull = R.layout.fragment_onboarding_ad_full_layout,
                nextOBActivity = MainActivity::class.java
            ),
            adsOBConfig = AdsOBConfig(
                nativeOB1Id = getString(R.string.native_onboarding_1),
                nativeOB3Id = getString(R.string.native_onboarding_3),
                nativeOBFull1Id = getString(R.string.native_onboarding_full_1),
                nativeOBFull2Id = getString(R.string.native_onboarding_full_2),
                layoutNativeOB1 = R.layout.admob_layout_native_medium,
                layoutNativeOB3 = R.layout.admob_layout_native_medium,
                layoutNativeFullOB = R.layout.admob_layout_native_full,
                isLoadNativeOBInLanguage = false,
                nativeFullInterOBLayout = R.layout.fragment_onboarding_ad_full_layout
            )
        )
    }
}
```

### 5.2 Giao diện chọn ngôn ngữ ([LanguageActivity.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/views/language/LanguageActivity.kt))
Thư viện tự động điều hướng sang `LanguageActivity`. Trong layout XML tùy biến của bạn (`uiLanguageConfig.resLayout`), hãy thiết lập các ID tương ứng:
*   `rcvLanguage` (`RecyclerView`): Hiển thị danh sách ngôn ngữ.
*   `btnDone` (`View`): Nút xác nhận ngôn ngữ.
*   `progressLoading` (`View`): Loading khi bấm chọn ngôn ngữ.
*   `layoutAds` (`FrameLayout`): Khung hiển thị Native Ad.
*   `btnBack` (`View`): Nút quay lại (chỉ hiển thị khi đi từ màn hình Settings của App qua intent `Constants.FROM_SETTING = true`).

### 5.3 Giao diện giới thiệu ([OnboardingActivity.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/views/onboarding/OnboardingActivity.kt))
Thư viện tự động quản lý chuyển slide bằng `ViewPager2`. Layout XML `uiOBConfig.resLayout` bắt buộc phải chứa một `ViewPager2` với ID `viewPager2`. 

---

## 6. Các API Tích Hợp Quảng Cáo (Ad APIs Reference)

### 6.1 Banner Ads ([Banner.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/ads/banner_ads/Banner.kt))
Hỗ trợ hiển thị Banner thường hoặc Banner dạng cuộn thu gọn (Collapsible Banner):

```kotlin
import com.mobi.libraryads.ads.banner_ads.Banner

Banner.requestBanner(
    activity = this,
    id = getString(R.string.banner_home),
    typeAds = Banner.TypeAds.BANNER_NORMAL, // Hoặc BANNER_COLLAPSIBLE_BOTTOM / BANNER_COLLAPSIBLE_TOP
    adFrame = viewBinding.layoutAdsBottom,       // FrameLayout chứa Banner
    canShowAd = RemoteConfigValueApp.banner_home // Biến Remote Config điều khiển
)
```

> **Mẹo Shimmer cho Banner**: Đặt thẻ `<include layout="@layout/layout_shimmer_load_ads_native_banner" />` trực tiếp bên trong `layoutAdsBottom` trong XML để shimmer hiển thị ngay khi mở màn hình. Thư viện sẽ tự động quản lý tắt/bật shimmer tương ứng.

### 6.2 Interstitial Ads ([Inter.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/ads/inter_ads/Inter.kt))
Tích hợp cơ chế tự động tải trước và giới hạn tần suất hiển thị (Capping Time) thông qua biến `ad_full_capping_time` cấu hình trên Remote Config (mặc định trễ 20s giữa các lần show):

**Tải trước (Preload) quảng cáo:**
```kotlin
import com.mobi.libraryads.ads.inter_ads.Inter
import com.mobi.libraryads.ads.inter_ads.InterAdModel

Inter.preLoadInter(
    activity = this,
    adModel = InterAdModel(
        name = "inter_save_file",           // Định danh/Key duy nhất cho vị trí Ad
        id = getString(R.string.inter_save)  // ID ad unit từ AdMob
    ),
    canShowAd = RemoteConfigValueApp.inter_save
)
```

**Hiển thị (Show) quảng cáo:**
```kotlin
Inter.showInter(
    activity = this,
    adName = "inter_save_file",
    nextAction = { onDismiss ->
        // Khởi chạy hành động tiếp theo sau khi tắt Ad hoặc ad load lỗi
        saveDataAndGoToNextScreen()
    },
    onShown = {
        // Gọi ngay khi ad hiển thị lên màn hình (Ẩn loading...)
    },
    preload = true, // Tự động load lại đợt tiếp theo sau khi tắt
    canShowAd = RemoteConfigValueApp.inter_save
)
```

### 6.3 Native Ads ([NativeManager.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/ads/native_ads/NativeManager.kt))
Quản lý thông minh hệ thống cache Native Ads bằng HashMap, hỗ trợ tự động xoay vòng hoặc thêm đếm ngược thời gian tắt ad (Spam Ads).

**Preload quảng cáo thường hoặc high floor:**
```kotlin
import com.mobi.libraryads.ads.native_ads.NativeManager
import com.mobi.libraryads.ads.native_ads.model.LoadStrategy

// Preload 1 ID thường
NativeManager.preloadNative(
    context = this,
    adName = "native_home",
    adId = getString(R.string.native_home_id),
    canShowAd = RemoteConfigValueApp.native_home
)

// Preload phân tầng High Floor / All Price
NativeManager.preloadNativeWithHigh(
    context = this,
    adName = "native_home",
    adId = getString(R.string.native_home_id),
    idHigh = getString(R.string.native_home_high_id),
    strategy = LoadStrategy.PARALLEL,
    canShowIdAll = true,
    canShowIdHigh = true
)
```

**Hiển thị Native Ad tiêu chuẩn:**
```kotlin
NativeManager.showNative(
    adFrame = viewBinding.layoutAdsNative,       // FrameLayout chứa ad
    adName = "native_home",                     // Tên ad đã preload
    adLayout = R.layout.admob_layout_native,     // Custom Native Layout XML
    isPreload = true,                            // Tự động load bù ad mới sau khi show
    canShowAd = RemoteConfigValueApp.native_home
)
```

> **Chuẩn Shimmer Cho Native**: Đặt thẻ `<include layout="@layout/layout_shimmer_load_ads_native_banner" />` bên trong `layoutAdsNative` tại XML màn hình. Thư viện sẽ tự kích hoạt shimmer trong lúc tải, và tự động tắt shimmer khi Native Ad hiển thị (hoặc khi ad bị lỗi/tắt).


**Hiển thị Native Collapsible tự động xoay vòng (Carousel):**
```kotlin
NativeManager.showCollapsibleNative(
    activity = this,
    adFrame = viewBinding.layoutNativeCollap,
    adName = "native_home",
    adLayout = R.layout.admob_layout_native_collapsible,
    refreshTime = 30_000, // Tự refresh ad sau mỗi 30s
    isPreload = true,
    canShowAd = RemoteConfigValueApp.native_home
)
```

**Hiển thị Native Full màn hình đếm ngược (Spam Ads):**
```kotlin
NativeManager.showFullscreenNativeWithCountdown(
    activity = this,
    adName = "native_home",
    adLayoutRes = R.layout.layout_native_full,
    countdownSeconds = 3,  // Đếm ngược 3 giây trước khi hiện nút Next/Close
    isPreload = true,
    startCountdownImmediately = false, // false để chờ gọi hàm startCountdown sau
    onDismissed = {
        // Chạy khi tắt ad
    }
)

// Gọi khi muốn kích hoạt chạy countdown đếm ngược
NativeManager.startFullscreenCountdown("native_home")
```

### 6.4 Reward Ads ([Reward.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/ads/reward_ads/Reward.kt))
Sử dụng để thưởng cho người dùng khi xem hết video (Ví dụ: Mở khóa tính năng VIP tạm thời):

```kotlin
import com.mobi.libraryads.ads.reward_ads.Reward

// Tải ad
Reward.loadRewardAd(
    activity = this,
    rewardId = getString(R.string.reward_ad_id)
)

// Hiển thị ad
Reward.showRewardAd(
    activity = this,
    nextAction = {
        // Chạy sau khi đóng ad hoặc khi lỗi xảy ra
    },
    onShowSuccess = {
        // Gọi khi show thành công
    },
    reload = true, // Tải lại ad khác ngay sau khi đóng
    onUserEarnedReward = {
        // Người dùng đã xem hết video, thực hiện trao thưởng tại đây
        unlockVipFeature()
    }
)
```

### 6.5 App Open Ads AOA ([OpenAds.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/ads/open_ads/OpenAds.kt))
Thư viện tự động lắng nghe vòng đời ứng dụng để hiển thị quảng cáo App Open Ads (AOA) khi người dùng mở lại app từ background. Để chặn AOA hiển thị ở các màn hình đặc thù (ví dụ: màn hình Mua hàng, màn hình đang tải file...):

```kotlin
import com.mobi.libraryads.ads.utils.StatusShowAd

// Tạm thời vô hiệu hóa AOA
StatusShowAd.ignoreAOA = true

// Bật lại tính năng AOA
StatusShowAd.ignoreAOA = false
```

---

## 7. Các Extension & Utility hữu ích ([ViewEx.kt](file:///D:/Mobi4/ads_lib/mobilibraryads/src/main/java/com/Mobi/libraryads/commons/utils/ViewEx.kt))

Thư viện tích hợp hàng loạt Extension Function thông minh giúp tối ưu hóa viết code trên Android:

### 7.1 Thay đổi Visibility của View an toàn
Tránh crash luồng Main Thread bằng các hàm hiển thị/ẩn view:
*   `view.setVisible()`: Đặt trạng thái `VISIBLE`.
*   `view.setInVisible()`: Đặt trạng thái `INVISIBLE`.
*   `view.setGone()`: Đặt trạng thái `GONE`.

### 7.2 Chống Click Spam (Debouncer)
Chống spam nút bấm làm crash hoặc mở nhiều màn hình trùng nhau:
*   `view.clickOnce(threshold = 500L) { ... }`: Không cho click liên tục, trễ tối thiểu 500ms.
*   `view.click(delay = 300L) { ... }`: Sử dụng Custom TapListener bảo vệ sự kiện click.

### 7.3 Hiệu ứng Collapse / Expand View
Thu/Phóng View kèm animation trơn tru:
*   `view.expand(duration = 300)`
*   `view.collapse(duration = 300) { /* callback khi kết thúc */ }`

### 7.4 Điều hướng màn hình (Navigation Extensions)
Mở Activity kèm cờ dọn dẹp task nhanh chóng:
*   `context.openActivity(TargetActivity::class.java) { putString("key", "value") }`
*   `context.openActivityAndClearApp(TargetActivity::class.java)`
*   `context.openActivityWithClearTask(TargetActivity::class.java)`
*   `context.openActivityClearTop(TargetActivity::class.java)`

### 7.5 Hiển thị Toast & Keyboard
*   `context.showToastShort("Message")` / `context.showToastLong("Message")`
*   `context.showToastOnceEvery15Seconds("Message")` (Tránh spam thông báo liên tiếp)
*   `activity.showKeyboardOnView(view)` / `activity.hideKeyboardEdt()`

### 7.6 Kiểm tra cập nhật ứng dụng tự động (`checkShowUpdate`)
Tự động hiển thị Dialog gợi ý/bắt buộc cập nhật dựa trên cấu hình Firebase Remote Config:
```kotlin
activity.checkShowUpdate(
    onRemind = {
        // Chạy khi người dùng chọn nhắc sau
    },
    onUpdate = { isForce ->
        // Chạy khi người dùng đồng ý cập nhật (isForce = true nếu bắt buộc)
    },
    inForceUpdate = {
        // Xử lý khi bắt buộc cập nhật (ví dụ: khóa màn hình chính)
    }
)
```

### 7.7 Firebase Event Tracking (`FirebaseTracking`)
Gửi log sự kiện phân tích lên Firebase Analytics vô cùng ngắn gọn:
```kotlin
import com.mobi.libraryads.commons.firebasetracking.FirebaseTracking.postFirebaseEvent

// Bắn sự kiện đơn giản
"click_button_premium".postFirebaseEvent()

// Bắn sự kiện kèm Bundle chi tiết
val bundle = Bundle().apply {
    putString("position", "home_banner")
}
"ad_click_event".postFirebaseEvent(bundle)
```

### 7.8 Lấy thông tin Package & Chữ ký (Signing Key)
*   `context.getAppVersionCode()`: Lấy mã phiên bản App.
*   `context.getAppSigningMD5()` / `context.getAppSigningSha1()`: Tiện lợi khi lấy chữ ký ứng dụng để cấu hình Firebase/Google Console mà không cần chạy commandline.
*   `isInstalledFromGooglePlay(context)`: Kiểm tra người dùng tải app từ Google Play Store hay cài đặt file APK ngoài.
