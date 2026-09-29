package com.mobi.libraryads.commons.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object InternetConnectionObserver {

    private lateinit var connectivityManager: ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val verifyChannel = Channel<Unit>(Channel.CONFLATED)

    private val _isConnected = MutableStateFlow<Boolean?>(null)
    val isConnected: StateFlow<Boolean?> = _isConnected.asStateFlow()
    private var delayJob: Job? = null

    fun init(context: Context) {

        if (::connectivityManager.isInitialized) return

        connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching {
            connectivityManager.registerNetworkCallback(
                request,
                networkCallback
            )
        }.onFailure {
            _isConnected.value = false
        }

        startVerifyLoop()
        refresh()
    }

    fun refresh() {
        delayJob?.cancel()

        delayJob = scope.launch {
            delay(100)
            verifyChannel.trySend(Unit)
        }
    }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                refresh()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                refresh()
            }

            override fun onLost(network: Network) {
                refresh()
            }
        }

    private fun startVerifyLoop() {

        scope.launch {
            for (ignored in verifyChannel) {
                val connected = runCatching {
                    val network = connectivityManager.activeNetwork
                    network != null && DoesNetworkHaveInternet.execute(network.socketFactory)
                }.getOrDefault(false)

                if (_isConnected.value != connected) {
                    _isConnected.value = connected
                }
            }
        }
    }
}