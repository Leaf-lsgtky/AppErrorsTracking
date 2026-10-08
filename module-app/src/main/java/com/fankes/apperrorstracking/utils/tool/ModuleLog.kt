package com.fankes.apperrorstracking.utils.tool

import android.util.Log
import io.github.libxposed.api.XposedInterface
import java.io.Serializable
import java.util.Collections
import java.util.LinkedList

/**
 * 模块日志工具类
 */
object ModuleLog {

    const val TAG = "AppErrorsTracking"

    @Volatile
    var xposedInterface: XposedInterface? = null

    private const val MAX_LOG_SIZE = 500
    val inMemoryLogs: MutableList<LogItem> = Collections.synchronizedList(LinkedList<LogItem>())

    data class LogItem(
        val priority: String,
        val msg: String,
        val throwable: Throwable? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : Serializable {
        override fun toString(): String {
            return "[$priority] $msg"
        }
    }

    private fun record(priority: String, msg: String, tr: Throwable? = null) {
        val item = LogItem(priority, msg, tr)
        synchronized(inMemoryLogs) {
            if (inMemoryLogs.size >= MAX_LOG_SIZE) {
                inMemoryLogs.removeAt(0)
            }
            inMemoryLogs.add(item)
        }
    }

    fun debug(msg: String) {
        record("D", msg)
        Log.d(TAG, msg)
        xposedInterface?.log(Log.DEBUG, TAG, msg)
    }

    fun info(msg: String) {
        record("I", msg)
        Log.i(TAG, msg)
        xposedInterface?.log(Log.INFO, TAG, msg)
    }

    fun warn(msg: String) {
        record("W", msg)
        Log.w(TAG, msg)
        xposedInterface?.log(Log.WARN, TAG, msg)
    }

    fun error(msg: String, tr: Throwable? = null) {
        record("E", msg, tr)
        if (tr != null) {
            Log.e(TAG, msg, tr)
            xposedInterface?.log(Log.ERROR, TAG, msg, tr)
        } else {
            Log.e(TAG, msg)
            xposedInterface?.log(Log.ERROR, TAG, msg)
        }
    }

    fun contents(logs: List<LogItem>): String = buildString {
        logs.forEach { item ->
            append("[${item.priority}] ${item.timestamp} ${item.msg}\n")
            item.throwable?.let { append(Log.getStackTraceString(it)).append("\n") }
        }
    }
}

