
![Logo](https://Mobi.com/wp-content/uploads/2018/09/Mobi-logo-header.png)


# Mobi lib ads, FO Android

Lib ads luồng First open, load ads, show ads và tracking cho các dự án của Android studio


ver 1.0.0

Quản lý các version: [[https://docs.google.com/spreadsheets/d/17m0WY3BJE7xDaPKwrCv26ikiwHxUJywovO-z49acpao/edit?gid=0#gid=0](https://docs.google.com/spreadsheets/d/1iWJ005Iiwl8VlvqYtpg3EZo9_P3V9caw5-2I9V1CUWM/edit?usp=sharing)](https://docs.google.com/spreadsheets/d/1iWJ005Iiwl8VlvqYtpg3EZo9_P3V9caw5-2I9V1CUWM/edit?usp=sharing)
## Cài đặt

Chú ý: Download đúng ver mới nhất để sử dụng

B1: Import thư viện
- Trong file `settings.gradle` (hoặc `settings.gradle.kts`), mục repositories của `dependencyResolutionManagement`, thêm (KHÔNG CẦN TOKEN / PASSWORD):
```groovy
repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://raw.githubusercontent.com/nguyenvietcuong0112/lib-ads/maven-repo/")
    }
}
```

- Trong `dependencies` của `build.gradle` app, thêm:
```groovy
dependencies {
    implementation("com.app:library-ads:0.0.1")
}
```
    
B2: Import các thành phần phụ thuộc và thư viện:
- Tạo firebase và thêm vào dự án (File google-services.json)
- Trong settings.gradle, mục repositories của dependencyResolutionManagement, thêm:

  ```bash
        maven { url 'https://jitpack.io' }

        maven {
            url 'https://android-sdk.is.com/'
        }
        maven {
            url 'https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea'
        }
        maven {
            url 'https://artifact.bytedance.com/repository/pangle/'
        }
  ```

- Trong file build.gradle của dự án, mục plugin thêm:
  ```bash
        id 'com.android.library' version '8.1.4' apply false
        alias(libs.plugins.google.gms.google.services) apply false
        alias(libs.plugins.google.firebase.crashlytics) apply false
  ```
*chú ý: trong libs.versions.toml, thêm vào plugins:
  ```bash
        google-gms-google-services = { id = "com.google.gms.google-services", version.ref = "googleGmsGoogleServices" }
        google-firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "googleFirebaseCrashlytics" }
  ```
với các phiên bản tương ứng:
  ```bash
        googleGmsGoogleServices = "4.4.3"
        googleFirebaseCrashlytics = "3.0.3"
  ```

- Nâng cấp gradle lên phiên bản:
  ```bash
        distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip
  ```
- Trong build.gradle của ứng dụng:



```bash
1. Thêm plugin vào:
          alias(libs.plugins.google.gms.google.services)
          alias(libs.plugins.google.firebase.crashlytics)

2. Sử dụng compile ver 17

          compileOptions {
            sourceCompatibility JavaVersion.VERSION_17
            targetCompatibility JavaVersion.VERSION_17
          }
          kotlinOptions {
            jvmTarget = '17'
          }

3. Thêm viewBinding và buildConfig:

          buildFeatures {
            dataBinding true
            viewBinding true
            buildConfig true
          }

4. Thêm vào dependencies các thư viện sau:

          //lib ads
          implementation "com.google.android.gms:play-services-ads:24.5.0"

          implementation platform('com.google.firebase:firebase-bom:34.0.0')
          implementation 'com.google.firebase:firebase-config'
          implementation 'com.google.firebase:firebase-analytics'
          implementation 'com.google.firebase:firebase-crashlytics'
//        implementation 'com.google.firebase:firebase-core:21.1.1' không dùng thằng core này vì có thể gây ra crash
          implementation 'com.google.firebase:firebase-messaging:25.0.0'

          implementation "com.airbnb.android:lottie:6.4.0"
          implementation 'com.facebook.shimmer:shimmer:0.5.0' //shimmer
          implementation 'androidx.fragment:fragment-ktx:1.8.8'
          implementation 'com.github.bumptech.glide:glide:4.16.0' //glide
          //consent UMP GDPR
          implementation("com.google.android.ump:user-messaging-platform:3.1.0")
          implementation 'com.google.code.gson:gson:2.11.0'
          implementation "com.squareup.okhttp3:okhttp:5.0.0-alpha.11"
          implementation "com.squareup.retrofit2:retrofit:2.9.0"
          implementation "com.squareup.retrofit2:converter-gson:3.0.0"

          //mediation
          implementation 'com.google.ads.mediation:applovin:13.3.1.1'
          implementation 'com.google.ads.mediation:inmobi:10.8.3.1'
          implementation 'com.google.ads.mediation:ironsource:8.10.0.0'
      //    implementation 'com.google.ads.mediation:vungle:7.5.0.0'
          implementation 'com.google.ads.mediation:facebook:6.20.0.0'
          implementation 'com.google.ads.mediation:mintegral:16.9.81.0'
          implementation 'com.google.ads.mediation:pangle:7.3.0.4.0'
          implementation 'com.unity3d.ads:unity-ads:4.16.0'
          implementation 'com.google.ads.mediation:unity:4.15.1.0'

          //lifecycle
          def lifecycle_version = "2.9.2"
          implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version"
          implementation "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version"
          implementation "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version"

          //adjust sdk
          implementation 'com.adjust.sdk:adjust-android:5.4.1'
          implementation 'com.android.installreferrer:installreferrer:2.2'
          // Add the following if you are using the Adjust SDK inside web views on your app
          implementation 'com.adjust.sdk:adjust-android-webbridge:5.4.1'

```


- Trong manifest của ứng dụng, thêm các thành phần sau:
```bash
        <!-- Sample AdMob app ID: ca-app-pub-3940256099942544~3347511713 -->
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
```

Chú ý: Trong manifest không để
```bash
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
```
trong bất cứ activity nào.

- Tạo Application cho app
- Khởi tạo và gắn biến cho lib trong Application:

```bash
      var adsLibrary: MobiLibraryApplication? = null

      override fun onCreate() {
        super.onCreate()

        initLibrary()

    }


    private fun initLibrary() {
        adsLibrary = MobiLibraryApplication(this, RemoteConfigValue, BuildConfig.DEBUG)

        val listStringUsePhone = enumValues<ItemUsePhoneEnum>().map {
            it.value
        }

        adsLibrary?.initLib(
            context = this,                            //context của app

            adjust_app_token = "",                     //app token của adjust

            gsmAppId = "",                             //id của gsm

            isDebug = BuildConfig.DEBUG,               //kiểu boolean, đang chạy debug hay release?
            versionName = BuildConfig.VERSION_NAME,    // Version name của app
            listLanguage = listOf<LanguageModel>(),    //list ngôn ngữ của app
            listCategoryNameQuestion = listOf<Int>(),  //list id string của màn question
            //có template sẵn trong project

            //init id admob
            //gắn id ads tương ứng vào đây, cái nào không dùng thì để rỗng
            //Kiểu dữ liệu String
            admob_id_inter_splash_high = "",           
            admob_id_inter_splash_allprice = "",
            admob_id_banner_splash = "",

            admob_id_native_language_high = "",
            admob_id_native_language_allprice = "",
            admob_id_native_language_alt_high = "",
            admob_id_native_language_alt_allprice = "",

            admob_id_native_onboarding_1 = "",
            admob_id_native_onboarding_full_1 = "",
            admob_id_native_onboarding_3 = "",
            admob_id_native_onboarding_full_3 = "",


            admob_id_native_question = "",
            admob_id_native_question_full = "",

            admob_id_open_ads = "",

            admob_id_inter_all = "",
            admob_id_native_all = "",
            admob_id_native_home = "",
            admob_id_banner_all = "",
            admob_id_reward_all = "",
            admob_id_banner_collapse_all = "",


            //init view FO
            //Truyền id layout các màn tương ứng theo template có sẵn. Chỉ thay đổi UI, không thay đổi id của các UI đó.
            //Cái gì quan trọng thì nhắc lại nhiều lần: Chú ý lấy layout, thay đổi ảnh, view tuỳ ý nhưng bắt buộc phải đúng ID các view trong template
            //cái gì quan trọng thì nhắc lại lần nữa: Chú ý lấy layout, thay đổi ảnh, view tuỳ ý nhưng bắt buộc phải đúng ID các view trong template. Nếu đổi id view sẽ bị crash app.
            //Kiểu dữ liệu Int (id của layout trong R.layout....)

            splash_activity_view = "",
            language_activity_view = "",
            language_item_view = "",
            onboarding_activity_view = "",

            fragment_onboarding_1 = "",
            fragment_onboarding_2 = "",
            fragment_onboarding_3 = "",
            question_activity_view = "",
            ad_full_onboard_layout = "",

            //Màu của chip tương ứng trong màn question. Có template, chỉ việc đổi màu.
            color_chip_background = "",
            color_chip_text = "",
            color_chip_stroke_color = "",

            //màu chủ đạo của app, dùng để thay đổi màu của các button trong luồng FO
            color_button_cta = "",

            //init activity start, thường vào main hoặc home
            mainActivity = MainActivity::class.java,

            //Sau màn onboarding cần custom màn gì(Giả sử custom lại question, permission hoặc,...)  thì điền vào đây. Không cần custom mà vẫn giữ nguyên luồng FO thì để null. 
            nextActivityAfterOnboarding = null,
            

            //Nếu có màn permission hoặc màn nào đó trước khi vào main thì điền vào đây. Nếu ko thì để null, nó sẽ nhảy vào Main.
            permissionActivity = null,

            )

    }


```



## Documentation

1. Load and show ads
   Note: Quản lý các Ad theo tên của ad. Cách tốt nhất là tạo 1 enum các ads để quản lý các ads đó 1 cách tốt nhất:
   Ví dụ enum:

```Bash
enum class EnumAdsNamePositionApp(val position: String) {
    NATIVE_PERMISSION("native_permission"),
    NATIVE_ALARM("native_alarm"),
    NATIVE_SETTING("native_setting"),
    INTER_BACK("inter_back"),
}
```
Tất cả các ad đều đã được tracking adjust, không cần tracking

- Native: Có 2 quá trình là note -> show.
```Bash
    1. PreLoad:
          NativeManager.preLoadNativeAd(
              context = this,
              adModel = NativeAdModel(
                  name = EnumAdsNamePosition.NATIVE_ALL_APP.position,
                  id = getString(R.string.admob_native_all)
              ),
              onResult = { it->
              //trả về trạng thái load được ads hay không. 
              //it = true : Load thành công ad,
              //It = false: Load fail ad
              }
          )

          Chú ý: truyền name ad vào là gì thì khi nào cần show thì lấy name ra như vậy.

    2. Show:
          NativeManager.showNativeAd(
                adFrame = viewBinding.layoutAds,
                adName = EnumAdsNamePosition.NATIVE_ALL_APP.position,
                onShowFail = {
                //show ad lỗi
                },
                onShown = {
                //show ad thành công
                },
                reloadNormalId = getString(R.string.admob_native_all),
                isPreload = true
          )

          Chú ý: 
          - Khi isPreload = true, ads sẽ được preload sau khi ad được show. Nếu truyền reloadNormalId thì sẽ preload theo id đó và name ad tương ứng với adName. Nếu không truyền reloadNormalId thì sẽ preload theo id của ad name. Trong trường hợp không lấy được thì sẽ load fail.
          - Ad đã được tracking adjust revenue nên không cần tracking lại.
          - adFrame sẽ có template riêng, không dùng các dạng khác tránh crash:

              <FrameLayout
                android:id="@+id/layoutAds"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_alignParentBottom="true">
                <include layout="@layout/admob_native_small_media_2" />
              </FrameLayout>

              chỉ thay đổi admob_native_small_media_2 theo các template có sẵn. Chú ý không xoá bất cứ id view nào để không bị crash. Để ẩn view đi thì dùng gone.


      3. Native inline recycleview
```

- Banner:
```kotlin
    // 1. Tải và hiển thị Banner (Normal, Adaptive, hoặc Collapsible):
    // Nên gọi hàm này trong onCreate() hoặc onResume().
    // LƯU Ý: Vì thư viện sử dụng một đối tượng quảng cáo tĩnh toàn cục (Singleton), 
    // khi bạn sang màn hình mới có tải Banner, Banner ở màn hình cũ sẽ bị hủy. 
    // Do đó, nếu bạn muốn khôi phục hoặc tải lại quảng cáo khi nhấn BACK quay lại màn hình cũ, hãy đặt lệnh này trong onResume().
    
    Banner.requestBanner(
        activity = this,
        id = getString(R.string.admob_banner_id),
        typeAds = TypeAds.BANNER_ADAPTIVE, // Các loại: BANNER_NORMAL, BANNER_ADAPTIVE, BANNER_COLLAPSIBLE_BOTTOM, BANNER_COLLAPSIBLE_TOP
        adFrame = viewBinding.layoutAdsBottom, // Khung chứa FrameLayout
        onResult = { adView ->
            if (adView != null) {
                // Tải quảng cáo thành công
            } else {
                // Tải quảng cáo thất bại (sau tất cả các lượt thử lại/retry)
            }
        },
        onShown = {
            // Ghi nhận lượt hiển thị quảng cáo (Impression) thành công
        }
    )

    // Khung giao diện FrameLayout khai báo trong XML:
    <FrameLayout
        android:id="@+id/layoutAdsBottom"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        />

    // 2. Tiêu hủy Banner và giải phóng bộ nhớ:
    // Thư viện đã TỰ ĐỘNG lắng nghe Lifecycle và dọn dẹp hoàn toàn quảng cáo khi Activity bị hủy (ON_DESTROY).
    // Nếu bạn muốn chủ động tiêu hủy thủ công vào thời điểm khác, hãy gọi:
    Banner.destroyBannerAds(activity = this)
```
- Inter:
```Bash
    1. preload

          Inter.loadInterAd(
            activity = this,
            adModel = InterAdModel(
                name = EnumAdsNamePosition.INTER_ALL_APP.position,
                id = getString(R.string.admob_inter_all)
            )
        )

    2. Show:

          Inter.showInterAd(
            activity = this,
            adName = EnumAdsNamePosition.INTER_ALL_APP.position,
            nextAction = {
                
            },
            onShown = {

            },
            preload = true
          )



```
- Reward:

```Bash
    1. Preload:
          Reward.loadRewardAd(
            activity = this,
            rewardId = getString(R.string.admob_reward)
        )

    2. Show:

          Reward.showRewardAd(
            activity = this@SelectRingtoneActivity,
            nextAction = {
                  
            },
            onShowSuccess = {

            },
            reload = false
          )


```
- Open app: tự động show khi init lib trong application

2. Common
- Adjust tracking:
```Bash
        1. Tracking event:

            trackingEvent(tokenEvent: String)


            - truyền token event vào để tracking

        2. Tracking revenue (tất cả các ads đã được tracking)
```
- Firebase tracking:

```Bash
        1. Tracking event:

            "String".postFirebaseEvent()

            "String".postFirebaseEvent(bundle: Bundle)

            Ví dụ: "main_view".postFirebaseEvent()

        2. Tracking revenue:

            postAdRevenueEvent(
              revenue: Double,
              adPlatform: String = "",
              adUnitId: String = "",
              adFormat: String = "",
              currency: String = "USD"
            )

```
- Remote:
```Bash
        - Hiện tại đang dùng thư viện ngoài. Đã có những remote mặc định, cần thêm thì thêm vào template RemoteConfigValue (đã có ví dụ)
        - Các remote có sẵn trong lib:
            show_AOA, inter_splash_high, inter_splash_all_price, native_language, native_onboarding_1, native_onboarding_full, native_onboarding_3, native_question, native_full_question, banner_splash, ad_full_capping_time
```
- SharePreferences:

```Bash
      1. Để sử dụng, dùng:
        set: SPF(context).is_app_pro = true //giả sử gắn giá trị = true khi user mua hàng.
        get: SPF(context).is_app_pro // get giá trị về để sử dụng giả sử: if(SPF(context).is_app_pro) {....}

      2. Các giá trị có sẵn trong lib:

      is_app_pro: Boolean - Quy định app sẽ ở trạng thái premium hay không
      language_code_selected: String - Lưu lại mã code ngôn ngữ sử dụng của ứng dụng. Ví dụ: en, vi,...
      is_second_time_open_app: Boolean - Quy định lần mở app thứ mấy? lần thứ 2 trở đi sẽ là true
      is_click_native_collapsible: Hàm này để lắng nghe sự kiện click vào native collap. Không nên dùng.
```
- Utils:
```Bash
    Trong này có rất nhiều hàm, nhưng có 1 vài hay dùng và thường xuyên dùng:

    1. Context.dpToPx(dp: Float): Int => Convert dp to px 
    2. View.setVisible() => để set visible, hiển thị view
    3. View.setInVisible() => Ẩn view tại vị trí
    4. View.setGone() => Ẩn hẳn view đi
    5. Context.isInternetConnected() : Boolean => Trả về trạng thái kết nối internet của điện thoại ngay lúc gọi.
    6. Activity.hideStatusBar(): Ẩn app dưới statusbar 
    7. Context.openActivity(it: Class<T>, extras: Bundle.() -> Unit = {}) => Mở 1 activity. Ví dụ cách dùng: 

          openActivity(LanguageActivity::class.java) {
            putBoolean("FROM_MENU", true)
          }


    8. Context.showToastLong(msg: String) => Show toast, tương tự có hàm showToastOnceEvery15Seconds, showToastShort

    9. AppCompatActivity.addOnBackPressedDispatcher(onBackPressed: () -> Unit = { finish() }) => sử dụng trong activity để thay thế onBackPress 

    10. View.clickOnce(threshold: Long = 500L, action: () -> Unit) => Thực hiện onclick của view để tránh double click, thích delay thời gian bao nhiêu thì truyền vào


    11. DialogFragment.showAllowingStateLoss(manager: FragmentManager, tag: String?) => Dùng để show dialog, tránh 1 vài trường hợp crash.

```



## License

- [Mobi](https://Mobi.com/privacy-policy/)
- [ThanhLV](https://Mobi.com/privacy-policy/)
- [SonBV](https://github.com/sonbui210)

