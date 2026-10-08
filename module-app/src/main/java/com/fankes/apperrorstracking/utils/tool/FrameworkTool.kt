/*
 * AppErrorsTracking - Added more features to app's crash dialog, fixed custom rom deleted dialog, the best experience to Android developer.
 * Copyright (C) 2017 Fankes Studio(qzmmcn@163.com)
 * https://github.com/KitsunePie/AppErrorsTracking
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2022/5/12.
 */
package com.fankes.apperrorstracking.utils.tool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.bean.AppErrorsInfoBean
import com.fankes.apperrorstracking.bean.AppFiltersBean
import com.fankes.apperrorstracking.bean.AppInfoBean
import com.fankes.apperrorstracking.bean.MutedErrorsAppBean
import com.fankes.apperrorstracking.utils.factory.execShell
import com.fankes.apperrorstracking.utils.factory.isRootAccess
import com.fankes.apperrorstracking.utils.factory.newThread
import com.fankes.apperrorstracking.utils.factory.showDialog
import com.fankes.apperrorstracking.utils.factory.toEntityOrNull
import com.fankes.apperrorstracking.utils.factory.toJsonOrNull
import com.fankes.apperrorstracking.wrapper.BuildConfigWrapper
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 系统框架控制工具
 */
object FrameworkTool {

    const val ACTION_APP_TO_SYSTEM = "com.fankes.apperrorstracking.action.TO_SYSTEM"
    const val ACTION_SYSTEM_TO_APP = "com.fankes.apperrorstracking.action.TO_APP"

    private const val EXTRA_CMD = "extra_cmd"
    private const val EXTRA_REQ_ID = "extra_req_id"
    private const val EXTRA_DATA = "extra_data"
    private const val EXTRA_CHUNK_INDEX = "extra_chunk_index"
    private const val EXTRA_CHUNK_TOTAL = "extra_chunk_total"

    private const val CHUNK_SIZE = 8 * 1024 // 8 KB chars = 16 KB bytes (ultra-safe for BroadcastQueue parcel limit)

    private const val CMD_PING = "cmd_ping"
    private const val CMD_REFRESH_PREFS = "cmd_refresh_prefs"
    private const val CMD_OPEN_APP = "cmd_open_app"
    private const val CMD_GET_APP_ERROR_DATA = "cmd_get_app_error_data"
    private const val CMD_GET_APP_ERRORS_DATA = "cmd_get_app_errors_data"
    private const val CMD_GET_APP_ERRORS_DATA_FULL = "cmd_get_app_errors_data_full"
    private const val CMD_REMOVE_APP_ERRORS_DATA = "cmd_remove_app_errors_data"
    private const val CMD_CLEAR_APP_ERRORS_DATA = "cmd_clear_app_errors_data"
    private const val CMD_MUTE_APP_UNLOCK = "cmd_mute_app_unlock"
    private const val CMD_MUTE_APP_RESTART = "cmd_mute_app_restart"
    private const val CMD_GET_MUTED_APPS = "cmd_get_muted_apps"
    private const val CMD_UNMUTE_APP = "cmd_unmute_app"
    private const val CMD_UNMUTE_ALL = "cmd_unmute_all"
    private const val CMD_GET_APP_LIST = "cmd_get_app_list"
    private const val CMD_GET_LOGS = "cmd_get_logs"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = ConcurrentHashMap<String, (data: String?) -> Unit>()
    private val pendingChunks = ConcurrentHashMap<String, Array<String?>>()

    @Volatile
    private var isAppReceiverRegistered = false

    private fun dispatchReplyIntent(context: Context, replyIntent: Intent) {
        val userHandleClass = runCatching { Class.forName("android.os.UserHandle") }.getOrNull()
        val allUser = userHandleClass?.getField("ALL")?.get(null)
        val sent = runCatching {
            context.javaClass.getMethod("sendBroadcastAsUser", Intent::class.java, userHandleClass!!)
                .invoke(context, replyIntent, allUser)
            true
        }.getOrElse { false }
        if (!sent) {
            context.sendBroadcast(replyIntent)
        }
    }

