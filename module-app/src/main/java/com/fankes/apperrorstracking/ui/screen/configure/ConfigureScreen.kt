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
package com.fankes.apperrorstracking.ui.screen.configure

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.bean.AppFiltersBean
import com.fankes.apperrorstracking.bean.AppInfoBean
import com.fankes.apperrorstracking.bean.enum.AppFiltersType
import com.fankes.apperrorstracking.data.AppErrorsConfigData
import com.fankes.apperrorstracking.data.enum.AppErrorsConfigType
import com.fankes.apperrorstracking.ui.component.AdaptiveTopAppBar
import com.fankes.apperrorstracking.ui.component.BlurredBar
import com.fankes.apperrorstracking.ui.component.WideContentBox
import com.fankes.apperrorstracking.ui.component.horizontalCutoutPadding
import com.fankes.apperrorstracking.ui.component.rememberBlurBackdrop
import com.fankes.apperrorstracking.utils.factory.appIconOf
import com.fankes.apperrorstracking.utils.factory.toast
import com.fankes.apperrorstracking.utils.tool.FrameworkTool
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ConfigureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appsList = remember { mutableStateListOf<AppInfoBean>() }
    var isLoading by remember { mutableStateOf(true) }
    var appFilters by remember { mutableStateOf(AppFiltersBean()) }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface

    var showGlobalDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedAppForConfig by remember { mutableStateOf<AppInfoBean?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    fun refresh() {
        isLoading = true
        FrameworkTool.fetchAppListData(context, appFilters) { list ->
            appsList.clear()
            appsList.addAll(list)
            isLoading = false
        }
    }

    LaunchedEffect(appFilters, refreshTrigger) {
        refresh()
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.apps_config_template),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = stringResource(R.string.back),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showGlobalDialog = true }) {
                            Icon(
                                imageVector = MiuixIcons.Settings,
                                contentDescription = stringResource(R.string.global_config),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        if (appsList.isNotEmpty()) {
                            IconButton(onClick = { showBatchDialog = true }) {
                                Icon(
                                    imageVector = MiuixIcons.Layers,
                                    contentDescription = stringResource(R.string.batch_operations),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                imageVector = MiuixIcons.Filter,
                                contentDescription = stringResource(R.string.filter_by_condition),
                                tint = MiuixTheme.colorScheme.onSurface,
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
                    .horizontalCutoutPadding()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    start = sidePadding,
                    end = sidePadding,
                )
            ) {
                item { Spacer(Modifier.height(12.dp)) }

                if (appsList.isEmpty() && !isLoading) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_apps),
                                    fontSize = 16.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = appsList,
                        key = { it.packageName }
                    ) { bean ->
                        val appIcon = remember(bean.packageName) {
                            runCatching {
                                context.appIconOf(bean.packageName).toBitmap().asImageBitmap()
                            }.getOrNull()
                        }
                        val configTypeLabel = when {
                            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.GLOBAL, bean.packageName) ->
                                stringResource(R.string.follow_global_config)
                            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.DIALOG, bean.packageName) ->
                                stringResource(R.string.show_errors_dialog)
                            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTIFY, bean.packageName) ->
                                stringResource(R.string.show_errors_notify)
                            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.TOAST, bean.packageName) ->
                                stringResource(R.string.show_errors_toast)
                            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTHING, bean.packageName) ->
                                stringResource(R.string.show_nothing)
                            else -> "Unknown"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAppForConfig = bean }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .squircleBackground(MiuixTheme.colorScheme.surfaceVariant, 10.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bean.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = bean.packageName,
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .squircleBackground(MiuixTheme.colorScheme.secondaryContainer, 4.dp)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = configTypeLabel,
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(24.dp).navigationBarsPadding())
                }
            }
        }

    // 单个 App 配置弹窗
    var lastSelectedApp by remember { mutableStateOf(selectedAppForConfig) }
    if (selectedAppForConfig != null) lastSelectedApp = selectedAppForConfig
    val app = lastSelectedApp
    ConfigTypeChooserDialog(
        show = selectedAppForConfig != null,
        title = app?.name ?: "",
        allowGlobal = true,
        currentSelected = when {
            app == null -> AppErrorsConfigType.GLOBAL
            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.GLOBAL, app.packageName) -> AppErrorsConfigType.GLOBAL
            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.DIALOG, app.packageName) -> AppErrorsConfigType.DIALOG
            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTIFY, app.packageName) -> AppErrorsConfigType.NOTIFY
            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.TOAST, app.packageName) -> AppErrorsConfigType.TOAST
            else -> AppErrorsConfigType.NOTHING
        },
        onDismiss = { selectedAppForConfig = null },
        onConfirm = { type ->
            if (app != null) {
                AppErrorsConfigData.putAppShowingType(type, app.packageName)
                FrameworkTool.refreshFrameworkPrefsData(context)
                selectedAppForConfig = null
                refreshTrigger++
            }
        }
    )

    // 全局配置弹窗
    ConfigTypeChooserDialog(
        show = showGlobalDialog,
        title = stringResource(R.string.global_config),
        allowGlobal = false,
        currentSelected = when {
            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.DIALOG) -> AppErrorsConfigType.DIALOG
            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.NOTIFY) -> AppErrorsConfigType.NOTIFY
            AppErrorsConfigData.isAppShowingType(AppErrorsConfigType.TOAST) -> AppErrorsConfigType.TOAST
            else -> AppErrorsConfigType.NOTHING
        },
        onDismiss = { showGlobalDialog = false },
        onConfirm = { type ->
            AppErrorsConfigData.putAppShowingType(type)
            FrameworkTool.refreshFrameworkPrefsData(context)
            showGlobalDialog = false
            refreshTrigger++
            context.toast(context.getString(R.string.confirm))
        }
    )

    // 批量配置弹窗
    ConfigTypeChooserDialog(
        show = showBatchDialog,
        title = stringResource(R.string.batch_operations_number, appsList.size),
        allowGlobal = true,
        currentSelected = AppErrorsConfigType.GLOBAL,
        onDismiss = { showBatchDialog = false },
        onConfirm = { type ->
            appsList.forEach { appItem ->
                AppErrorsConfigData.putAppShowingType(type, appItem.packageName)
            }
            FrameworkTool.refreshFrameworkPrefsData(context)
            showBatchDialog = false
            refreshTrigger++
            context.toast(context.getString(R.string.confirm))
        }
    )

    // 过滤与搜索条件弹窗
    var tempType by remember(appFilters.type) { mutableStateOf(appFilters.type) }
    var tempKeyword by remember(appFilters.name) { mutableStateOf(appFilters.name) }

    OverlayDialog(
        show = showFilterDialog,
        title = stringResource(R.string.filter_by_condition),
        insideMargin = DpSize(0.dp, 24.dp),
        onDismissRequest = { showFilterDialog = false }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CheckboxPreference(
                title = stringResource(R.string.user_apps),
                checked = tempType == AppFiltersType.USER,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { if (it) tempType = AppFiltersType.USER }
            )
            CheckboxPreference(
                title = stringResource(R.string.system_apps),
                checked = tempType == AppFiltersType.SYSTEM,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { if (it) tempType = AppFiltersType.SYSTEM }
            )
            CheckboxPreference(
                title = stringResource(R.string.all_apps),
                checked = tempType == AppFiltersType.ALL,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { if (it) tempType = AppFiltersType.ALL }
            )
            Spacer(Modifier.height(10.dp))
            TextField(
                value = tempKeyword,
                onValueChange = { tempKeyword = it },
                label = stringResource(R.string.keyword_search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(16.dp))
            if (tempKeyword.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.confirm),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                        onClick = {
                            appFilters = AppFiltersBean(type = tempType, name = tempKeyword.trim())
                            showFilterDialog = false
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        text = stringResource(R.string.clear_filters),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            tempKeyword = ""
                            appFilters = AppFiltersBean(type = tempType, name = "")
                            showFilterDialog = false
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showFilterDialog = false }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { showFilterDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    TextButton(
                        text = stringResource(R.string.confirm),
                        onClick = {
                            appFilters = AppFiltersBean(type = tempType, name = tempKeyword.trim())
                            showFilterDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
}
}

@Composable
private fun ConfigTypeChooserDialog(
    show: Boolean,
    title: String,
    allowGlobal: Boolean,
    currentSelected: AppErrorsConfigType,
    onDismiss: () -> Unit,
    onConfirm: (AppErrorsConfigType) -> Unit,
) {
    var selected by remember(currentSelected, show) { mutableStateOf(currentSelected) }

    OverlayDialog(
        show = show,
        title = title,
        insideMargin = DpSize(0.dp, 24.dp),
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (allowGlobal) {
                CheckboxPreference(
                    title = stringResource(R.string.follow_global_config),
                    checked = selected == AppErrorsConfigType.GLOBAL,
                    checkboxLocation = CheckboxLocation.End,
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    onCheckedChange = { if (it) selected = AppErrorsConfigType.GLOBAL }
                )
            }
            CheckboxPreference(
                title = stringResource(R.string.show_errors_dialog),
                checked = selected == AppErrorsConfigType.DIALOG,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { if (it) selected = AppErrorsConfigType.DIALOG }
            )
            CheckboxPreference(
                title = stringResource(R.string.show_errors_notify),
                checked = selected == AppErrorsConfigType.NOTIFY,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { if (it) selected = AppErrorsConfigType.NOTIFY }
            )
            CheckboxPreference(
                title = stringResource(R.string.show_errors_toast),
                checked = selected == AppErrorsConfigType.TOAST,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { if (it) selected = AppErrorsConfigType.TOAST }
            )
            CheckboxPreference(
                title = stringResource(R.string.show_nothing),
                checked = selected == AppErrorsConfigType.NOTHING,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { if (it) selected = AppErrorsConfigType.NOTHING }
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = { onConfirm(selected) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
