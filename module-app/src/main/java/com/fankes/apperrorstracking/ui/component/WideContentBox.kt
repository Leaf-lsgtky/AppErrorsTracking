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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val WideScreenMinWidth = 600.dp
val MaxContentWidth = 800.dp

@Composable
fun rememberIsWideScreen(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp.dp >= WideScreenMinWidth
}

/**
 * 宽屏内容居中容器，遵循 ui-guidelines.md 规范：
 * LazyColumn 保持全宽（避免两侧出现滚动死区），仅把计算出的 sidePadding 传入 contentPadding，
 * 将内容最大宽度限制为 MaxContentWidth (800dp)。
 */
@Composable
fun WideContentBox(
    modifier: Modifier = Modifier,
    isWide: Boolean = rememberIsWideScreen(),
    content: @Composable (sidePadding: Dp) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val sidePadding = if (isWide && maxWidth > MaxContentWidth) {
            (maxWidth - MaxContentWidth) / 2
        } else {
            0.dp
        }
        content(sidePadding)
    }
}

/**
 * 屏幕缺口适配：仅补水平 displayCutout ∪ navigationBars，竖屏为 0。
 * 二级页面根 LazyColumn 在 fillMaxSize() 后调用。
 */
@Composable
fun Modifier.horizontalCutoutPadding(): Modifier =
    this.windowInsetsPadding(
        WindowInsets.displayCutout.union(WindowInsets.navigationBars)
            .only(WindowInsetsSides.Horizontal)
    )

