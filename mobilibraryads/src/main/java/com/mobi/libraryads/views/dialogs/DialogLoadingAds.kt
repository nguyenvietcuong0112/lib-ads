package com.mobi.libraryads.views.dialogs

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import com.mobi.libraryads.FOConfigs

class DialogLoadingAds(activity: Activity) : Dialog(activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(FOConfigs.splashConfig.adsSplashConfig.loadingAdFullLayout)
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    companion object {
        fun showLoading(activity: Activity?): DialogLoadingAds? {
            if (activity == null || activity.isFinishing || activity.isDestroyed) return null
            return try {
                val dialog = DialogLoadingAds(activity)
                dialog.show()
                dialog
            } catch (_: Exception) {
                null
            }
        }

        fun dismissLoading(dialog: DialogLoadingAds?, activity: Activity?) {
            if (dialog != null && dialog.isShowing) {
                try {
                    if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                        dialog.dismiss()
                    } else {
                        dialog.dismiss()
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
