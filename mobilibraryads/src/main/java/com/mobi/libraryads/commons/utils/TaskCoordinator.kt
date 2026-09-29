package com.mobi.libraryads.commons.utils

class TaskCoordinator(
    private val onAllDone: () -> Unit
) {
    private var needTaskB = false
    private var taskADone = false
    private var taskBDone = false

    fun requireTaskB() {
        needTaskB = true
    }

    fun onTaskADone() {
        taskADone = true
        check()
    }

    fun onTaskBDone() {
        taskBDone = true
        check()
    }
    @Synchronized
    private fun check() {
        if (!taskADone) return

        if (!needTaskB || taskBDone) {
            onAllDone()
        }
    }
}