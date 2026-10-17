/*
 * AppErrorsTracking - Added more features to app's crash dialog, fixed custom rom deleted dialog, the best experience to Android developer.
 * Copyright (C) 2017 Fankes Studio(qzmmcn@163.com)
 * https://github.com/KitsunePie/AppErrorsTracking
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 */
package com.fankes.apperrorstracking.ui.activity.main

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fankes.apperrorstracking.application.AppErrorsApplication
import com.fankes.apperrorstracking.const.ModuleVersion
import com.fankes.apperrorstracking.ui.screen.MainScreen
import com.fankes.apperrorstracking.ui.theme.AppMiuixTheme
import com.fankes.projectpromote.ProjectPromote

class MainActivity : AppCompatActivity() {

    companion object {
        /** 模块是否有效 */
        var isModuleValied = false

        /** 模块是否激活 */
        val isModuleActive: Boolean
            get() = AppErrorsApplication.xposedService != null || isModuleValied
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkingTopComponentName()
        supportActionBar?.hide()
        enableEdgeToEdge()

        if (isModuleActive) {
            ProjectPromote.show(activity = this, ModuleVersion.toString())
        }

        setContent {
            AppMiuixTheme {
                MainScreen()
            }
        }
    }

    /**
     * 在旧版的 Android 系统中使用了 activity-alias 标签从启动器启动 Activity 会造成其组件名称 (完整类名) 为代理名称
     *
     * 为了获取真实的顶层 Activity 组件名称 (完整类名) - 如果名称不正确将自动执行一次结束并重新打开当前 Activity
     */
    private fun checkingTopComponentName() {
        val topComponentName = runCatching {
            @Suppress("DEPRECATION")
            (getSystemService(ACTIVITY_SERVICE) as? ActivityManager?)
                ?.getRunningTasks(9999)?.firstOrNull()?.topActivity?.className ?: ""
        }.getOrNull() ?: ""
        if (topComponentName.isNotBlank() && topComponentName != javaClass.name) {
            finish()
            startActivity(Intent(this, javaClass))
        }
    }
}