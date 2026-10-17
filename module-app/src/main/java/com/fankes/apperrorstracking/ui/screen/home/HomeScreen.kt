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
package com.fankes.apperrorstracking.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.ui.activity.errors.AppErrorsMutedActivity
import com.fankes.apperrorstracking.ui.activity.errors.AppErrorsRecordActivity
import com.fankes.apperrorstracking.ui.activity.main.ConfigureActivity
import com.fankes.apperrorstracking.ui.component.AdaptiveTopAppBar
import com.fankes.apperrorstracking.ui.component.BlurredBar
import com.fankes.apperrorstracking.ui.component.WideContentBox
import com.fankes.apperrorstracking.ui.component.rememberBlurBackdrop
import com.fankes.apperrorstracking.ui.theme.LocalStatusColors
import com.fankes.apperrorstracking.ui.viewmodel.HomeViewModel
import com.fankes.apperrorstracking.utils.factory.isSystemLanguageSimplifiedChinese
import com.fankes.apperrorstracking.utils.factory.navigate
import com.fankes.apperrorstracking.utils.factory.toast
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

import top.yukonga.miuix.kmp.blur.LayerBackdrop
import com.fankes.apperrorstracking.ui.component.MainTab

@Composable
fun HomeScreen(
    bottomPadding: Dp,
    bottomBackdrop: LayerBackdrop? = null,
    onNavigateToTab: ((MainTab) -> Unit)? = null,
    viewModel: HomeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val statusColors = LocalStatusColors.current

    var showSystemInfoDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showDeveloperNoticeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.app_name),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    actions = {
                        IconButton(
                            onClick = { showRestartDialog = true }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Reset,
                                contentDescription = stringResource(R.string.restart_system),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { viewModel.openGithub(context) }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Info,
                                contentDescription = stringResource(R.string.project_address),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
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
                item { Spacer(Modifier.height(12.dp)) }

                // 模块状态卡片
                item {
                    val isFullyActive = uiState.isModuleActive && uiState.isModuleValid
                    val isActive = uiState.isModuleActive

                    val containerColor = when {
                        isFullyActive -> statusColors.activeContainer
                        isActive -> statusColors.notFullyActiveContainer
                        else -> statusColors.inactiveContainer
                    }
                    val onContainerColor = when {
                        isFullyActive -> statusColors.onActiveContainer
                        isActive -> statusColors.onNotFullyActiveContainer
                        else -> statusColors.onInactiveContainer
                    }
                    val iconColor = when {
                        isFullyActive -> statusColors.activeIcon
                        isActive -> statusColors.notFullyActiveIcon
                        else -> statusColors.inactiveIcon
                    }
                    val watermarkIcon = when {
                        isFullyActive -> Icons.Rounded.CheckCircleOutline
                        isActive -> Icons.Rounded.WarningAmber
                        else -> Icons.Rounded.ErrorOutline
                    }
                    val statusTitle = when {
                        isFullyActive -> stringResource(R.string.module_is_activated)
                        isActive -> stringResource(R.string.module_not_fully_activated)
                        else -> stringResource(R.string.module_not_activated)
                    }
                    val statusSummary = when {
                        isFullyActive -> uiState.frameworkApiInfo.ifBlank { stringResource(R.string.active_by_libxposed) }
                        isActive -> stringResource(R.string.module_not_fully_activated_tip)
                        else -> stringResource(R.string.not_active_summary)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = containerColor),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // 水印背景
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(x = 24.dp, y = 24.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Icon(
                                    imageVector = watermarkIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(110.dp),
                                    tint = iconColor.copy(alpha = 0.22f)
                                )
                            }
                            // 前景内容
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = watermarkIcon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = statusTitle,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = onContainerColor,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = statusSummary,
                                    fontSize = 13.sp,
                                    color = onContainerColor.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                // 快捷功能入口
                item {
                    SmallTitle(text = stringResource(R.string.quick_actions))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        ArrowPreference(
                            title = stringResource(R.string.errors_record),
                            summary = stringResource(R.string.view_errors_record),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = {
                                if (uiState.isModuleActive) {
                                    if (onNavigateToTab != null) {
                                        onNavigateToTab(MainTab.Records)
                                    } else {
                                        context.navigate<AppErrorsRecordActivity>()
                                    }
                                } else {
                                    context.toast(context.getString(R.string.module_not_activated))
                                }
                            }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.muted_errors_apps),
                            summary = stringResource(R.string.view_muted_errors_apps),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = {
                                if (uiState.isModuleActive) {
                                    context.navigate<AppErrorsMutedActivity>()
                                } else {
                                    context.toast(context.getString(R.string.module_not_activated))
                                }
                            }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.apps_config_template),
                            summary = stringResource(R.string.mgr_apps_config_template),
                            startAction = {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            onClick = {
                                if (uiState.isModuleActive) {
                                    context.navigate<ConfigureActivity>()
                                } else {
                                    context.toast(context.getString(R.string.module_not_activated))
                                }
                            }
                        )
                    }
                }

                // 系统与运行环境信息
                item {
                    SmallTitle(text = stringResource(R.string.app_info))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.module_version, uiState.moduleVersion),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            if (uiState.isCiMode) {
                                Text(
                                    text = "CI Commit: ${uiState.ciCommitId}",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.system_version, uiState.systemVersion),
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.clickable { showSystemInfoDialog = true }
                            )
                        }
                    }
                }

                // 开发者与项目链接
                item {
                    SmallTitle(text = stringResource(R.string.settings_about))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        if (isSystemLanguageSimplifiedChinese) {
                            ArrowPreference(
                                title = stringResource(R.string.follow_developer),
                                summary = stringResource(R.string.follow_developer_summary),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                onClick = { viewModel.openCoolapk(context) }
                            )
                        }
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
                            onClick = { viewModel.openGithub(context) }
                        )
                        ArrowPreference(
                            title = stringResource(R.string.developer_notice),
                            summary = stringResource(R.string.developer_notice_tip).take(36) + "...",
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

        // 系统详情弹窗
        OverlayDialog(
            show = showSystemInfoDialog,
            title = stringResource(R.string.notice),
            onDismissRequest = { showSystemInfoDialog = false },
            content = {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = uiState.systemVersion,
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.got_it),
                        onClick = { showSystemInfoDialog = false }
                    )
                }
            }
        )

        // 重启系统确认弹窗
        OverlayDialog(
            show = showRestartDialog,
            title = stringResource(R.string.restart_system),
            onDismissRequest = { showRestartDialog = false },
            content = {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.are_your_sure_restart_system),
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.cancel),
                            onClick = { showRestartDialog = false }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            text = stringResource(R.string.confirm),
                            onClick = {
                                showRestartDialog = false
                                viewModel.restartSystem(context)
                            }
                        )
                    }
                }
            }
        )

        // 开发者说明弹窗
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