    private fun sendReply(context: Context, cmd: String, reqId: String, replyData: String?) {
        if (replyData == null || replyData.length <= CHUNK_SIZE) {
            val replyIntent = Intent(ACTION_SYSTEM_TO_APP).apply {
                setPackage(BuildConfigWrapper.APPLICATION_ID)
                putExtra(EXTRA_CMD, cmd)
                putExtra(EXTRA_REQ_ID, reqId)
                putExtra(EXTRA_CHUNK_INDEX, 0)
                putExtra(EXTRA_CHUNK_TOTAL, 1)
                replyData?.let { putExtra(EXTRA_DATA, it) }
            }
            dispatchReplyIntent(context, replyIntent)
            return
        }

        val totalChunks = (replyData.length + CHUNK_SIZE - 1) / CHUNK_SIZE
        for (i in 0 until totalChunks) {
            val start = i * CHUNK_SIZE
            val end = minOf(start + CHUNK_SIZE, replyData.length)
            val chunk = replyData.substring(start, end)
            val replyIntent = Intent(ACTION_SYSTEM_TO_APP).apply {
                setPackage(BuildConfigWrapper.APPLICATION_ID)
                putExtra(EXTRA_CMD, cmd)
                putExtra(EXTRA_REQ_ID, reqId)
                putExtra(EXTRA_CHUNK_INDEX, i)
                putExtra(EXTRA_CHUNK_TOTAL, totalChunks)
                putExtra(EXTRA_DATA, chunk)
            }
            dispatchReplyIntent(context, replyIntent)
        }
    }

    private fun ensureAppReceiver(context: Context) {
        if (isAppReceiverRegistered) return
        synchronized(this) {
            if (isAppReceiverRegistered) return
            val filter = IntentFilter(ACTION_SYSTEM_TO_APP)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action != ACTION_SYSTEM_TO_APP) return
                    val reqId = intent.getStringExtra(EXTRA_REQ_ID) ?: return
                    val totalChunks = intent.getIntExtra(EXTRA_CHUNK_TOTAL, 1)
                    val chunkIndex = intent.getIntExtra(EXTRA_CHUNK_INDEX, 0)
                    val chunkData = intent.getStringExtra(EXTRA_DATA) ?: ""

                    if (totalChunks <= 1) {
                        val callback = pendingCallbacks.remove(reqId)
                        callback?.let { cb ->
                            mainHandler.post { cb(chunkData.ifEmpty { null }) }
                        }
                    } else {
                        val chunks = pendingChunks.computeIfAbsent(reqId) { arrayOfNulls(totalChunks) }
                        if (chunkIndex in chunks.indices) {
                            chunks[chunkIndex] = chunkData
                        }
                        if (chunks.all { it != null }) {
                            pendingChunks.remove(reqId)
                            val fullData = chunks.filterNotNull().joinToString("")
                            val callback = pendingCallbacks.remove(reqId)
                            callback?.let { cb ->
                                mainHandler.post { cb(fullData.ifEmpty { null }) }
                            }
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.applicationContext.registerReceiver(receiver, filter)
            }
            isAppReceiverRegistered = true
        }
    }

    private fun sendRequest(
        context: Context,
        cmd: String,
        data: String? = null,
        timeoutMs: Long = 5000L,
        callback: ((data: String?) -> Unit)? = null
    ) {
        ensureAppReceiver(context)
        val reqId = UUID.randomUUID().toString()
        if (callback != null) {
            pendingCallbacks[reqId] = callback
            mainHandler.postDelayed({
                val cb = pendingCallbacks.remove(reqId)
                cb?.let { mainHandler.post { it(null) } }
            }, timeoutMs)
        }
        val intent = Intent(ACTION_APP_TO_SYSTEM).apply {
            setPackage("android")
            putExtra(EXTRA_CMD, cmd)
            putExtra(EXTRA_REQ_ID, reqId)
            data?.let { putExtra(EXTRA_DATA, it) }
        }
        context.sendBroadcast(intent)
    }

    /**
     * 宿主注册监听
     */
    object Host {

        private var systemContext: Context? = null

        private var refreshFrameworkPrefsDataCallback: (() -> Unit)? = null
        private var openAppUsedFrameworkCallback: ((Pair<String, Int>) -> Unit)? = null
        private var pushAppErrorInfoDataCallback: ((String) -> AppErrorsInfoBean)? = null
        private var pushAppErrorsInfoDataCallback: (() -> ArrayList<AppErrorsInfoBean>)? = null
        private var pushAppErrorsInfoDataFullCallback: (() -> ArrayList<AppErrorsInfoBean>)? = null
        private var removeAppErrorsInfoDataCallback: ((AppErrorsInfoBean) -> Unit)? = null
        private var clearAppErrorsInfoDataCallback: (() -> Unit)? = null
        private var mutedErrorsIfUnlockCallback: ((String) -> Unit)? = null
        private var mutedErrorsIfRestartCallback: ((String) -> Unit)? = null
        private var pushMutedErrorsAppsDataCallback: (() -> ArrayList<MutedErrorsAppBean>)? = null
        private var unmuteErrorsAppCallback: ((MutedErrorsAppBean) -> Unit)? = null
        private var unmuteAllErrorsAppsCallback: (() -> Unit)? = null
        private var pushAppListDataCallback: ((AppFiltersBean) -> ArrayList<AppInfoBean>)? = null

