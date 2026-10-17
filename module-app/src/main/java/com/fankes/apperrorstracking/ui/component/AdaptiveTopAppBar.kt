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
package com.fankes.apperrorstracking.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 响应式顶栏组件，遵循 ui-guidelines.md 规范：
 * 在宽屏（横屏/平板，窗口宽 >= 600dp）下固定不折叠，避免纵向空间紧张。
 */
@Composable
fun AdaptiveTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior? = null,
    color: Color = MiuixTheme.colorScheme.surface,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    isWideScreen: Boolean = rememberIsWideScreen(),
) {
    val effectiveScrollBehavior = if (isWideScreen) null else scrollBehavior
    TopAppBar(
        modifier = modifier,
        title = title,
        color = color,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = effectiveScrollBehavior,
    )
}
