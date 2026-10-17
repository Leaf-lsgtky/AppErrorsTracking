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
package com.fankes.apperrorstracking.ui.screen.logs

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.ui.component.AdaptiveTopAppBar
import com.fankes.apperrorstracking.ui.component.BlurredBar
import com.fankes.apperrorstracking.ui.component.WideContentBox
import com.fankes.apperrorstracking.ui.component.horizontalCutoutPadding
import com.fankes.apperrorstracking.ui.component.rememberBlurBackdrop
import com.fankes.apperrorstracking.ui.theme.LocalStatusColors
import com.fankes.apperrorstracking.ui.viewmodel.LogsViewModel
import com.fankes.apperrorstracking.utils.factory.copyToClipboard
import com.fankes.apperrorstracking.utils.factory.toUtcTime
import com.fankes.apperrorstracking.utils.factory.toast
import com.fankes.apperrorstracking.utils.tool.ModuleLog
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun LogsScreen(
    bottomPadding: Dp = 0.dp,
    onBack: (() -> Unit)? = null,
    viewModel: LogsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val statusColors = LocalStatusColors.current

    var selectedLogForDialog by remember { mutableStateOf<ModuleLog.LogItem?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(viewModel.getExportContent().toByteArray())
                }
                context.toast(context.getString(R.string.export_all_logs_success))
            }.onFailure {
                context.toast(context.getString(R.string.export_all_logs_fail))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadLogs(context)
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = stringResource(R.string.debug_logs),
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = {
                        if (onBack != null) {
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
                        IconButton(
                            onClick = { viewModel.setFilterDialogOpen(true) }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Filter,
                                contentDescription = stringResource(R.string.filter_by_condition),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { viewModel.loadLogs(context) }
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                                tint = MiuixTheme.colorScheme.onSurface
                            )
                        }
                        if (uiState.filteredLogs.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val fileName = "app_errors_tracking_${System.currentTimeMillis().toUtcTime()}.log"
                                    exportLauncher.launch(fileName)
                                }
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Download,
                                    contentDescription = stringResource(R.string.export_all),
                                    tint = MiuixTheme.colorScheme.onSurface
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
                    .then(if (onBack != null) Modifier.horizontalCutoutPadding() else Modifier)
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = bottomPadding + 16.dp,
                    start = sidePadding,
                    end = sidePadding
                )
            ) {
                item {
                    SmallTitle(text = stringResource(R.string.this_contents_clear_when_restarts_tip))
                }

                if (uiState.filteredLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (uiState.filters.size < 4) {
                                        stringResource(R.string.no_list_result)
                                    } else {
                                        stringResource(R.string.no_list_data)
                                    },
                                    fontSize = 15.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = uiState.filteredLogs,
                        key = { index, item -> "${item.timestamp}_${index}" }
                    ) { _, item ->
                        var isExpanded by remember { mutableStateOf(false) }

                        val tagBgColor = when (item.priority) {
                            "D" -> statusColors.logDebug
                            "I" -> statusColors.logInfo
                            "W" -> statusColors.logWarn
                            "E" -> statusColors.logError
                            else -> statusColors.logDebug
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            onClick = { selectedLogForDialog = item }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // 优先级圆角胶囊
                                    Text(
                                        text = item.priority,
                                        modifier = Modifier
                                            .squircleBackground(color = tagBgColor, cornerRadius = 4.dp)
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                        color = statusColors.onLogTag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = SimpleDateFormat.getDateTimeInstance().format(Date(item.timestamp)),
                                        fontSize = 11.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = item.msg.replace("--", "\n--"),
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    lineHeight = 19.sp
                                )

                                if (item.throwable != null) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (isExpanded) "收起堆栈 ▲" else "展开堆栈 ▼",
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { isExpanded = !isExpanded }
                                            .padding(vertical = 4.dp)
                                    )
                                    AnimatedVisibility(visible = isExpanded) {
                                        Text(
                                            text = Log.getStackTraceString(item.throwable),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = statusColors.logError,
                                            lineHeight = 15.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .squircleBackground(
                                                    color = MiuixTheme.colorScheme.surfaceVariant,
                                                    cornerRadius = 8.dp
                                                )
                                                .padding(10.dp)
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

        // 筛选对话框
        var tempFilters by remember(uiState.filters) { mutableStateOf(uiState.filters) }
        OverlayDialog(
            show = uiState.isFilterDialogOpen,
            title = stringResource(R.string.filter_by_condition),
            insideMargin = DpSize(0.dp, 24.dp),
            onDismissRequest = { viewModel.setFilterDialogOpen(false) },
            content = {
                Column {
                    val levels = listOf(
                        "D" to "Debug (D)",
                        "I" to "Info (I)",
                        "W" to "Warn (W)",
                        "E" to "Error (E)",
                    )
                    levels.forEach { (level, label) ->
                        CheckboxPreference(
                            title = label,
                            checked = tempFilters.contains(level),
                            checkboxLocation = CheckboxLocation.End,
                            insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            onCheckedChange = { checked ->
                                tempFilters = if (checked) tempFilters + level else tempFilters - level
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.cancel),
                            onClick = { viewModel.setFilterDialogOpen(false) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TextButton(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            text = stringResource(R.string.confirm),
                            onClick = {
                                viewModel.setFilters(tempFilters)
                                viewModel.setFilterDialogOpen(false)
                            }
                        )
                    }
                }
            }
        )

        // 单条日志点击操作菜单弹窗
        val currentLog = selectedLogForDialog
        OverlayDialog(
            show = currentLog != null,
            title = if (currentLog != null) "[${currentLog.priority}] 日志详情" else "",
            onDismissRequest = { selectedLogForDialog = null },
            content = {
                if (currentLog != null) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = currentLog.msg.replace("--", "\n--"),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 6,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    context.copyToClipboard(currentLog.msg)
                                    context.toast(context.getString(R.string.copied))
                                    selectedLogForDialog = null
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Copy,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.copy_log_message),
                                fontSize = 15.sp,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                        if (currentLog.throwable != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val full = "${currentLog.msg}\n${Log.getStackTraceString(currentLog.throwable)}"
                                        context.copyToClipboard(full)
                                        context.toast(context.getString(R.string.copied))
                                        selectedLogForDialog = null
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Share,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.copy_stack_trace),
                                    fontSize = 15.sp,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
