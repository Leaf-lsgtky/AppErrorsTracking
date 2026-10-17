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
package com.fankes.apperrorstracking.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.fankes.apperrorstracking.data.ConfigData
import com.fankes.apperrorstracking.utils.factory.hideOrShowLauncherIcon
import com.fankes.apperrorstracking.utils.factory.isLauncherIconShowing
import com.fankes.apperrorstracking.utils.tool.AppAnalyticsTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class SettingsUiState(
    val onlyShowErrorsInFront: Boolean = false,
    val onlyShowErrorsInMain: Boolean = false,
    val alwaysShowsReopenAppOptions: Boolean = false,
    val preventMisoperation: Boolean = false,
    val shareWithFile: Boolean = false,
    val material3StyleDialog: Boolean = false,
    val hideLauncherIcon: Boolean = false,
    val enableAppConfigTemplate: Boolean = false,
    val isAnalyticsAvailable: Boolean = false,
    val enableAnonymousStatistics: Boolean = false,
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun refreshSettings(context: Context) {
        _uiState.update {
            SettingsUiState(
                onlyShowErrorsInFront = ConfigData.isEnableOnlyShowErrorsInFront,
                onlyShowErrorsInMain = ConfigData.isEnableOnlyShowErrorsInMain,
                alwaysShowsReopenAppOptions = ConfigData.isEnableAlwaysShowsReopenAppOptions,
                preventMisoperation = ConfigData.isEnablePreventMisoperation,
                shareWithFile = ConfigData.isShareWithFile,
                material3StyleDialog = ConfigData.isEnableMaterial3StyleAppErrorsDialog,
                hideLauncherIcon = !context.isLauncherIconShowing,
                enableAppConfigTemplate = ConfigData.isEnableAppConfigTemplate,
                isAnalyticsAvailable = AppAnalyticsTool.isAvailable,
                enableAnonymousStatistics = AppAnalyticsTool.isEnabled,
            )
        }
    }

    fun setOnlyShowErrorsInFront(enabled: Boolean) {
        ConfigData.isEnableOnlyShowErrorsInFront = enabled
        _uiState.update { it.copy(onlyShowErrorsInFront = enabled) }
    }

    fun setOnlyShowErrorsInMain(enabled: Boolean) {
        ConfigData.isEnableOnlyShowErrorsInMain = enabled
        _uiState.update { it.copy(onlyShowErrorsInMain = enabled) }
    }

    fun setAlwaysShowsReopenAppOptions(enabled: Boolean) {
        ConfigData.isEnableAlwaysShowsReopenAppOptions = enabled
        _uiState.update { it.copy(alwaysShowsReopenAppOptions = enabled) }
    }

    fun setPreventMisoperation(enabled: Boolean) {
        ConfigData.isEnablePreventMisoperation = enabled
        _uiState.update { it.copy(preventMisoperation = enabled) }
    }

    fun setShareWithFile(enabled: Boolean) {
        ConfigData.isShareWithFile = enabled
        _uiState.update { it.copy(shareWithFile = enabled) }
    }

    fun setMaterial3StyleDialog(enabled: Boolean) {
        ConfigData.isEnableMaterial3StyleAppErrorsDialog = enabled
        _uiState.update { it.copy(material3StyleDialog = enabled) }
    }

    fun setHideLauncherIcon(context: Context, hide: Boolean) {
        context.hideOrShowLauncherIcon(hide)
        _uiState.update { it.copy(hideLauncherIcon = hide) }
    }

    fun setEnableAppConfigTemplate(enabled: Boolean) {
        ConfigData.isEnableAppConfigTemplate = enabled
        _uiState.update { it.copy(enableAppConfigTemplate = enabled) }
    }

    fun setEnableAnonymousStatistics(enabled: Boolean) {
        AppAnalyticsTool.isEnabled = enabled
        _uiState.update { it.copy(enableAnonymousStatistics = enabled) }
    }
}
