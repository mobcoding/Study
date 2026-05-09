package com.zero.base.util

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object ThreadPool {
    private const val CORE_POOL_SIZE = 5
    private const val MAX_POOL_SIZE = 10
    private const val KEEP_ALIVE_TIME = 5000

    private val THREAD_POOL_EXECUTOR: ThreadPoolExecutor

    init {
        THREAD_POOL_EXECUTOR = ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME.toLong(), TimeUnit.MILLISECONDS, LinkedBlockingQueue(), NamedThreadFactory())
    }

    fun execute(runnable: Runnable?) {
        THREAD_POOL_EXECUTOR.execute(runnable)
    }

    private class NamedThreadFactory : ThreadFactory {
        private val atomicInteger = AtomicInteger(0)

        override fun newThread(runnable: Runnable): Thread {
            atomicInteger.incrementAndGet()
            val thread = Thread(runnable)
            thread.name = "Z-THREAD-POOL-$atomicInteger"
            thread.isDaemon = false
            return thread
        }
    }
}
