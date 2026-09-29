package com.example.appadslib

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.mobi.libraryads.views.base.BaseActivity
import com.example.appadslib.databinding.LayoutTestBannerBinding

class GuideActivity : BaseActivity<LayoutTestBannerBinding>() {
    override fun inflateVB(inflater: LayoutInflater): LayoutTestBannerBinding {
        return LayoutTestBannerBinding.inflate(inflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout_test_banner)

        setupWindow()


        findViewById<View>(R.id.ivClose).setOnClickListener {
            finish()
        }

        // Auto dismiss sau 10 giây
        window.decorView.postDelayed({

            if (!isFinishing &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ||
                        !isDestroyed)
            ) {
                finish()
            }

        }, 20_000L)
    }
    private fun setupWindow() {

        window.apply {

            // Quan trọng: loại bỏ background trắng của Window
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            setGravity(Gravity.TOP)

            addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )

            addFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )

            clearFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )

            attributes = attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.TOP
            }
        }

        setFinishOnTouchOutside(false)
    }
}