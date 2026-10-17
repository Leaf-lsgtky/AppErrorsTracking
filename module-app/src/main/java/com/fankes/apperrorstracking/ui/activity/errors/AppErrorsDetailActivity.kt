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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import com.fankes.apperrorstracking.bean.AppErrorsInfoBean
import com.fankes.apperrorstracking.ui.screen.detail.DetailScreen
import com.fankes.apperrorstracking.ui.theme.AppMiuixTheme
import com.fankes.apperrorstracking.utils.factory.getSerializableExtraCompat
import com.fankes.apperrorstracking.utils.factory.navigate
import com.fankes.apperrorstracking.utils.factory.toast

class AppErrorsDetailActivity : AppCompatActivity() {

    companion object {
        /** [AppErrorsInfoBean] 传值 */
        private const val EXTRA_APP_ERRORS_INFO = "app_errors_info_extra"

        /**
         * 启动 [AppErrorsDetailActivity]
         * @param context 实例
         * @param appErrorsInfo 应用异常信息
         */
        fun start(context: Context, appErrorsInfo: AppErrorsInfoBean) =
            context.navigate<AppErrorsDetailActivity> { putExtra(EXTRA_APP_ERRORS_INFO, appErrorsInfo) }
    }

    private val currentErrorState = mutableStateOf<AppErrorsInfoBean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()

        val appErrorsInfo = parseErrorFromIntent(intent)
        if (appErrorsInfo == null || appErrorsInfo.isEmpty) {
            toast("Unable to get app error details")
            finish()
            return
        }
        currentErrorState.value = appErrorsInfo

        setContent {
            AppMiuixTheme {
                val error = currentErrorState.value
                if (error != null && !error.isEmpty) {
                    key(error.timestamp, error.pid) {
                        DetailScreen(
                            initialError = error,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val appErrorsInfo = parseErrorFromIntent(intent)
        if (appErrorsInfo != null && !appErrorsInfo.isEmpty) {
            currentErrorState.value = appErrorsInfo
        }
    }

    private fun parseErrorFromIntent(intent: Intent?): AppErrorsInfoBean? {
        return runCatching {
            intent?.getSerializableExtraCompat<AppErrorsInfoBean>(EXTRA_APP_ERRORS_INFO)
        }.getOrNull()
    }
}