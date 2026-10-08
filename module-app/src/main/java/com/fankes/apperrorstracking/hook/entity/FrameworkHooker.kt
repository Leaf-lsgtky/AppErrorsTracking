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
 * This file is created by fankes on 2022/5/7.
 */
@file:Suppress("ConstPropertyName")

package com.fankes.apperrorstracking.hook.entity

import android.app.ApplicationErrorReport
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.os.SystemClock
import android.util.ArrayMap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.bean.AppErrorsDisplayBean
import com.fankes.apperrorstracking.bean.AppErrorsInfoBean
import com.fankes.apperrorstracking.bean.AppInfoBean
import com.fankes.apperrorstracking.bean.MutedErrorsAppBean
import com.fankes.apperrorstracking.bean.enum.AppFiltersType
import com.fankes.apperrorstracking.data.AppErrorsConfigData
import com.fankes.apperrorstracking.data.AppErrorsRecordData
import com.fankes.apperrorstracking.data.ConfigData
import com.fankes.apperrorstracking.data.enum.AppErrorsConfigType
import com.fankes.apperrorstracking.ui.activity.errors.AppErrorsDisplayActivity
import com.fankes.apperrorstracking.ui.activity.errors.AppErrorsRecordActivity
import com.fankes.apperrorstracking.utils.factory.appNameOf
import com.fankes.apperrorstracking.utils.factory.drawableOf
import com.fankes.apperrorstracking.utils.factory.isAppCanOpened
import com.fankes.apperrorstracking.utils.factory.listOfPackages
import com.fankes.apperrorstracking.utils.factory.openApp
import com.fankes.apperrorstracking.utils.factory.pushNotify
import com.fankes.apperrorstracking.utils.factory.toArrayList
import com.fankes.apperrorstracking.utils.factory.toast
import com.fankes.apperrorstracking.utils.tool.FrameworkTool
import com.fankes.apperrorstracking.utils.tool.ModuleLog
import com.fankes.apperrorstracking.wrapper.BuildConfigWrapper
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method

object FrameworkHooker {

    private lateinit var module: XposedModule

    @Volatile
    private var systemContext: Context? = null

    @Volatile
    private var cachedModuleResources: Resources? = null

    @Volatile
    private var isHostInitialized = false

    /** 已忽略错误的 APP 数组 - 直到重新解锁 */
    private val mutedErrorsIfUnlockApps = mutableSetOf<String>()

    /** 已忽略错误的 APP 数组 - 直到重新启动 */
    private val mutedErrorsIfRestartApps = mutableSetOf<String>()

    private fun findClass(className: String, classLoader: ClassLoader): Class<*>? = runCatching {
        Class.forName(className, false, classLoader)
    }.getOrNull()

    private fun findMethod(clazz: Class<*>?, name: String, vararg paramTypes: Class<*>): Method? {
        if (clazz == null) return null
        return runCatching { clazz.getDeclaredMethod(name, *paramTypes).apply { isAccessible = true } }.getOrNull()
            ?: runCatching { clazz.getMethod(name, *paramTypes).apply { isAccessible = true } }.getOrNull()
            ?: clazz.declaredMethods.firstOrNull { it.name == name && it.parameterTypes.contentEquals(paramTypes) }?.apply { isAccessible = true }
    }

