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
package com.fankes.apperrorstracking.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 语义化状态颜色定义，遵循 ui-guidelines.md 规范：
 * 禁止在屏幕中散落原始 Color(0xFF...)，所有状态、日志级别及重要语义颜色均由 StatusColors 统一提供。
 */
@Immutable
data class StatusColors(
    val activeContainer: Color,
    val onActiveContainer: Color,
    val activeIcon: Color,
    val notFullyActiveContainer: Color,
    val onNotFullyActiveContainer: Color,
    val notFullyActiveIcon: Color,
    val inactiveContainer: Color,
    val onInactiveContainer: Color,
    val inactiveIcon: Color,
    val logDebug: Color,
    val logInfo: Color,
    val logWarn: Color,
    val logError: Color,
    val onLogTag: Color,
)

val LightStatusColors = StatusColors(
    activeContainer = Color(0xFFE8F5E9),
    onActiveContainer = Color(0xFF1B5E20),
    activeIcon = Color(0xFF2E7D32),
    notFullyActiveContainer = Color(0xFFFFF8E1),
    onNotFullyActiveContainer = Color(0xFFE65100),
    notFullyActiveIcon = Color(0xFFF57C00),
    inactiveContainer = Color(0xFFF5F5F5),
    onInactiveContainer = Color(0xFF424242),
    inactiveIcon = Color(0xFF757575),
    logDebug = Color(0xFF1976D2),
    logInfo = Color(0xFF388E3C),
    logWarn = Color(0xFFF57C00),
    logError = Color(0xFFD32F2F),
    onLogTag = Color(0xFFFFFFFF),
)

val DarkStatusColors = StatusColors(
    activeContainer = Color(0xFF1A3825),
    onActiveContainer = Color(0xFFA5D6A7),
    activeIcon = Color(0xFF4CAF50),
    notFullyActiveContainer = Color(0xFF3E2D12),
    onNotFullyActiveContainer = Color(0xFFFFCC80),
    notFullyActiveIcon = Color(0xFFFFB74D),
    inactiveContainer = Color(0xFF2B2B2B),
    onInactiveContainer = Color(0xFFB0B0B0),
    inactiveIcon = Color(0xFF888888),
    logDebug = Color(0xFF64B5F6),
    logInfo = Color(0xFF81C784),
    logWarn = Color(0xFFFFB74D),
    logError = Color(0xFFE57373),
    onLogTag = Color(0xFF121212),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
