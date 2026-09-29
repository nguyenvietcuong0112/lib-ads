package com.mobi.libraryads.commons.utils

import android.net.ConnectivityManager
import android.net.Network

class MyNetworkCallback(
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit
) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        onConnected.invoke() // Gọi hàm khi có kết nối
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        // Có thể xử lý mất mạng tại đây nếu cần
        onDisconnected.invoke()
    }
}