        @Volatile
        private var isRegistered = false

        fun with(context: Context, initiate: Host.() -> Unit) = apply {
            this.systemContext = context
            initiate()
            register(context)
        }

        private fun register(context: Context) {
            if (isRegistered) return
            val filter = IntentFilter(ACTION_APP_TO_SYSTEM)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action != ACTION_APP_TO_SYSTEM) return
                    val cmd = intent.getStringExtra(EXTRA_CMD) ?: return
                    val reqId = intent.getStringExtra(EXTRA_REQ_ID) ?: ""
                    val data = intent.getStringExtra(EXTRA_DATA)

                    fun reply(replyData: String? = null) {
                        if (reqId.isBlank()) return
                        sendReply(context, cmd, reqId, replyData)
                    }

                    when (cmd) {
                        CMD_PING -> reply("pong")
                        CMD_REFRESH_PREFS -> refreshFrameworkPrefsDataCallback?.invoke()
                        CMD_OPEN_APP -> {
                            data?.toEntityOrNull<Pair<String, Int>>()?.let {
                                openAppUsedFrameworkCallback?.invoke(it)
                            }
                        }
                        CMD_GET_APP_ERROR_DATA -> {
                            val id = data ?: ""
                            val bean = pushAppErrorInfoDataCallback?.invoke(id)
                            reply(bean?.toJsonOrNull())
                        }
                        CMD_GET_APP_ERRORS_DATA -> {
                            val list = pushAppErrorsInfoDataCallback?.invoke()
                            reply(list?.toJsonOrNull())
                        }
                        CMD_GET_APP_ERRORS_DATA_FULL -> {
                            val list = pushAppErrorsInfoDataFullCallback?.invoke()
                            reply(list?.toJsonOrNull())
                        }
                        CMD_REMOVE_APP_ERRORS_DATA -> {
                            data?.toEntityOrNull<AppErrorsInfoBean>()?.let {
                                removeAppErrorsInfoDataCallback?.invoke(it)
                            }
                            reply("ok")
                        }
                        CMD_CLEAR_APP_ERRORS_DATA -> {
                            clearAppErrorsInfoDataCallback?.invoke()
                            reply("ok")
                        }
                        CMD_MUTE_APP_UNLOCK -> {
                            data?.let { mutedErrorsIfUnlockCallback?.invoke(it) }
                            reply("ok")
                        }
                        CMD_MUTE_APP_RESTART -> {
                            data?.let { mutedErrorsIfRestartCallback?.invoke(it) }
                            reply("ok")
                        }
                        CMD_GET_MUTED_APPS -> {
                            val list = pushMutedErrorsAppsDataCallback?.invoke()
                            reply(list?.toJsonOrNull())
                        }
                        CMD_UNMUTE_APP -> {
                            data?.toEntityOrNull<MutedErrorsAppBean>()?.let {
                                unmuteErrorsAppCallback?.invoke(it)
                            }
                            reply("ok")
                        }
                        CMD_UNMUTE_ALL -> {
                            unmuteAllErrorsAppsCallback?.invoke()
                            reply("ok")
                        }
                        CMD_GET_APP_LIST -> {
                            val filters = data?.toEntityOrNull<AppFiltersBean>() ?: AppFiltersBean()
                            val list = pushAppListDataCallback?.invoke(filters)
                            reply(list?.toJsonOrNull())
                        }
                        CMD_GET_LOGS -> {
                            val logs = ArrayList(ModuleLog.inMemoryLogs)
                            reply(logs.toJsonOrNull())
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            isRegistered = true
            ModuleLog.info("FrameworkTool.Host registered successfully")
        }

        fun onRefreshFrameworkPrefsData(callback: () -> Unit) {
            refreshFrameworkPrefsDataCallback = callback
        }

        fun onOpenAppUsedFramework(result: (Pair<String, Int>) -> Unit) {
            openAppUsedFrameworkCallback = result
        }

        fun onPushAppErrorInfoData(result: (String) -> AppErrorsInfoBean) {
            pushAppErrorInfoDataCallback = result
        }

        fun onPushAppErrorsInfoData(result: () -> ArrayList<AppErrorsInfoBean>) {
            pushAppErrorsInfoDataCallback = result
        }

        fun onPushAppErrorsInfoDataFull(result: () -> ArrayList<AppErrorsInfoBean>) {
            pushAppErrorsInfoDataFullCallback = result
        }

        fun onRemoveAppErrorsInfoData(result: (AppErrorsInfoBean) -> Unit) {
            removeAppErrorsInfoDataCallback = result
        }

        fun onClearAppErrorsInfoData(callback: () -> Unit) {
            clearAppErrorsInfoDataCallback = callback
        }

        fun onMutedErrorsIfUnlock(result: (String) -> Unit) {
            mutedErrorsIfUnlockCallback = result
        }

        fun onMutedErrorsIfRestart(result: (String) -> Unit) {
            mutedErrorsIfRestartCallback = result
        }

        fun onPushMutedErrorsAppsData(result: () -> ArrayList<MutedErrorsAppBean>) {
            pushMutedErrorsAppsDataCallback = result
        }

        fun onUnmuteErrorsApp(result: (MutedErrorsAppBean) -> Unit) {
            unmuteErrorsAppCallback = result
        }

        fun onUnmuteAllErrorsApps(callback: () -> Unit) {
            unmuteAllErrorsAppsCallback = callback
        }

        fun onPushAppListData(result: (AppFiltersBean) -> ArrayList<AppInfoBean>) {
            pushAppListDataCallback = result
        }
    }

