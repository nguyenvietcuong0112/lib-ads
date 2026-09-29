package com.mobi.libraryads.commons.consents

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

class GoogleMobileAdsConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        val params = ConsentRequestParameters.Builder().build()
        try {
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            onConsentGatheringCompleteListener.consentGatheringComplete(formError)
                        }
                    } catch (_: Exception) {
                        onConsentGatheringCompleteListener.consentGatheringComplete(null)
                    }
                },
                { requestConsentError ->
                    onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
                }
            )
        } catch (_: Exception) {
            onConsentGatheringCompleteListener.consentGatheringComplete(null)
        }
    }

    companion object {
        @Volatile
        private var instance: GoogleMobileAdsConsentManager? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: GoogleMobileAdsConsentManager(context).also { instance = it }
                }
    }
}
