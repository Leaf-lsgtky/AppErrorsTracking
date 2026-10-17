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
package com.fankes.apperrorstracking.ui.screen.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.fankes.apperrorstracking.R
import com.fankes.apperrorstracking.bean.AppErrorsInfoBean
import com.fankes.apperrorstracking.data.ConfigData
import com.fankes.apperrorstracking.ui.component.AdaptiveTopAppBar
import com.fankes.apperrorstracking.ui.component.BlurredBar
import com.fankes.apperrorstracking.ui.component.WideContentBox
import com.fankes.apperrorstracking.ui.component.horizontalCutoutPadding
import com.fankes.apperrorstracking.ui.component.rememberBlurBackdrop
import com.fankes.apperrorstracking.ui.theme.LocalStatusColors
import com.fankes.apperrorstracking.utils.factory.appIconOf
import com.fankes.apperrorstracking.utils.factory.appNameOf
import com.fankes.apperrorstracking.utils.factory.copyToClipboard
import com.fankes.apperrorstracking.utils.factory.openSelfSetting
import com.fankes.apperrorstracking.utils.factory.toast
import com.fankes.apperrorstracking.utils.tool.FrameworkTool
import com.fankes.apperrorstracking.utils.tool.ModuleLog
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
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
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File

private enum class ShareActionType { COPY, SHARE, EXPORT }

