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
package com.fankes.apperrorstracking.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.const.ModuleVersion
import com.fankes.apperrorstracking.ui.activity.errors.AppErrorsMutedActivity
import com.fankes.apperrorstracking.ui.activity.main.ConfigureActivity
import com.fankes.apperrorstracking.ui.component.AdaptiveTopAppBar
import com.fankes.apperrorstracking.ui.component.BlurredBar
import com.fankes.apperrorstracking.ui.component.WideContentBox
import com.fankes.apperrorstracking.ui.component.rememberBlurBackdrop
import com.fankes.apperrorstracking.ui.viewmodel.SettingsViewModel
import com.fankes.apperrorstracking.utils.factory.navigate
import com.fankes.apperrorstracking.utils.factory.openBrowser
import com.fankes.apperrorstracking.utils.factory.toast
import com.fankes.apperrorstracking.utils.tool.GithubReleaseTool
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

import androidx.compose.material.icons.automirrored.rounded.Article
import com.fankes.apperrorstracking.ui.activity.debug.LoggerActivity
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun SettingsScreen(
    bottomPadding: Dp,
    bottomBackdrop: LayerBackdrop? = null,
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    var showDeveloperNoticeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshSettings(context)
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.tab_settings),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                )
            }
        }
    ) { innerPadding ->
        WideContentBox { sidePadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .then(if (bottomBackdrop != null) Modifier.layerBackdrop(bottomBackdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding + 16.dp,
                    start = sidePadding,
                    end = sidePadding
                )
            ) {
                // 异常拦截设置
                item {
                    SmallTitle(text = stringResource(R.string.settings_interception))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.only_show_errors_in_front),
                            summary = stringResource(R.string.only_show_errors_in_front_tip),
                            checked = uiState.onlyShowErrorsInFront,
                            onCheckedChange = { viewModel.setOnlyShowErrorsInFront(it) }
                        )
                        SwitchPreference(
                            title = stringResource(R.string.only_show_errors_in_main_process),
                            summary = stringResource(R.string.only_show_errors_in_main_process_tip),
                            checked = uiState.onlyShowErrorsInMain,
                            onCheckedChange = { viewModel.setOnlyShowErrorsInMain(it) }
                        )
                        SwitchPreference(
                            title = stringResource(R.string.errors_dialog_always_show_reopen),
                            summary = stringResource(R.string.errors_dialog_always_show_reopen_tip),
                            checked = uiState.alwaysShowsReopenAppOptions,
                            onCheckedChange = { viewModel.setAlwaysShowsReopenAppOptions(it) }
                        )
                        SwitchPreference(
                            title = stringResource(R.string.errors_dialog_prevent_misoperation),
                            summary = stringResource(R.string.errors_dialog_prevent_misoperation_tip),
                            checked = uiState.preventMisoperation,
                            onCheckedChange = { viewModel.setPreventMisoperation(it) }
                        )
                    }
                }

                // 分享与导出
                item {
                    SmallTitle(text = stringResource(R.string.settings_sharing))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.share_with_file),
                            summary = stringResource(R.string.share_with_file_tip),
                            checked = uiState.shareWithFile,
                            onCheckedChange = { viewModel.setShareWithFile(it) }
                        )
                    }
                }

                // 外观与显示
                item {
                    SmallTitle(text = stringResource(R.string.settings_appearance))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.enable_md3_app_errors_dialog),
                            summary = stringResource(R.string.enable_md3_app_errors_dialog_tip),
                            checked = uiState.material3StyleDialog,
                            onCheckedChange = { viewModel.setMaterial3StyleDialog(it) }
                        )
                        SwitchPreference(
                            title = stringResource(R.string.hide_app_icon_on_launcher),
                            summary = stringResource(R.string.hide_app_icon_on_launcher_tip),
                            checked = uiState.hideLauncherIcon,
                            onCheckedChange = { viewModel.setHideLauncherIcon(context, it) }
                        )
                    }
                }

                // 功能管理
                item {
                    SmallTitle(text = stringResource(R.string.settings_functions))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        SwitchPreference(
                            title = stringResource(R.string.enable_apps_config_template),
                            summary = stringResource(R.string.apps_config_template_tip),
                            checked = uiState.enableAppConfigTemplate,
                            onCheckedChange = { viewModel.setEnableAppConfigTemplate(it) }
                        )
                        AnimatedVisibility(visible = uiState.enableAppConfigTemplate) {
                            ArrowPreference(
                                title = stringResource(R.string.mgr_apps_config_template),
                                summary = stringResource(R.string.apps_config_template),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                onClick = { context.navigate<ConfigureActivity>() }
                            )
                        }
                        ArrowPreference(
                            title = stringResource(R.string.view_muted_errors_apps),
                            summary = stringResource(R.string.view_muted_errors_apps_tip),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = { context.navigate<AppErrorsMutedActivity>() }
                        )
                        if (uiState.isAnalyticsAvailable) {
                            SwitchPreference(
                                title = stringResource(R.string.enable_anonymous_statistics),
                                summary = stringResource(R.string.enable_anonymous_statistics_tip),
                                checked = uiState.enableAnonymousStatistics,
                                onCheckedChange = { viewModel.setEnableAnonymousStatistics(it) }
                            )
                        }
                    }
                }

                // 关于与支持
                item {
                    SmallTitle(text = stringResource(R.string.settings_about))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.click_to_update, ModuleVersion.NAME),
                            summary = "检查 GitHub Releases 最新版本",
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.SystemUpdate,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = {
                                GithubReleaseTool.checkingForUpdate(context, ModuleVersion.NAME) { _, action ->
                                    action()
                                }
                                context.toast("正在检查更新...")
                            }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.project_address),
                            summary = "https://github.com/KitsunePie/AppErrorsTracking",
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Code,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = { context.openBrowser("https://github.com/KitsunePie/AppErrorsTracking") }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.debug_logs),
                            summary = stringResource(R.string.debug_logs_summary),
                            startAction = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Article,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = { context.navigate<LoggerActivity>() }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.developer_notice),
                            summary = "使用说明及项目初衷",
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = { showDeveloperNoticeDialog = true }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp).navigationBarsPadding())
                }
            }
        }

        OverlayDialog(
            show = showDeveloperNoticeDialog,
            title = stringResource(R.string.developer_notice),
            onDismissRequest = { showDeveloperNoticeDialog = false },
            content = {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.developer_notice_tip),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.got_it),
                        onClick = { showDeveloperNoticeDialog = false }
                    )
                }
            }
        )
    }
}
