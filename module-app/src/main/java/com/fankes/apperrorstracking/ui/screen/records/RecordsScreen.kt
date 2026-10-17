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
package com.fankes.apperrorstracking.ui.screen.records

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.bean.AppErrorsInfoBean
import com.fankes.apperrorstracking.ui.activity.errors.AppErrorsDetailActivity
import com.fankes.apperrorstracking.ui.component.AdaptiveTopAppBar
import com.fankes.apperrorstracking.ui.component.BlurredBar
import com.fankes.apperrorstracking.ui.component.WideContentBox
import com.fankes.apperrorstracking.ui.component.horizontalCutoutPadding
import com.fankes.apperrorstracking.ui.component.rememberBlurBackdrop
import com.fankes.apperrorstracking.ui.theme.LocalStatusColors
import com.fankes.apperrorstracking.ui.viewmodel.RecordsViewModel
import com.fankes.apperrorstracking.utils.factory.appIconOf
import com.fankes.apperrorstracking.utils.factory.appNameOf
import com.fankes.apperrorstracking.utils.factory.openSelfSetting
import com.fankes.apperrorstracking.utils.factory.toUtcTime
import com.fankes.apperrorstracking.utils.factory.toast
import kotlinx.coroutines.launch
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
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File
import java.io.FileInputStream