@Composable
fun DetailScreen(
    initialError: AppErrorsInfoBean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var errorInfo by remember(initialError) { mutableStateOf(initialError) }
    var disableAutoWrap by remember {
        mutableStateOf(ConfigData.isDisableAutoWrapErrorStackTrace)
    }

    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val statusColors = LocalStatusColors.current

    // Share/Export chooser dialog
    var pendingAction by remember { mutableStateOf<ShareActionType?>(null) }
    var shareBrand by remember { mutableStateOf(true) }
    var shareModel by remember { mutableStateOf(true) }
    var shareDisplay by remember { mutableStateOf(true) }
    var sharePackageName by remember { mutableStateOf(true) }
    var pendingExportContent by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null && pendingExportContent.isNotEmpty()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pendingExportContent.toByteArray())
                }
                context.toast(context.getString(R.string.output_stack_success))
            }.onFailure {
                context.toast(context.getString(R.string.output_stack_fail))
            }
        }
    }

    LaunchedEffect(initialError) {
        errorInfo = initialError
        if (initialError.stackTrace.isEmpty()) {
            FrameworkTool.fetchAppErrorInfoData(context, initialError.timestamp) { fullBean ->
                if (!fullBean.isEmpty) {
                    errorInfo = fullBean
                }
            }
        }
    }

    val appIcon = remember(errorInfo.packageName) {
        runCatching {
            context.appIconOf(errorInfo.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    val appName = remember(errorInfo.packageName) {
        context.appNameOf(errorInfo.packageName).ifBlank { errorInfo.packageName }
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, blurActive = blurActive) {
                AdaptiveTopAppBar(
                    title = appName,
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
                        IconButton(onClick = {
                            ModuleLog.error(errorInfo.stackTrace)
                            context.toast(context.getString(R.string.print_to_logcat_success))
                        }) {
                            Icon(
                                imageVector = MiuixIcons.Notes,
                                contentDescription = stringResource(R.string.print_to_logcat),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { pendingAction = ShareActionType.COPY }) {
                            Icon(
                                imageVector = MiuixIcons.Copy,
                                contentDescription = stringResource(R.string.copy_error_stack),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { pendingAction = ShareActionType.SHARE }) {
                            Icon(
                                imageVector = MiuixIcons.Share,
                                contentDescription = stringResource(R.string.share_error_stack),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(onClick = { pendingAction = ShareActionType.EXPORT }) {
                            Icon(
                                imageVector = MiuixIcons.Download,
                                contentDescription = stringResource(R.string.export_to_file),
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
                // 应用信息卡片
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(46.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .squircleBackground(MiuixTheme.colorScheme.surfaceVariant, 12.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = errorInfo.packageName,
                                        fontSize = 13.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }
                                IconButton(onClick = { context.openSelfSetting(errorInfo.packageName) }) {
                                    Icon(
                                        imageVector = MiuixIcons.Info,
                                        contentDescription = stringResource(R.string.open_app_settings),
                                        tint = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            DetailInfoRow("版本品牌", errorInfo.versionBrand.ifBlank { "无" })
                            if (errorInfo.userId > 0) {
                                DetailInfoRow("用户 ID", errorInfo.userId.toString())
                            }
                            DetailInfoRow("CPU 架构", errorInfo.cpuAbi.ifBlank { stringResource(R.string.no_cpu_abi) })
                            DetailInfoRow("Target SDK", errorInfo.targetSdk.toString())
                            DetailInfoRow("Min SDK", errorInfo.minSdk.toString())
                        }
                    }
                }

                // 异常概要卡片
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            DetailInfoRow(
                                label = "异常类型",
                                value = if (errorInfo.isNativeCrash) stringResource(R.string.app_type_native) else stringResource(R.string.app_type_jvm)
                            )
                            DetailInfoRow(
                                label = "发生时间",
                                value = errorInfo.dateTime
                            )
                            DetailInfoRow(
                                label = "异常类名",
                                value = errorInfo.exceptionClassName.ifBlank { "Unknown Exception" },
                                isMonospace = true
                            )
                            if (errorInfo.exceptionMessage.isNotBlank() && errorInfo.exceptionMessage != "unknown") {
                                DetailInfoRow(
                                    label = "异常信息",
                                    value = errorInfo.exceptionMessage
                                )
                            }
                            if (!errorInfo.isNativeCrash && errorInfo.throwClassName.isNotBlank() && errorInfo.throwClassName != "unknown") {
                                DetailInfoRow(label = "源文件", value = errorInfo.throwFileName)
                                DetailInfoRow(label = "抛出类名", value = errorInfo.throwClassName, isMonospace = true)
                                DetailInfoRow(label = "抛出方法", value = errorInfo.throwMethodName)
                                DetailInfoRow(label = "抛出行号", value = errorInfo.throwLineNumber.toString())
                            }
                        }
                    }
                }

                // 堆栈跟踪卡片（参考 ghostlock-app 终端黑底风格）
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(color = Color(0xFF0B1220)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // 顶部终端工具栏
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .squircleBackground(Color(0xFF5FD68A), 4.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "STACKTRACE LOG",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 换行/滚动切换
                                    Box(
                                        modifier = Modifier
                                            .squircleBackground(Color(0xFF1E293B), 6.dp)
                                            .clickable {
                                                val next = !disableAutoWrap
                                                disableAutoWrap = next
                                                ConfigData.isDisableAutoWrapErrorStackTrace = next
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (disableAutoWrap) "单行滚动" else "自动换行",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (disableAutoWrap) Color(0xFFFFC94D) else Color(0xFF60A5FA)
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            context.copyToClipboard(errorInfo.stackTrace)
                                            context.toast(context.getString(R.string.copy_stack_success))
                                        }
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Copy,
                                            contentDescription = stringResource(R.string.copy_error_stack),
                                            tint = Color(0xFFD1D5DB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            val horizontalScrollState = rememberScrollState()
                            SelectionContainer {
                                val stackLines = remember(errorInfo.stackTrace) {
                                    errorInfo.stackTrace.lines()
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (disableAutoWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
                                ) {
                                    stackLines.forEach { line ->
                                        val lineColor = when {
                                            line.contains("Exception") || line.contains("Error") || line.contains("FATAL") || line.contains("SIGSEGV") || line.contains("Abort") -> Color(0xFFFF6B6B)
                                            line.trimStart().startsWith("at ") || line.trimStart().startsWith("at\t") -> Color(0xFF60A5FA)
                                            line.contains("Caused by:") -> Color(0xFFFFC94D)
                                            line.contains("Build fingerprint:") || line.contains("pid:") -> Color(0xFF5FD68A)
                                            else -> Color(0xFFD1D5DB)
                                        }
                                        Text(
                                            text = line,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = lineColor,
                                            lineHeight = 17.sp,
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

        // 分享/导出设置弹窗（置于 Scaffold 内部）
        var lastActiveAction by remember { mutableStateOf(ShareActionType.COPY) }
        if (pendingAction != null) {
            lastActiveAction = pendingAction!!
        }
        val dialogTitle = when (lastActiveAction) {
            ShareActionType.COPY -> stringResource(R.string.copy_error_stack)
            ShareActionType.SHARE -> stringResource(R.string.share_error_stack)
            ShareActionType.EXPORT -> stringResource(R.string.export_to_file)
        }
        OverlayDialog(
            show = pendingAction != null,
            title = dialogTitle,
            insideMargin = DpSize(0.dp, 24.dp),
            onDismissRequest = { pendingAction = null }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.stack_trace_share_top_tip),
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(10.dp))
                CheckboxPreference(
                    title = stringResource(R.string.stack_trace_share_device_brand),
                    checked = shareBrand,
                    checkboxLocation = CheckboxLocation.End,
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    onCheckedChange = { shareBrand = it }
                )
                CheckboxPreference(
                    title = stringResource(R.string.stack_trace_share_device_model),
                    checked = shareModel,
                    checkboxLocation = CheckboxLocation.End,
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    onCheckedChange = { shareModel = it }
                )
                CheckboxPreference(
                    title = stringResource(R.string.stack_trace_share_system_build_id),
                    checked = shareDisplay,
                    checkboxLocation = CheckboxLocation.End,
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    onCheckedChange = { shareDisplay = it }
                )
                CheckboxPreference(
                    title = stringResource(R.string.stack_trace_share_package_name),
                    checked = sharePackageName,
                    checkboxLocation = CheckboxLocation.End,
                    insideMargin = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    onCheckedChange = { sharePackageName = it }
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
                        onClick = { pendingAction = null },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    TextButton(
                        text = stringResource(R.string.confirm),
                        onClick = {
                            val action = pendingAction ?: lastActiveAction
                            val content = errorInfo.stackOutputShareContent(shareBrand, shareModel, shareDisplay, sharePackageName)
                            when (action) {
                                ShareActionType.COPY -> {
                                    context.copyToClipboard(content)
                                    context.toast(context.getString(R.string.copy_stack_success))
                                }
                                ShareActionType.SHARE -> {
                                    performShare(context, errorInfo, content)
                                }
                                ShareActionType.EXPORT -> {
                                    val exportFileContent = errorInfo.stackOutputFileContent(shareBrand, shareModel, shareDisplay, sharePackageName)
                                    pendingExportContent = exportFileContent
                                    val pkg = if (sharePackageName) errorInfo.packageName else "anonymous"
                                    exportLauncher.launch("${pkg}_${errorInfo.utcTime}.log")
                                }
                            }
                            pendingAction = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false,
    valueColor: Color = MiuixTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun performShare(context: Context, bean: AppErrorsInfoBean, content: String) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    if (ConfigData.isShareWithFile) {
        shareIntent.type = "application/octet-stream"
        runCatching {
            val file = File.createTempFile("app_errors_stacktrace_", ".log", context.cacheDir)
            file.deleteOnExit()
            file.writeText(content)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.onFailure {
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, content)
        }
    } else {
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_error_stack)))
}
