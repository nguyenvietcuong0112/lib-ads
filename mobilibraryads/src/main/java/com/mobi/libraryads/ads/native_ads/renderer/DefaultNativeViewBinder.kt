package com.mobi.libraryads.ads.native_ads.renderer

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.mobi.libraryads.R
import com.mobi.libraryads.commons.utils.setGone
import com.mobi.libraryads.commons.utils.setInVisible
import com.mobi.libraryads.commons.utils.setVisible
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

class DefaultNativeViewBinder : INativeViewBinder {
    override fun canHandle(adapterClassName: String): Boolean = true  // Fallback binder

    override fun bind(adView: NativeAdView, ad: NativeAd) {
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)

        // MediaView
        val mediaView: MediaView? = adView.findViewById(R.id.media_view)
        if (mediaView != null) {
            try {
                ad.mediaContent?.let { mediaContent ->
                    mediaView.mediaContent = mediaContent
                    mediaView.setBackgroundColor(Color.TRANSPARENT)
                    mediaView.setVisible()
                } ?: run {
                    mediaView.setInVisible()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mediaView.setInVisible()
            }
        }

        // Headline
        adView.headlineView?.let { headline ->
            (headline as? TextView)?.text = ad.headline
            headline.isSelected = true
        }

        // Body
        adView.bodyView?.let { bodyView ->
            if (ad.body == null) {
                bodyView.visibility = View.INVISIBLE
            } else {
                bodyView.setVisible()
                (bodyView as? TextView)?.text = ad.body
            }
        }

        // Call to Action
        adView.callToActionView?.let { ctaView ->
            if (ad.callToAction != null) {
                (ctaView as? TextView)?.text = ad.callToAction
            }
        }

        // Icon
        adView.iconView?.let { iconView ->
            val parentCard = iconView.parent as? View
            if (ad.icon != null) {
                parentCard?.setVisible()
                iconView.setVisible()
                (iconView as? ImageView)?.setImageDrawable(ad.icon?.drawable)
            } else {
                parentCard?.setInVisible()
                iconView.setInVisible()
            }
        }

        // Advertiser
        adView.advertiserView?.let { advertiserView ->
            if (ad.advertiser != null) {
                (advertiserView as? TextView)?.text = ad.advertiser
            }
        }

        adView.registerNativeAd(ad, mediaView)

        // Show/hide content and shimmer containers if present
        val adContent: ViewGroup? = adView.findViewById(R.id.ad_content_view)
        adContent?.setVisible()
        val shimmer: View? = adView.findViewById(R.id.shimmer_view)
        shimmer?.setGone()
    }
}
