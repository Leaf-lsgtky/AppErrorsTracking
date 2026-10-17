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
package com.fankes.apperrorstracking.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fankes.apperrorstracking.ui.component.AppBottomBar
import com.fankes.apperrorstracking.ui.component.AppNavigationRail
import com.fankes.apperrorstracking.ui.component.MainTab
import com.fankes.apperrorstracking.ui.component.rememberBlurBackdrop
import com.fankes.apperrorstracking.ui.component.rememberIsWideScreen
import com.fankes.apperrorstracking.ui.screen.home.HomeScreen
import com.fankes.apperrorstracking.ui.screen.records.RecordsScreen
import com.fankes.apperrorstracking.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold

@Composable
fun MainScreen() {
    val isWideScreen = rememberIsWideScreen()
    val tabs = MainTab.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val backdrop = rememberBlurBackdrop()

    val currentTab = tabs[pagerState.currentPage]

    fun selectTab(tab: MainTab) {
        val index = tabs.indexOf(tab)
        if (index >= 0 && index != pagerState.currentPage) {
            coroutineScope.launch {
                pagerState.scrollToPage(index)
            }
        }
    }

    if (isWideScreen) {
        // 宽屏模式：使用可折叠侧边 NavigationRail，底栏隐藏
        Row(modifier = Modifier.fillMaxSize()) {
            AppNavigationRail(
                selectedTab = currentTab,
                onTabSelected = { selectTab(it) }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                    .windowInsetsPadding(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.End)
                    )
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false
                ) { page ->
                    when (tabs[page]) {
                        MainTab.Home -> HomeScreen(
                            bottomPadding = 0.dp,
                            onNavigateToTab = { selectTab(it) }
                        )
                        MainTab.Records -> RecordsScreen(bottomPadding = 0.dp)
                        MainTab.Settings -> SettingsScreen(bottomPadding = 0.dp)
                    }
                }
            }
        }
    } else {
        // 竖屏/手机模式：使用 Scaffold 包裹并由 BlurredBar 承载底部 NavigationBar
        Scaffold(
            bottomBar = {
                AppBottomBar(
                    selectedTab = currentTab,
                    onTabSelected = { selectTab(it) },
                    backdrop = backdrop
                )
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                when (tabs[page]) {
                    MainTab.Home -> HomeScreen(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        bottomBackdrop = backdrop,
                        onNavigateToTab = { selectTab(it) }
                    )
                    MainTab.Records -> RecordsScreen(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        bottomBackdrop = backdrop
                    )
                    MainTab.Settings -> SettingsScreen(
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        bottomBackdrop = backdrop
                    )
                }
            }
        }
    }
}
