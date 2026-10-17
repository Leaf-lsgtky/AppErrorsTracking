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
 * This file is created by fankes on 2022/10/1.
 */
@file:Suppress("MemberVisibilityCanBePrivate")

package com.fankes.apperrorstracking.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.fankes.apperrorstracking.application.AppErrorsApplication
import com.fankes.apperrorstracking.utils.tool.ModuleLog

/**
 * 全局配置存储控制类
 */
object ConfigData {

    const val PREFS_GROUP = "default"
    private const val LOCAL_PREFS_NAME = "app_errors_config"

    /** 显示开发者提示 */
    val SHOW_DEVELOPER_NOTICE = PrefsData("_show_developer_notice", true)

    /** 启用 Material 3 风格的错误对话框 */
    val ENABLE_MATERIAL3_STYLE_APP_ERRORS_DIALOG = PrefsData("_enable_material3_style_dialog", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    /** 仅对前台应用显示错误对话框 */
    val ENABLE_ONLY_SHOW_ERRORS_IN_FRONT = PrefsData("_enable_only_show_errors_in_front", false)

    /** 仅对应用主进程显示错误对话框 */
    val ENABLE_ONLY_SHOW_ERRORS_IN_MAIN = PrefsData("_enable_only_show_errors_in_main", false)

    /** 错误对话框始终显示“重新打开”选项 */
    val ENABLE_ALWAYS_SHOWS_REOPEN_APP_OPTIONS = PrefsData("_enable_always_shows_reopen_app_options", false)

    /** 启用应用配置模板 */
    val ENABLE_APP_CONFIG_TEMPLATE = PrefsData("_enable_app_config_template", false)

    /** 启用对话框防误触 */
    val ENABLE_PREVENT_MISOPERATION_FOR_DIALOG = PrefsData("_enable_prevent_misoperation_for_dialog", false)

    /** 禁止异常堆栈内容自动换行 */
    val DISABLE_AUTO_WRAP_ERROR_STACK_TRACE = PrefsData("_disable_auto_wrap_error_stack_trace", false)

    /** 分享时使用文件 */
    val SHARE_WITH_FILE = PrefsData("_share_with_file", false)

    /** 当前存储实例 */
    @Volatile
    private var sharedPrefs: SharedPreferences? = null

    /**
     * 初始化存储控制类
     * @param instance 实例 - 可以是 [Context] 或 [SharedPreferences]
     */
    fun init(instance: Any) {
        when (instance) {
            is Context -> {
                sharedPrefs = instance.getSharedPreferences(LOCAL_PREFS_NAME, Context.MODE_PRIVATE)
            }
            is SharedPreferences -> {
                sharedPrefs = instance
            }
            else -> ModuleLog.warn("Unknown instance type for ConfigData.init: ${instance::class.java.name}")
        }
        AppErrorsConfigData.refresh()
    }

    /**
     * 读取 [Set]<[String]> 数据
     * @param key 键值名称
     * @return [Set]<[String]>
     */
    internal fun getStringSet(key: String): Set<String> {
        return sharedPrefs?.getStringSet(key, emptySet()) ?: emptySet()
    }

    /**
     * 存入 [Set]<[String]> 数据
     * @param key 键值名称
     * @param value 键值内容
     */
    internal fun putStringSet(key: String, value: Set<String>) {
        sharedPrefs?.edit()?.putStringSet(key, value)?.apply()
        runCatching {
            AppErrorsApplication.xposedService?.getRemotePreferences(PREFS_GROUP)?.edit()?.putStringSet(key, value)?.apply()
        }
    }

    /**
     * 读取 [Int] 数据
     * @param data 键值数据模板
     * @return [Int]
     */
    internal fun getInt(data: PrefsData<Int>): Int {
        return sharedPrefs?.getInt(data.key, data.value) ?: data.value
    }

    /**
     * 存入 [Int] 数据
     * @param data 键值数据模板
     * @param value 键值内容
     */
    internal fun putInt(data: PrefsData<Int>, value: Int) {
        sharedPrefs?.edit()?.putInt(data.key, value)?.apply()
        runCatching {
            AppErrorsApplication.xposedService?.getRemotePreferences(PREFS_GROUP)?.edit()?.putInt(data.key, value)?.apply()
        }
    }

    /**
     * 读取 [Boolean] 数据
     * @param data 键值数据模板
     * @return [Boolean]
     */
    internal fun getBoolean(data: PrefsData<Boolean>): Boolean {
        return sharedPrefs?.getBoolean(data.key, data.value) ?: data.value
    }

    /**
     * 存入 [Boolean] 数据
     * @param data 键值数据模板
     * @param value 键值内容
     */
    internal fun putBoolean(data: PrefsData<Boolean>, value: Boolean) {
        sharedPrefs?.edit()?.putBoolean(data.key, value)?.apply()
        runCatching {
            AppErrorsApplication.xposedService?.getRemotePreferences(PREFS_GROUP)?.edit()?.putBoolean(data.key, value)?.apply()
        }
    }

    /**
     * 是否显示开发者提示
     * @return [Boolean]
     */
    var isShowDeveloperNotice
        get() = getBoolean(SHOW_DEVELOPER_NOTICE)
        set(value) {
            putBoolean(SHOW_DEVELOPER_NOTICE, value)
        }

    /**
     * 是否仅对前台应用显示错误对话框
     * @return [Boolean]
     */
    var isEnableOnlyShowErrorsInFront
        get() = getBoolean(ENABLE_ONLY_SHOW_ERRORS_IN_FRONT)
        set(value) {
            putBoolean(ENABLE_ONLY_SHOW_ERRORS_IN_FRONT, value)
        }

    /**
     * 是否仅对应用主进程显示错误对话框
     * @return [Boolean]
     */
    var isEnableOnlyShowErrorsInMain
        get() = getBoolean(ENABLE_ONLY_SHOW_ERRORS_IN_MAIN)
        set(value) {
            putBoolean(ENABLE_ONLY_SHOW_ERRORS_IN_MAIN, value)
        }

    /**
     * 错误对话框是否始终显示“重新打开”选项
     * @return [Boolean]
     */
    var isEnableAlwaysShowsReopenAppOptions
        get() = getBoolean(ENABLE_ALWAYS_SHOWS_REOPEN_APP_OPTIONS)
        set(value) {
            putBoolean(ENABLE_ALWAYS_SHOWS_REOPEN_APP_OPTIONS, value)
        }

    /**
     * 是否启用应用配置模板
     * @return [Boolean]
     */
    var isEnableAppConfigTemplate
        get() = getBoolean(ENABLE_APP_CONFIG_TEMPLATE)
        set(value) {
            putBoolean(ENABLE_APP_CONFIG_TEMPLATE, value)
        }

    /**
     * 是否启用对话框防误触
     * @return [Boolean]
     */
    var isEnablePreventMisoperation
        get() = getBoolean(ENABLE_PREVENT_MISOPERATION_FOR_DIALOG)
        set(value) {
            putBoolean(ENABLE_PREVENT_MISOPERATION_FOR_DIALOG, value)
        }

    /**
     * 是否启用 Material 3 风格的错误对话框
     * @return [Boolean]
     */
    var isEnableMaterial3StyleAppErrorsDialog
        get() = getBoolean(ENABLE_MATERIAL3_STYLE_APP_ERRORS_DIALOG)
        set(value) {
            putBoolean(ENABLE_MATERIAL3_STYLE_APP_ERRORS_DIALOG, value)
        }

    /**
     * 是否以文件方式分享
     */
    var isShareWithFile
        get() = getBoolean(SHARE_WITH_FILE)
        set(value) {
            putBoolean(SHARE_WITH_FILE, value)
        }

    /**
     * 是否禁止异常堆栈内容自动换行
     */
    var isDisableAutoWrapErrorStackTrace
        get() = getBoolean(DISABLE_AUTO_WRAP_ERROR_STACK_TRACE)
        set(value) {
            putBoolean(DISABLE_AUTO_WRAP_ERROR_STACK_TRACE, value)
        }
}