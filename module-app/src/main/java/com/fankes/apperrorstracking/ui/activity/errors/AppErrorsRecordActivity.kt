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
package com.fankes.apperrorstracking.ui.activity.errors

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.fankes.apperrorstracking.ui.screen.records.RecordsScreen
import com.fankes.apperrorstracking.ui.theme.AppMiuixTheme
import com.fankes.apperrorstracking.wrapper.BuildConfigWrapper

class AppErrorsRecordActivity : AppCompatActivity() {

    companion object {
        /**
         * 获取 [Intent]
         * @return [Intent]
         */
        fun intent() = Intent().apply {
            component = ComponentName(BuildConfigWrapper.APPLICATION_ID, AppErrorsRecordActivity::class.java.name)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()

        setContent {
            AppMiuixTheme {
                RecordsScreen(
                    isStandalone = true,
                    onBack = { finish() }
                )
            }
        }
    }
}