    private fun findField(clazz: Class<*>?, name: String): Field? {
        if (clazz == null) return null
        var cur: Class<*>? = clazz
        while (cur != null && cur != Any::class.java) {
            val f = runCatching { cur.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()
            if (f != null) return f
            cur = cur.superclass
        }
        return null
    }

    private fun getContextFromInstance(instance: Any?): Context? {
        if (instance == null) return null
        return runCatching {
            var clazz: Class<*>? = instance.javaClass
            while (clazz != null && clazz != Any::class.java) {
                val field = clazz.declaredFields.firstOrNull { it.name == "mContext" || it.type == Context::class.java }
                if (field != null) {
                    field.isAccessible = true
                    val ctx = field.get(instance) as? Context
                    if (ctx != null) return ctx
                }
                clazz = clazz.superclass
            }
            // For AppErrors: it holds mService (ActivityManagerService) which has mContext
            val service = runCatching {
                instance.javaClass.declaredFields.firstOrNull { it.name == "mService" }?.apply { isAccessible = true }?.get(instance)
            }.getOrNull()
            if (service != null) {
                var sClazz: Class<*>? = service.javaClass
                while (sClazz != null && sClazz != Any::class.java) {
                    val sField = sClazz.declaredFields.firstOrNull { it.name == "mContext" || it.type == Context::class.java }
                    if (sField != null) {
                        sField.isAccessible = true
                        val sCtx = sField.get(service) as? Context
                        if (sCtx != null) return sCtx
                    }
                    sClazz = sClazz.superclass
                }
            }
            null
        }.getOrNull()
    }

    private fun getSystemContextFromActivityThread(): Context? {
        return runCatching {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            activityThreadClass.getMethod("getSystemContext").invoke(currentActivityThread) as? Context
        }.getOrNull()
    }

    private fun getModuleResources(context: Context): Resources {
        cachedModuleResources?.let { return it }
        return runCatching {
            val moduleAppInfo = module.moduleApplicationInfo
            context.packageManager.getResourcesForApplication(moduleAppInfo)
        }.getOrElse {
            context.resources
        }.also {
            cachedModuleResources = it
        }
    }

    private fun refreshModuleAppResources(context: Context) {
        cachedModuleResources = null
        getModuleResources(context)
    }

    /**
     * APP 进程异常数据定义类
     */
    private class AppErrorsProcessData(private val errors: Any?, private val proc: Any, private val resultData: Any? = null) {

        private val pkgList by lazy {
            runCatching {
                proc.javaClass.methods.firstOrNull { it.name == "getPkgList" && it.parameterTypes.isEmpty() }?.invoke(proc)
            }.getOrNull() ?: runCatching {
                findField(proc.javaClass, "mPkgList")?.get(proc) ?: findField(proc.javaClass, "pkgList")?.get(proc)
            }.getOrNull()
        }

        private val pkgListSize by lazy {
            runCatching {
                if (pkgList != null) {
                    pkgList?.javaClass?.methods?.firstOrNull { it.name == "size" && it.parameterTypes.isEmpty() }?.invoke(pkgList) as? Int
                        ?: (pkgList as? ArrayMap<*, *>)?.size
                        ?: (pkgList as? Collection<*>)?.size
                        ?: -1
                } else {
                    (findField(proc.javaClass, "pkgList")?.get(proc) as? ArrayMap<*, *>)?.size ?: -1
                }
            }.getOrNull() ?: -1
        }

        val pid: Int by lazy {
            runCatching {
                (findField(proc.javaClass, "mPid")?.get(proc) as? Int)
                    ?: (findField(proc.javaClass, "pid")?.get(proc) as? Int)
                    ?: 0
            }.getOrNull() ?: 0
        }

        val userId: Int by lazy {
            runCatching {
                (findField(proc.javaClass, "userId")?.get(proc) as? Int) ?: 0
            }.getOrNull() ?: 0
        }

        val appInfo: ApplicationInfo? by lazy {
            runCatching {
                (findField(proc.javaClass, "info")?.get(proc) as? ApplicationInfo)
                    ?: (findField(proc.javaClass, "mInfo")?.get(proc) as? ApplicationInfo)
            }.getOrNull()
        }

        val processName: String by lazy {
            runCatching {
                (findField(proc.javaClass, "processName")?.get(proc) as? String)
                    ?: (findField(proc.javaClass, "mProcessName")?.get(proc) as? String)
                    ?: ""
            }.getOrNull() ?: ""
        }

        val packageName: String = appInfo?.packageName ?: processName

        val isActualApp: Boolean = pkgListSize == 1 && appInfo != null

        val isMainProcess: Boolean = packageName == processName

        val isBackgroundProcess: Boolean by lazy {
            runCatching {
                val service = findField(errors?.javaClass, "mService")?.get(errors)
                val userController = findField(service?.javaClass, "mUserController")?.get(service)
                val method = userController?.javaClass?.methods?.firstOrNull {
                    it.name == "getCurrentProfileIds" || it.name == "getCurrentProfileIdsLocked"
                }
                val profileIds = method?.invoke(userController) as? IntArray
                if (profileIds != null && profileIds.isNotEmpty()) {
                    profileIds.any { it != userId }
                } else false
            }.getOrNull() ?: false
        }

        val isRepeatingCrash: Boolean by lazy {
            resultData?.let {
                runCatching {
                    findField(it.javaClass, "repeating")?.get(it) as? Boolean
                }.getOrNull()
            } ?: false
        }
    }

    private fun ensureInitializedWithContext(context: Context) {
        if (isHostInitialized) return
        synchronized(this) {
            if (isHostInitialized) return

            // ActivityManager must be ready before registering receivers, otherwise NPE occurs
            val isAmReady = runCatching {
                Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null) != null
            }.getOrElse { false }
            if (!isAmReady) {
                ModuleLog.info("ActivityManager not ready yet, postponing ensureInitializedWithContext")
                return
            }

            systemContext = context

            runCatching {
                val filter1 = IntentFilter(Intent.ACTION_USER_PRESENT)
                context.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        mutedErrorsIfUnlockApps.clear()
                    }
                }, filter1)