    /**
     * 重启系统
     * @param context 实例
     */
    fun restartSystem(context: Context) {
        fun showWhenAccessRootFail() =
            context.showDialog {
                title = context.getString(R.string.access_root_fail)
                msg = context.getString(R.string.access_root_fail_tip)
                confirmButton(context.getString(R.string.got_it))
            }
        context.showDialog {
            title = context.getString(R.string.notice)
            msg = context.getString(R.string.are_your_sure_restart_system)
            confirmButton {
                if (isRootAccess)
                    execShell(cmd = "reboot")
                else showWhenAccessRootFail()
            }
            neutralButton(context.getString(R.string.fast_restart)) {
                context.showDialog {
                    title = context.getString(R.string.warning)
                    msg = context.getString(R.string.fast_restart_problem)
                    confirmButton {
                        if (isRootAccess)
                            execShell(cmd = "killall zygote")
                        else showWhenAccessRootFail()
                    }
                    cancelButton()
                }
            }
            cancelButton()
        }
    }

    /**
     * 检查模块在系统框架中是否激活
     * @param context 实例
     * @param result 成功后回调
     */
    fun checkingActivated(context: Context, result: (Boolean) -> Unit) {
        var answered = false
        sendRequest(context, CMD_PING, timeoutMs = 1500L) { data ->
            if (!answered) {
                answered = true
                result(data != null)
            }
        }
    }

    /**
     * 通知系统框架刷新存储的数据
     * @param context 实例
     */
    fun refreshFrameworkPrefsData(context: Context) {
        sendRequest(context, CMD_REFRESH_PREFS)
    }

    /**
     * 使用系统框架打开 [packageName]
     * @param context 实例
     * @param packageName APP 包名
     * @param userId APP 用户 ID
     */
    fun openAppUsedFramework(context: Context, packageName: String, userId: Int) {
        sendRequest(context, CMD_OPEN_APP, Pair(packageName, userId).toJsonOrNull())
    }

    /**
     * 获取指定 APP 异常信息
     * @param context 实例
     * @param id 当前进程 ID 或记录时间戳
     * @param result 回调数据
     */
    fun fetchAppErrorInfoData(context: Context, id: Any, result: (AppErrorsInfoBean) -> Unit) {
        sendRequest(context, CMD_GET_APP_ERROR_DATA, id.toString()) { data ->
            val bean = data?.toEntityOrNull<AppErrorsInfoBean>() ?: AppErrorsInfoBean()
            result(bean)
        }
    }

    /**
     * 获取 APP 异常信息数组 (精简摘要列表)
     * @param context 实例
     * @param result 回调数据
     */
    fun fetchAppErrorsInfoData(context: Context, result: (ArrayList<AppErrorsInfoBean>) -> Unit) {
        sendRequest(context, CMD_GET_APP_ERRORS_DATA, timeoutMs = 15000L) { data ->
            newThread {
                val list = data?.toEntityOrNull<ArrayList<AppErrorsInfoBean>>() ?: arrayListOf()
                mainHandler.post { result(list) }
            }
        }
    }

