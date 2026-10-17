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

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.fankes.apperrorstracking.R
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import androidx.compose.material.icons.rounded.History
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class MainTab(
    @get:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    Home(R.string.tab_home, MiuixIcons.Home),
    Records(R.string.tab_records, Icons.Rounded.History),
    Settings(R.string.tab_settings, MiuixIcons.Settings),
}

@Composable
fun AppBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
) {
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    BlurredBar(backdrop = backdrop, blurActive = blurActive) {
        NavigationBar(
            modifier = modifier,
            color = barColor,
        ) {
            MainTab.entries.forEach { tab ->
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    icon = tab.icon,
                    label = stringResource(tab.titleRes),
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
fun AppNavigationRail(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier,
        state = rememberNavigationRailState(),
        color = MiuixTheme.colorScheme.surface,
        expandContentDescription = stringResource(R.string.nav_rail_expand),
        collapseContentDescription = stringResource(R.string.nav_rail_collapse),
    ) {
        MainTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = tab.icon,
                label = stringResource(tab.titleRes),
            )
        }
    }
}
