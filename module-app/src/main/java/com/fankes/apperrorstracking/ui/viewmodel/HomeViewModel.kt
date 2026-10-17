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
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.fankes.apperrorstracking.application.AppErrorsApplication
import com.fankes.apperrorstracking.const.ModuleVersion
import com.fankes.apperrorstracking.data.ConfigData
import com.fankes.apperrorstracking.ui.activity.main.MainActivity
import com.fankes.apperrorstracking.utils.factory.openBrowser
import com.fankes.apperrorstracking.utils.tool.FrameworkTool
import com.fankes.apperrorstracking.utils.tool.GithubReleaseTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class HomeUiState(
    val isModuleActive: Boolean = false,
    val isModuleValid: Boolean = false,
    val frameworkApiInfo: String = "",
    val systemVersion: String = "",
    val moduleVersion: String = "",
    val isCiMode: Boolean = false,
    val ciCommitId: String = "",
    val updateVersion: String? = null,
    val updateAction: (() -> Unit)? = null,
    val showDeveloperNotice: Boolean = false,
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            systemVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) ${Build.DISPLAY}",
            moduleVersion = ModuleVersion.NAME,
            isCiMode = ModuleVersion.isCiMode,
            ciCommitId = ModuleVersion.GITHUB_COMMIT_ID,
            showDeveloperNotice = ConfigData.isShowDeveloperNotice,
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun init(context: Context) {
        refreshModuleStatus(context)
        GithubReleaseTool.checkingForUpdate(context, ModuleVersion.NAME) { version, action ->
            _uiState.update { it.copy(updateVersion = version, updateAction = action) }
        }
    }

    fun refreshModuleStatus(context: Context) {
        val service = AppErrorsApplication.xposedService
        val isActive = service != null || MainActivity.isModuleValied
        val apiInfo = if (service != null) {
            "Activated by ${service.frameworkName} API ${service.apiVersion}"
        } else if (isActive) {
            "Activated by libxposed (System Framework)"
        } else {
            ""
        }

        _uiState.update {
            it.copy(
                isModuleActive = isActive,
                isModuleValid = MainActivity.isModuleValied,
                frameworkApiInfo = apiInfo,
            )
        }

        FrameworkTool.checkingActivated(context) { isValid ->
            MainActivity.isModuleValied = isValid
            val updatedActive = AppErrorsApplication.xposedService != null || isValid
            val updatedApiInfo = if (service != null) {
                "Activated by ${service.frameworkName} API ${service.apiVersion}"
            } else if (updatedActive) {
                "Activated by libxposed (System Framework)"
            } else {
                ""
            }
            _uiState.update {
                it.copy(
                    isModuleActive = updatedActive,
                    isModuleValid = isValid,
                    frameworkApiInfo = updatedApiInfo,
                )
            }
        }
    }

    fun dismissDeveloperNotice() {
        ConfigData.isShowDeveloperNotice = false
        _uiState.update { it.copy(showDeveloperNotice = false) }
    }

    fun restartSystem(context: Context) {
        FrameworkTool.restartSystem(context)
    }

    fun openGithub(context: Context) {
        context.openBrowser("https://github.com/KitsunePie/AppErrorsTracking")
    }

    fun openCoolapk(context: Context) {
        context.openBrowser(url = "https://www.coolapk.com/u/876977", packageName = "com.coolapk.market")
    }
}