    /**
     * 获取 APP 完整异常信息数组 (包含完整堆栈，用于导出)
     * @param context 实例
     * @param result 回调数据
     */
    fun fetchAppErrorsInfoDataFull(context: Context, result: (ArrayList<AppErrorsInfoBean>) -> Unit) {
        sendRequest(context, CMD_GET_APP_ERRORS_DATA_FULL, timeoutMs = 25000L) { data ->
            newThread {
                val list = data?.toEntityOrNull<ArrayList<AppErrorsInfoBean>>() ?: arrayListOf()
                mainHandler.post { result(list) }
            }
        }
    }

    /**
     * 移除指定 APP 异常信息
     * @param context 实例
     * @param appErrorsInfo 指定 APP 异常信息
     * @param callback 成功后回调
     */
    fun removeAppErrorsInfoData(context: Context, appErrorsInfo: AppErrorsInfoBean, callback: () -> Unit) {
        sendRequest(context, CMD_REMOVE_APP_ERRORS_DATA, appErrorsInfo.toJsonOrNull()) {
            callback()
        }
    }

    /**
     * 清空 APP 异常信息数组
     * @param context 实例
     * @param callback 成功后回调
     */
    fun clearAppErrorsInfoData(context: Context, callback: () -> Unit) {
        sendRequest(context, CMD_CLEAR_APP_ERRORS_DATA) {
            callback()
        }
    }

    /**
     * 忽略 [packageName] 的错误直到设备重新解锁
     * @param context 实例
     * @param packageName APP 包名
     * @param callback 成功后回调
     */
    fun mutedErrorsIfUnlock(context: Context, packageName: String, callback: () -> Unit) {
        sendRequest(context, CMD_MUTE_APP_UNLOCK, packageName) {
            callback()
        }
    }

    /**
     * 忽略 [packageName] 的错误直到设备重新启动
     * @param context 实例
     * @param packageName APP 包名
     * @param callback 成功后回调
     */
    fun mutedErrorsIfRestart(context: Context, packageName: String, callback: () -> Unit) {
        sendRequest(context, CMD_MUTE_APP_RESTART, packageName) {
            callback()
        }
    }

    /**
     * 获取已忽略异常的 APP 信息数组
     * @param context 实例
     * @param result 回调数据
     */
    fun fetchMutedErrorsAppsData(context: Context, result: (ArrayList<MutedErrorsAppBean>) -> Unit) {
        sendRequest(context, CMD_GET_MUTED_APPS) { data ->
            val list = data?.toEntityOrNull<ArrayList<MutedErrorsAppBean>>() ?: arrayListOf()
            result(list)
        }
    }

    /**
     * 取消指定已忽略异常的 APP
     * @param context 实例
     * @param mutedErrorsApp 指定已忽略异常的 APP 信息
     * @param callback 成功后回调
     */
    fun unmuteErrorsApp(context: Context, mutedErrorsApp: MutedErrorsAppBean, callback: () -> Unit) {
        sendRequest(context, CMD_UNMUTE_APP, mutedErrorsApp.toJsonOrNull()) {
            callback()
        }
    }

    /**
     * 取消全部已忽略异常的 APP
     * @param context 实例
     * @param callback 成功后回调
     */
    fun unmuteAllErrorsApps(context: Context, callback: () -> Unit) {
        sendRequest(context, CMD_UNMUTE_ALL) {
            callback()
        }
    }

    /**
     * 获取已安装 APP 列表数组
     * @param context 实例
     * @param appFilters 过滤条件
     * @param result 回调数据
     */
    fun fetchAppListData(context: Context, appFilters: AppFiltersBean, result: (ArrayList<AppInfoBean>) -> Unit) {
        sendRequest(context, CMD_GET_APP_LIST, appFilters.toJsonOrNull(), timeoutMs = 15000L) { data ->
            newThread {
                val list = data?.toEntityOrNull<ArrayList<AppInfoBean>>() ?: arrayListOf()
                mainHandler.post { result(list) }
            }
        }
    }

    /**
     * 获取内存调试日志
     * @param context 实例
     * @param result 回调数据
     */
    fun fetchLoggerData(context: Context, result: (List<ModuleLog.LogItem>) -> Unit) {
        sendRequest(context, CMD_GET_LOGS, timeoutMs = 10000L) { data ->
            newThread {
                val list = data?.toEntityOrNull<ArrayList<ModuleLog.LogItem>>() ?: arrayListOf()
                mainHandler.post { result(list) }
            }
        }
    }
}