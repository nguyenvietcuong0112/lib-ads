package com.mobi.libraryads.commons.GSM;

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object RunUtils {
    private var handler: Handler? = null
    private const val NUM_OF_THREADS = 20
    private var executor: Executor? = null

    init {
        createExecutor()
    }

    private fun createExecutor() {
        executor = Executors.newFixedThreadPool(
            NUM_OF_THREADS
        ) { r: Runnable? ->
            val th = Thread(r)
            th.name = "Background Thread"
            th
        }
    }

    fun runOnUI(runnable: Runnable?) {
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }
        handler!!.post(runnable!!)
    }

    fun runInBackground(runnable: Runnable) {
        if (isMain) {
            executor!!.execute(runnable)
        } else {
            runnable.run()
        }
    }

    private val isMain: Boolean
        get() = Looper.myLooper() == Looper.getMainLooper()
}