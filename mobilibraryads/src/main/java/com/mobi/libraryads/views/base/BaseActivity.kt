package com.mobi.libraryads.views.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.mobi.libraryads.AdsApplication
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.commons.utils.InternetConnectionObserver
import com.mobi.libraryads.commons.utils.checkShowUpdate
import com.mobi.libraryads.commons.utils.hideNavigationBar
import com.mobi.libraryads.commons.utils.hideStatusBar
import com.mobi.libraryads.views.dialogs.DialogRequestInternet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

abstract class BaseActivity<VB : ViewBinding> :
    AppCompatActivity() {

    protected lateinit var binding: VB

    val mHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    abstract fun inflateVB(inflater: LayoutInflater): VB

    companion object {
        var inForceUpdate = false
    }

    suspend fun awaitFetchConfig(timeout: Long = 6000): Boolean? {
        return withTimeoutOrNull(timeout.milliseconds) {
            AdsApplication.fetchConfigDone
                .filterNotNull()
                .first()
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        initWindow()
        fullScreenCall()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            awaitFetchConfig()
            checkShowUpdate {
                inForceUpdate = true
            }
        }

        preLoadData()

        binding = inflateVB(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                InternetConnectionObserver.isConnected.collect { connected ->
                    when (connected) {
                        true -> onConnected()
                        false -> onDisconnected()
                        null -> {} // chưa xác định
                    }
                }
            }
        }


        initView()

        if (!SPF(this).is_app_pro) {
            lifecycleScope.launch {
                loadAds()
//                delay(50.milliseconds)
                showAds()
            }
        }

        clickView()
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
        hideStatusBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            fullScreenImmersive(window)
        }
    }

    private fun fullScreenImmersive(window: Window?) {
        if (window != null) {
            fullScreenImmersive(window.decorView)
        }
    }

    private fun fullScreenImmersive(view: View) {
        val uiOptions =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        view.systemUiVisibility = uiOptions
    }

    override fun attachBaseContext(mContext: Context) {
        val currentLanguage = SPF(mContext).language_code_selected
        val resources = mContext.resources
        val locale = Locale(currentLanguage)
        locale.let {
            Locale.setDefault(it)
        }
        val config = resources.configuration
        config.setLocale(locale)
        val newContext = mContext.createConfigurationContext(config)
        super.attachBaseContext(newContext)
    }

    private var noInternetDialogShowing = false
    private fun showPopupNoInternet() {

        if (noInternetDialogShowing) return
        val fm = supportFragmentManager
        if (isFinishing || isDestroyed || fm.isStateSaved) return
        val oldFrag = supportFragmentManager.findFragmentByTag("DialogRequestInternet")
        if (oldFrag is DialogRequestInternet) return

        noInternetDialogShowing = true
        DialogRequestInternet.newInstance()
            .show(fm, "DialogRequestInternet")
    }

    private fun dismissPopupNoInternet() {
        (supportFragmentManager.findFragmentByTag("DialogRequestInternet") as? DialogFragment)
            ?.dismissAllowingStateLoss()
        noInternetDialogShowing = false
    }

    open fun onConnected() {
        dismissPopupNoInternet()
    }

    open fun onDisconnected() {
        showPopupNoInternet()
    }

    open fun preLoadData() {}
    open fun loadAds() {}
    open fun showAds() {}
    open fun initView() {}
    open fun clickView() {}

    open fun initWindow() {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.WHITE
            setBackgroundDrawable(Color.WHITE.toDrawable())
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    private fun fullScreenCall() {
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        decorView.systemUiVisibility = uiOptions
    }

}