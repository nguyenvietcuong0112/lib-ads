package com.mobi.libraryads.commons.firebasetracking

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
//import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.installations.FirebaseInstallations

object FirebaseTracking {

    private const val TAG_FIREBASE = "firebase_tag"

    /**
     *  Syntax:
     *      try {}
     *      catch(ex: Exception){
     *          ex.recordException("MainActivity > OnCreate > getList")
     *      }
     */

    fun Throwable.recordException(logTag: String) {
        try {
//            FirebaseCrashlytics.getInstance().log(logTag)
//            FirebaseCrashlytics.getInstance().recordException(this)
            Log.e(logTag, "recordException: ${this.message.toString()}")
        } catch (e: Exception) {
            Log.e(logTag, "recordException: ${this.message.toString()}")
        }
    }

    /**
     *  Syntax:
     *      EventsProvider.HOME_SCREEN.postFirebaseEvent()
     *      EventsProvider.START_BUTTON.postFirebaseEvent()
     */

    @SuppressLint("HardwareIds")
    fun getDeviceID(context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }
    fun String.postFirebaseEvent() {
        try {
            val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
            val bundle = Bundle().also {
                it.putString(this, this)
            }
            val safeEventName = this
                .replace(".", "_")
                .replace(Regex("[^a-zA-Z0-9_]"), "")
                .take(40) // Firebase giới hạn tên event tối đa 40 ký tự

            firebaseAnalytics.logEvent(safeEventName, bundle)
            Log.d(TAG_FIREBASE, "postFirebaseEvent:$safeEventName successfully sent")
        } catch (ex: Exception) {
            ex.recordException("post_event_crash > $this")
        }
    }

    fun String.postFirebaseEvent(bundle: Bundle? = null) {
        try {
            val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
            val safeEventName = this
                .replace(".", "_")
                .replace(Regex("[^a-zA-Z0-9_]"), "")
                .take(40) // Firebase giới hạn tên event tối đa 40 ký tự

            val eventBundle = bundle ?: Bundle().also {
                it.putString(safeEventName, safeEventName)  // Nếu không có bundle, chỉ đẩy sự kiện với tên sự kiện
            }
            firebaseAnalytics.logEvent(safeEventName, eventBundle)
            Log.d(TAG_FIREBASE, "postFirebaseEvent: $this successfully sent with bundle: $eventBundle")
        } catch (ex: Exception) {
            ex.recordException("post_event_crash > $this")
        }
    }

    fun getDeviceToken() {
        // Add this 'id' in firebase AB testing console as a testing device
        FirebaseInstallations.getInstance().getToken(false)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    Log.d(TAG_FIREBASE, "Installation auth token: " + task.result.token)
                } else {
                    Log.e(TAG_FIREBASE, "Unable to get Installation auth token")
                }
            }
    }


    fun postAdRevenueEvent(
        revenue: Double,
        adPlatform: String = "",
        adUnitId: String = "",
        adFormat: String = "",
        currency: String = "USD"
    ) {
        try {
            val firebaseAnalytics = Firebase.analytics
            val bundle = Bundle().apply {
                putString("ad_platform", adPlatform)
                putString("ad_unit_id", adUnitId)
                putString("ad_format", adFormat)
                putDouble(FirebaseAnalytics.Param.VALUE, revenue/1000000.0)
                putString(FirebaseAnalytics.Param.CURRENCY, currency)
            }

            firebaseAnalytics.logEvent("ad_impression", bundle)

            Log.d("firebase_tag", "Ad revenue tracked: $revenue $currency ($adFormat / $adPlatform)")
        } catch (ex: Exception) {
            ex.recordException("postAdRevenueEvent > $adPlatform > $adUnitId")
        }
    }
}