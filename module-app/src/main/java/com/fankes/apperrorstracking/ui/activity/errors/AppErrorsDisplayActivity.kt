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
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.bean.AppErrorsDisplayBean
import com.fankes.apperrorstracking.ui.theme.AppMiuixTheme
import com.fankes.apperrorstracking.utils.factory.getSerializableExtraCompat
import com.fankes.apperrorstracking.utils.factory.navigate
import com.fankes.apperrorstracking.utils.factory.openSelfSetting
import com.fankes.apperrorstracking.utils.factory.toast
import com.fankes.apperrorstracking.utils.tool.FrameworkTool
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

import android.content.Intent
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf

class AppErrorsDisplayActivity : AppCompatActivity() {

    companion object {
        /** 当前实例 - 单例运行 */
        private var instance: AppErrorsDisplayActivity? = null

        /** [AppErrorsDisplayBean] 传值 */
        private const val EXTRA_APP_ERRORS_DISPLAY = "app_errors_display_extra"

        /**
         * 启动 [AppErrorsDisplayActivity]
         * @param context 实例
         * @param appErrorsDisplay 应用异常信息显示
         */
        fun start(context: Context, appErrorsDisplay: AppErrorsDisplayBean) =
            context.navigate<AppErrorsDisplayActivity>(isOutSide = true) {
                putExtra(EXTRA_APP_ERRORS_DISPLAY, appErrorsDisplay)
            }
    }

    private val currentDisplayState = mutableStateOf<AppErrorsDisplayBean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance?.finish()
        instance = this

        supportActionBar?.hide()
        enableEdgeToEdge()

        val appErrorsDisplay = parseDisplayFromIntent(intent)
        if (appErrorsDisplay == null) {
            toast("Invalid AppErrorsDisplay, exit")
            finish()
            return
        }
        currentDisplayState.value = appErrorsDisplay

        setContent {
            AppMiuixTheme {
                val display = currentDisplayState.value
                if (display != null) {
                    key(display.pid, display.packageName) {
                        AppErrorsDisplayDialog(
                            displayBean = display,
                            onDismiss = { finish() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val appErrorsDisplay = parseDisplayFromIntent(intent)
        if (appErrorsDisplay != null) {
            currentDisplayState.value = appErrorsDisplay
        }
    }

    private fun parseDisplayFromIntent(intent: Intent?): AppErrorsDisplayBean? {
        return runCatching {
            intent?.getSerializableExtraCompat<AppErrorsDisplayBean>(EXTRA_APP_ERRORS_DISPLAY)
        }.getOrNull()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    @Composable
    private fun AppErrorsDisplayDialog(
        displayBean: AppErrorsDisplayBean,
        onDismiss: () -> Unit,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WindowDialog(
                show = true,
                title = displayBean.title,
                onDismissRequest = onDismiss
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (displayBean.packageName != displayBean.processName) {
                        Text(
                            text = stringResource(R.string.crash_process, displayBean.processName),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    if (displayBean.isShowReopenButton) {
                        ArrowPreference(
                            title = stringResource(R.string.reopen_app),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = {
                                FrameworkTool.openAppUsedFramework(
                                    this@AppErrorsDisplayActivity,
                                    displayBean.packageName,
                                    displayBean.userId
                                )
                                onDismiss()
                            }
                        )
                    }

                    if (!displayBean.isShowReopenButton && displayBean.isShowCloseAppButton) {
                        ArrowPreference(
                            title = stringResource(R.string.close_app),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = onDismiss
                        )
                    }

                    ArrowPreference(
                        title = stringResource(R.string.error_detail),
                        startAction = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Article,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        onClick = {
                            FrameworkTool.fetchAppErrorInfoData(this@AppErrorsDisplayActivity, displayBean.pid) { appErrorsInfo ->
                                AppErrorsDetailActivity.start(this@AppErrorsDisplayActivity, appErrorsInfo)
                                onDismiss()
                            }
                        }
                    )

                    if (displayBean.isShowAppInfoButton) {
                        ArrowPreference(
                            title = stringResource(R.string.app_info),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = {
                                openSelfSetting(displayBean.packageName)
                                onDismiss()
                            }
                        )
                    }

                    ArrowPreference(
                        title = stringResource(R.string.mute_if_unlock),
                        startAction = {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        onClick = {
                            FrameworkTool.mutedErrorsIfUnlock(this@AppErrorsDisplayActivity, displayBean.packageName) {
                                toast(getString(R.string.mute_if_unlock_tip, displayBean.appName))
                                onDismiss()
                            }
                        }
                    )

                    ArrowPreference(
                        title = stringResource(R.string.mute_if_restart),
                        startAction = {
                            Icon(
                                imageVector = Icons.Rounded.PowerSettingsNew,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        onClick = {
                            FrameworkTool.mutedErrorsIfRestart(this@AppErrorsDisplayActivity, displayBean.packageName) {
                                toast(getString(R.string.mute_if_restart_tip, displayBean.appName))
                                onDismiss()
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}