package com.mobi.libraryads.commons.fcm

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FCMService : FirebaseMessagingService() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val notification = message.notification ?: return
        val title = notification.title
        val messageBody = notification.body

//        NotificationHelper(applicationContext).showNotification(title, messageBody, "HOME_ACTION_FROM_NOTI_FCM")
    }

    override fun onNewToken(token: String) {
        println("App FCM token ================= " + token)
        super.onNewToken(token)
    }
}