                val filter2 = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
                context.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        refreshModuleAppResources(context)
                    }
                }, filter2)
            }.onFailure {
                ModuleLog.warn("Failed to register system lifecycle receivers: ${it.message}")
            }

            runCatching {
                AppErrorsRecordData.init(context)
            }.onFailure {
                ModuleLog.error("Failed to init AppErrorsRecordData", it)
            }

            var registeredSuccessfully = false
            runCatching {
                FrameworkTool.Host.with(context) {
                    onRefreshFrameworkPrefsData {
                        SystemClock.sleep(100)
                        AppErrorsConfigData.refresh()
                    }
                    onOpenAppUsedFramework {
                        systemContext?.openApp(it.first, it.second)
                        ModuleLog.info("Opened \"${it.first}\"${it.second.takeIf { e -> e > 0 }?.let { e -> " --user $e" } ?: ""}")
                    }
                    onPushAppErrorInfoData { id ->
                        val timestamp = id.toLongOrNull()
                        val pid = id.toIntOrNull() ?: 0
                        (if (timestamp != null && timestamp > 0L) {
                            AppErrorsRecordData.allData.firstOrNull { e -> e.timestamp == timestamp }
                        } else null) ?: AppErrorsRecordData.allData.firstOrNull { e -> e.pid == pid } ?: run {
                            ModuleLog.warn("Cannot find crash application data --id $id")
                            AppErrorsInfoBean()
                        }
                    }
                    onPushAppErrorsInfoData {
                        AppErrorsRecordData.allData.map { it.copy(stackTrace = "") }.toArrayList()
                    }
                    onPushAppErrorsInfoDataFull {
                        AppErrorsRecordData.allData.toArrayList()
                    }
                    onRemoveAppErrorsInfoData {
                        ModuleLog.info("Removed app errors info data for package \"${it.packageName}\"")
                        AppErrorsRecordData.remove(it)
                    }
                    onClearAppErrorsInfoData {
                        ModuleLog.info("Cleared all app errors info data, size ${AppErrorsRecordData.allData.size}")
                        AppErrorsRecordData.clearAll()
                    }
                    onMutedErrorsIfUnlock {
                        mutedErrorsIfUnlockApps.add(it)
                        ModuleLog.info("Muted \"$it\" until unlocks")
                    }
                    onMutedErrorsIfRestart {
                        mutedErrorsIfRestartApps.add(it)
                        ModuleLog.info("Muted \"$it\" until restarts")
                    }
                    onPushMutedErrorsAppsData {
                        arrayListOf<MutedErrorsAppBean>().apply {
                            mutedErrorsIfUnlockApps.takeIf { it.isNotEmpty() }
                                ?.forEach { add(MutedErrorsAppBean(MutedErrorsAppBean.MuteType.UNTIL_UNLOCKS, it)) }
                            mutedErrorsIfRestartApps.takeIf { it.isNotEmpty() }
                                ?.forEach { add(MutedErrorsAppBean(MutedErrorsAppBean.MuteType.UNTIL_REBOOTS, it)) }
                        }
                    }
                    onUnmuteErrorsApp {
                        when (it.type) {
                            MutedErrorsAppBean.MuteType.UNTIL_UNLOCKS -> {
                                ModuleLog.info("Unmuted if unlocks errors app \"${it.packageName}\"")
                                mutedErrorsIfUnlockApps.remove(it.packageName)
                            }
                            MutedErrorsAppBean.MuteType.UNTIL_REBOOTS -> {
                                ModuleLog.info("Unmuted if restarts errors app \"${it.packageName}\"")
                                mutedErrorsIfRestartApps.remove(it.packageName)
                            }
                        }
                    }
                    onUnmuteAllErrorsApps {
                        ModuleLog.info("Unmute all errors apps --unlocks ${mutedErrorsIfUnlockApps.size} --restarts ${mutedErrorsIfRestartApps.size}")
                        mutedErrorsIfUnlockApps.clear()
                        mutedErrorsIfRestartApps.clear()
                    }
                    onPushAppListData { filters ->
                        systemContext?.let { ctx ->
                            ctx.listOfPackages()
                                .filter { it.packageName.let { e -> e != "android" && e != BuildConfigWrapper.APPLICATION_ID } }
                                .let { info ->
                                    arrayListOf<AppInfoBean>().apply {
                                        if (info.isNotEmpty())
                                            (if (filters.name.isNotBlank()) info.filter {
                                                it.packageName.contains(filters.name) || ctx.appNameOf(it.packageName).contains(filters.name)
                                            } else info).let { result ->
                                                fun PackageInfo.isSystemApp() = applicationInfo?.let {
                                                    (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                                                } ?: false
                                                when (filters.type) {
                                                    AppFiltersType.USER -> result.filter { it.isSystemApp().not() }
                                                    AppFiltersType.SYSTEM -> result.filter { it.isSystemApp() }
                                                    AppFiltersType.ALL -> result
                                                }
                                            }.sortedByDescending { it.lastUpdateTime }
                                                .forEach { add(AppInfoBean(name = ctx.appNameOf(it.packageName), packageName = it.packageName)) }
                                        else ModuleLog.warn("Fetched installed packages but got empty list")
                                    }
                                }
                        } ?: arrayListOf()
                    }
                }
                registeredSuccessfully = true
            }.onFailure {
                ModuleLog.error("Failed to register FrameworkTool.Host", it)
            }

            if (registeredSuccessfully) {
                isHostInitialized = true
                ModuleLog.info("FrameworkHooker host initialized successfully")
            }
        }
    }

    private fun AppErrorsProcessData.handleShowAppErrorUi(context: Context) {
        val res = getModuleResources(context)
        val appName = appInfo?.let { context.appNameOf(it.packageName).ifBlank { it.packageName } } ?: packageName
        val appNameWithUserId = if (userId != 0) "$appName (${res.getString(R.string.user_id, userId)})" else appName
        val errorTitle = if (isRepeatingCrash)
            res.getString(R.string.aerr_repeated_title, appNameWithUserId)
        else res.getString(R.string.aerr_title, appNameWithUserId)

        fun showAppErrorsWithNotify() {
            val notifyIcon = runCatching {
                IconCompat.createWithBitmap(res.drawableOf(R.drawable.ic_notify).toBitmap())
            }.getOrElse {
                IconCompat.createWithResource(context, android.R.drawable.stat_notify_error)
            }
            context.pushNotify(
                channelId = "APPS_ERRORS",
                channelName = res.getString(R.string.app_name),
                title = errorTitle,
                content = res.getString(R.string.app_errors_tip),
                icon = notifyIcon,
                color = 0xFFFF6200.toInt(),
                intent = AppErrorsRecordActivity.intent()
            )
        }

        fun showAppErrorsWithToast() = context.toast(errorTitle)

        fun showAppErrorsWithDialog() {
            AppErrorsDisplayActivity.start(
                context, AppErrorsDisplayBean(
                    pid = pid,
                    userId = userId,
                    packageName = packageName,
                    processName = processName,
                    appName = appName,
                    title = errorTitle,
                    isShowAppInfoButton = isActualApp,
                    isShowReopenButton = isActualApp &&
                        (isRepeatingCrash.not() || ConfigData.isEnableAlwaysShowsReopenAppOptions) &&
                        context.isAppCanOpened(packageName) &&
                        isMainProcess,
                    isShowCloseAppButton = isActualApp
                )
            )
        }

        if (mutedErrorsIfUnlockApps.contains(packageName) || mutedErrorsIfRestartApps.contains(packageName)) return
        if ((isBackgroundProcess || context.isAppCanOpened(packageName).not()) && ConfigData.isEnableOnlyShowErrorsInFront) return
        if (isMainProcess.not() && ConfigData.isEnableOnlyShowErrorsInMain) return

        when {
            packageName == BuildConfigWrapper.APPLICATION_ID -> {
                context.toast(msg = "AppErrorsTracking has crashed, please see the log in console")
                ModuleLog.error("AppErrorsTracking has crashed itself, please see the Android Runtime Exception in console")
            }
            ConfigData.isEnableAppConfigTemplate -> when {
                AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.GLOBAL, packageName) -> when {
                    AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.DIALOG) -> showAppErrorsWithDialog()
                    AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTIFY) -> showAppErrorsWithNotify()
                    AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.TOAST) -> showAppErrorsWithToast()
                    AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTHING) -> {}
                }
                AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.DIALOG, packageName) -> showAppErrorsWithDialog()
                AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTIFY, packageName) -> showAppErrorsWithNotify()
                AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.TOAST, packageName) -> showAppErrorsWithToast()
                AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTHING, packageName) -> {}
            }
            else -> showAppErrorsWithDialog()
        }

        if (isActualApp) ModuleLog.error(
            msg = "Application \"$packageName\" ${if (isRepeatingCrash) "keeps stopping" else "has stopped"}" +
                (if (packageName != processName) " --process \"$processName\"" else "") +
                "${if (userId != 0) " --user $userId" else ""} --pid $pid"
        ) else ModuleLog.error("Process \"$processName\" ${if (isRepeatingCrash) "keeps stopping" else "has stopped"} --pid $pid")
    }

    private fun AppErrorsProcessData.handleAppErrorsInfo(context: Context, info: ApplicationErrorReport.CrashInfo?) {
        AppErrorsRecordData.add(AppErrorsInfoBean.clone(context, pid, userId, appInfo?.packageName, info))
        ModuleLog.info("Received crash application data${if (userId != 0) " --user $userId" else ""} --pid $pid")
    }

    fun init(module: XposedModule, classLoader: ClassLoader) {
        this.module = module

        val appErrorsClass = findClass("com.android.server.am.AppErrors", classLoader)
        val appErrorDialogClass = findClass("com.android.server.am.AppErrorDialog", classLoader)
        val appErrorDialogDataClass = findClass("com.android.server.am.AppErrorDialog\$Data", classLoader)
        val activityManagerServiceClass = findClass("com.android.server.am.ActivityManagerService", classLoader)
        val activityTaskManagerServiceLocalServiceClass = findClass("com.android.server.wm.ActivityTaskManagerService\$LocalService", classLoader)
        val errorDialogControllerClass = findClass("com.android.server.am.ProcessRecord\$ErrorDialogController", classLoader)
            ?: findClass("com.android.server.am.ErrorDialogController", classLoader)

        // 尝试提前从 ActivityThread 获取 Context
        runCatching {
            val ctx = getSystemContextFromActivityThread()
            if (ctx != null) ensureInitializedWithContext(ctx)
        }

        // 监听 AMS.systemReady
        activityManagerServiceClass?.declaredMethods?.filter { it.name == "systemReady" }?.forEach { method ->
            runCatching {
                module.hook(method).intercept { chain ->
                    val res = chain.proceed()
                    runCatching {
                        val ctx = getContextFromInstance(chain.thisObject) ?: getSystemContextFromActivityThread()
                        if (ctx != null) ensureInitializedWithContext(ctx)
                    }
                    res
                }
            }
        }

        // 监听 AppErrors 构造以获得 Context
        appErrorsClass?.declaredConstructors?.forEach { ctor ->
            runCatching {
                module.hook(ctor).intercept { chain ->
                    val res = chain.proceed()
                    val ctx = chain.args.firstOrNull { it is Context } as? Context
                        ?: getContextFromInstance(chain.thisObject)
                        ?: getSystemContextFromActivityThread()
                    if (ctx != null) ensureInitializedWithContext(ctx)
                    res
                }
            }
        }

        // 干掉原生错误对话框 - ErrorDialogController
        errorDialogControllerClass?.let { edcClass ->
            val hasCrashDialogsMethod = findMethod(edcClass, "hasCrashDialogs")
            if (hasCrashDialogsMethod != null) {
                runCatching {
                    module.hook(hasCrashDialogsMethod).intercept { true }
                }
            } else {
                edcClass.declaredConstructors.firstOrNull { it.parameterTypes.size == 1 }?.let { ctor ->
                    runCatching {
                        module.hook(ctor).intercept { chain ->
                            val result = chain.proceed()
                            runCatching {
                                val field = findField(edcClass, "mCrashDialogs")
                                field?.set(chain.thisObject, emptyList<Any>())
                            }
                            result
                        }
                    }
                }
            }
            edcClass.declaredMethods.firstOrNull { it.name == "showCrashDialogs" && it.parameterTypes.size == 1 }?.let { method ->
                runCatching {
                    module.hook(method).intercept { null }
                }
            }
        }

        // 干掉原生错误对话框 - API 30 以下
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            findMethod(activityTaskManagerServiceLocalServiceClass, "canShowErrorDialogs")?.let { m ->
                runCatching { module.hook(m).intercept { false } }
            }
            findMethod(activityManagerServiceClass, "canShowErrorDialogs")?.let { m ->
                runCatching { module.hook(m).intercept { false } }
            }
        }

        // 干掉原生错误对话框 - 如果上述方法失效则直接取消对话框
        appErrorDialogClass?.let { dialogClass ->
            findMethod(dialogClass, "onCreate", Bundle::class.java)?.let { m ->
                runCatching {
                    module.hook(m).intercept { chain ->
                        val res = chain.proceed()
                        (chain.thisObject as? Dialog)?.cancel()
                        res
                    }
                }
            }
            findMethod(dialogClass, "onStart")?.let { m ->
                runCatching {
                    module.hook(m).intercept { chain ->
                        val res = chain.proceed()
                        (chain.thisObject as? Dialog)?.cancel()
                        res
                    }
                }
            }
        }

        // 注入自定义错误处理
        appErrorsClass?.let { aeClass ->
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                val method = aeClass.declaredMethods.firstOrNull { it.name == "handleAppCrashLSPB" && it.parameterTypes.size == 6 }
                method?.let {
                    runCatching {
                        module.hook(it).intercept { chain ->
                            val res = chain.proceed()
                            val args = chain.args
                            if (args.getOrNull(1) as? String == "user-terminated") return@intercept res
                            val context = systemContext ?: getContextFromInstance(chain.thisObject) ?: getSystemContextFromActivityThread()
                            if (context != null) {
                                ensureInitializedWithContext(context)
                                val proc = args.firstOrNull()
                                if (proc == null) {
                                    ModuleLog.warn("Received but got null ProcessRecord (Show UI failed)")
                                } else {
                                    val resultData = args.lastOrNull()
                                    AppErrorsProcessData(chain.thisObject, proc, resultData).handleShowAppErrorUi(context)
                                }
                            }
                            res
                        }
                    }
                }
            } else {
                val method = aeClass.declaredMethods.firstOrNull {
                    it.name == "handleShowAppErrorUi" && it.parameterTypes.size == 1 && it.parameterTypes[0] == Message::class.java
                }
                method?.let {
                    runCatching {
                        module.hook(it).intercept { chain ->
                            val res = chain.proceed()
                            val msg = chain.args.firstOrNull() as? Message
                            val resultData = msg?.obj
                            val context = systemContext ?: getContextFromInstance(chain.thisObject) ?: getSystemContextFromActivityThread()
                            if (context != null && resultData != null) {
                                ensureInitializedWithContext(context)
                                val procField = findField(resultData.javaClass, "proc")
                                    ?: appErrorDialogDataClass?.let { c -> findField(c, "proc") }
                                val proc = procField?.get(resultData)
                                if (proc != null) {
                                    AppErrorsProcessData(chain.thisObject, proc, resultData).handleShowAppErrorUi(context)
                                }
                            }
                            res
                        }
                    }
                }
            }

            val handleCrashInController = aeClass.declaredMethods.firstOrNull {
                it.name == "handleAppCrashInActivityController" && it.returnType == java.lang.Boolean.TYPE
            }
            handleCrashInController?.let {
                runCatching {
                    module.hook(it).intercept { chain ->
                        val res = chain.proceed()
                        val context = systemContext ?: getContextFromInstance(chain.thisObject) ?: getSystemContextFromActivityThread()
                        if (context != null) {
                            ensureInitializedWithContext(context)
                            val proc = chain.args.firstOrNull()
                            if (proc == null) {
                                ModuleLog.warn("Received but got null ProcessRecord")
                            } else {
                                val crashInfo = chain.args.getOrNull(1) as? ApplicationErrorReport.CrashInfo
                                AppErrorsProcessData(chain.thisObject, proc).handleAppErrorsInfo(context, crashInfo)
                            }
                        }
                        res
                    }
                }
            }
        }
    }
}