@Composable
fun RecordsScreen(
    bottomPadding: Dp = 0.dp,
    bottomBackdrop: LayerBackdrop? = null,
    isStandalone: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: RecordsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val topBackdrop = rememberBlurBackdrop()
    val blurActive = topBackdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val statusColors = LocalStatusColors.current

    var pendingZipFile by remember { mutableStateOf<File?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var selectedRecordForMenu by remember { mutableStateOf<AppErrorsInfoBean?>(null) }
    var showRecordDeleteConfirmDialog by remember { mutableStateOf<AppErrorsInfoBean?>(null) }

    // Export options
    var exportDeviceBrand by remember { mutableStateOf(true) }
    var exportDeviceModel by remember { mutableStateOf(true) }
    var exportDisplay by remember { mutableStateOf(true) }
    var exportPackageName by remember { mutableStateOf(true) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && pendingZipFile != null) {
            coroutineScope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        FileInputStream(pendingZipFile!!).use { fis ->
                            fis.copyTo(os)
                        }
                    }
                    pendingZipFile?.delete()
                    pendingZipFile = null
                    context.toast(context.getString(R.string.export_all_errors_success))
                }.onFailure {
                    context.toast(context.getString(R.string.export_all_errors_fail))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadRecords(context)
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = topBackdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.tab_records),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        if (isStandalone) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = stringResource(R.string.back),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    },
                    actions = {
                        if (uiState.records.size >= 5) {
                            IconButton(
                                onClick = { viewModel.calculateStatistics(context) }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Report,
                                    contentDescription = stringResource(R.string.app_errors_statistics),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        if (uiState.records.isNotEmpty()) {
                            IconButton(
                                onClick = { showExportOptionsDialog = true }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Download,
                                    contentDescription = stringResource(R.string.export_all),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(
                                onClick = { showClearDialog = true }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = stringResource(R.string.clear_all),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
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
                    .then(if (isStandalone) Modifier.horizontalCutoutPadding() else Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (topBackdrop != null) Modifier.layerBackdrop(topBackdrop) else Modifier)
                    .then(if (bottomBackdrop != null) Modifier.layerBackdrop(bottomBackdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (isStandalone) 24.dp else bottomPadding + 16.dp,
                    start = sidePadding,
                    end = sidePadding,
                )
            ) {
                item { Spacer(Modifier.height(12.dp)) }

                if (uiState.records.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.empty_records),
                                    fontSize = 16.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.records,
                        key = { it.timestamp }
                    ) { bean ->
                        val appIcon = remember(bean.packageName) {
                            runCatching {
                                context.appIconOf(bean.packageName).toBitmap().asImageBitmap()
                            }.getOrNull()
                        }
                        val appName = remember(bean.packageName) {
                            context.appNameOf(bean.packageName).ifBlank { bean.packageName }
                        }
                        val errorTypeLabel = if (bean.isNativeCrash) {
                            stringResource(R.string.app_type_native)
                        } else {
                            val cls = bean.exceptionClassName
                            if (cls.contains(".")) cls.substringAfterLast(".") else cls
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            AppErrorsDetailActivity.start(context, bean)
                                        },
                                        onLongClick = {
                                            selectedRecordForMenu = bean
                                        }
                                    )
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f, fill = false),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = appName,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MiuixTheme.colorScheme.onSurface,
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                val badgeColor = if (bean.isNativeCrash) statusColors.inactiveContainer else MiuixTheme.colorScheme.secondaryContainer
                                                val badgeTextColor = if (bean.isNativeCrash) statusColors.onInactiveContainer else MiuixTheme.colorScheme.onSecondaryContainer
                                                Box(
                                                    modifier = Modifier
                                                        .squircleBackground(badgeColor, 4.dp)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = errorTypeLabel,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = badgeTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (bean.userId > 0) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .squircleBackground(
                                                                MiuixTheme.colorScheme.secondaryContainer,
                                                                4.dp
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "UID:${bean.userId}",
                                                            fontSize = 10.sp,
                                                            color = MiuixTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            Text(
                                                text = bean.crossTime(context),
                                                fontSize = 12.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                                maxLines = 1,
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))

                                        val errorSummary = if (bean.exceptionMessage.isNotBlank() && bean.exceptionMessage != "unknown") {
                                            bean.exceptionMessage
                                        } else {
                                            bean.exceptionClassName.ifBlank { "未知异常" }
                                        }
                                        Text(
                                            text = errorSummary,
                                            fontSize = 13.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 17.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (isStandalone) {
                    item {
                        Spacer(Modifier.height(24.dp).navigationBarsPadding())
                    }
                }
            }
        }

    // 统计分析 Dialog
    var lastStats by remember { mutableStateOf(uiState.statistics) }
    if (uiState.statistics != null) lastStats = uiState.statistics
    OverlayDialog(
        show = uiState.statistics != null,
        title = stringResource(R.string.app_errors_statistics),
        onDismissRequest = { viewModel.clearStatistics() }
    ) {
        val stats = lastStats
        if (stats != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(
                    text = stringResource(R.string.total_errors_unit, stats.totalErrors),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.total_apps_unit, stats.totalApps),
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.app_info)}: ${context.appNameOf(stats.mostErrorsPackageName).ifBlank { stats.mostErrorsPackageName }}",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.app_details)}: ${stats.mostErrorsType}",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.app_errors_statistics)}: ${stats.errorsPpt}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                TextButton(
                    text = stringResource(R.string.got_it),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = { viewModel.clearStatistics() }
                )
            }
        }
    }

    // 单项操作菜单 Dialog
    var lastSelectedRecord by remember { mutableStateOf(selectedRecordForMenu) }
    if (selectedRecordForMenu != null) lastSelectedRecord = selectedRecordForMenu
    val menuBean = lastSelectedRecord
    OverlayDialog(
        show = selectedRecordForMenu != null,
        title = if (menuBean != null) context.appNameOf(menuBean.packageName).ifBlank { menuBean.packageName } else "",
        onDismissRequest = { selectedRecordForMenu = null }
    ) {
        if (menuBean != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ArrowPreference(
                    title = stringResource(R.string.view_detail),
                    onClick = {
                        val target = menuBean
                        selectedRecordForMenu = null
                        AppErrorsDetailActivity.start(context, target)
                    }
                )
                ArrowPreference(
                    title = stringResource(R.string.app_info),
                    onClick = {
                        val pkg = menuBean.packageName
                        selectedRecordForMenu = null
                        context.openSelfSetting(pkg)
                    }
                )
                ArrowPreference(
                    title = stringResource(R.string.remove_record),
                    onClick = {
                        val target = menuBean
                        selectedRecordForMenu = null
                        showRecordDeleteConfirmDialog = target
                    }
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedRecordForMenu = null }
                )
            }
        }
    }

    // 单项删除确认
    var lastDeleteTarget by remember { mutableStateOf(showRecordDeleteConfirmDialog) }
    if (showRecordDeleteConfirmDialog != null) lastDeleteTarget = showRecordDeleteConfirmDialog
    val deleteTarget = lastDeleteTarget
    OverlayDialog(
        show = showRecordDeleteConfirmDialog != null,
        title = stringResource(R.string.notice),
        onDismissRequest = { showRecordDeleteConfirmDialog = null }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.are_you_sure_remove_record),
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showRecordDeleteConfirmDialog = null },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        if (deleteTarget != null) {
                            viewModel.removeRecord(context, deleteTarget) {
                                showRecordDeleteConfirmDialog = null
                                context.toast(context.getString(R.string.confirm))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }

    // 清空全部 Dialog
    OverlayDialog(
        show = showClearDialog,
        title = stringResource(R.string.notice),
        onDismissRequest = { showClearDialog = false }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.are_you_sure_clear_errors),
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showClearDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        viewModel.clearAllRecords(context) {
                            showClearDialog = false
                            context.toast(context.getString(R.string.all_errors_clear_success))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }

    // 导出全部 Dialog
    OverlayDialog(
        show = showExportOptionsDialog,
        title = stringResource(R.string.export_all),
        insideMargin = DpSize(0.dp, 24.dp),
        onDismissRequest = { showExportOptionsDialog = false }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.stack_trace_share_top_tip),
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            CheckboxPreference(
                title = stringResource(R.string.stack_trace_share_device_brand),
                checked = exportDeviceBrand,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { exportDeviceBrand = it }
            )
            CheckboxPreference(
                title = stringResource(R.string.stack_trace_share_device_model),
                checked = exportDeviceModel,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { exportDeviceModel = it }
            )
            CheckboxPreference(
                title = stringResource(R.string.stack_trace_share_system_build_id),
                checked = exportDisplay,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { exportDisplay = it }
            )
            CheckboxPreference(
                title = stringResource(R.string.stack_trace_share_package_name),
                checked = exportPackageName,
                checkboxLocation = CheckboxLocation.End,
                insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onCheckedChange = { exportPackageName = it }
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
                    onClick = { showExportOptionsDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(16.dp))
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = {
                        showExportOptionsDialog = false
                        coroutineScope.launch {
                            val zip = viewModel.prepareExportZip(
                                context = context,
                                sDeviceBrand = exportDeviceBrand,
                                sDeviceModel = exportDeviceModel,
                                sDisplay = exportDisplay,
                                sPackageName = exportPackageName
                            )
                            if (zip != null) {
                                pendingZipFile = zip
                                exportLauncher.launch("app_errors_${System.currentTimeMillis().toUtcTime()}.zip")
                            } else {
                                context.toast(context.getString(R.string.export_all_errors_fail))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
}
