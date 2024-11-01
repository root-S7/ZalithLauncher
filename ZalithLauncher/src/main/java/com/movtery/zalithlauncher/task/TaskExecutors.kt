package com.movtery.zalithlauncher.task

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TaskExecutors {
    companion object {
        private val defaultExecutors: ExecutorService = ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        private val uiHandler = Handler(Looper.getMainLooper())

        fun getDefault(): ExecutorService {
            return defaultExecutors
        }

        fun getAndroidUI(): Executor {
            return Executor { r: Runnable -> uiHandler.post(r) }
        }

        fun runInUIThread(runnable: Runnable) {
            getAndroidUI().execute { runnable.run() }
        }
    }
}