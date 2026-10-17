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
import com.fankes.apperrorstracking.utils.tool.FrameworkTool
import com.fankes.apperrorstracking.utils.tool.ModuleLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class LogsUiState(
    val isLoading: Boolean = false,
    val allLogs: List<ModuleLog.LogItem> = emptyList(),
    val filteredLogs: List<ModuleLog.LogItem> = emptyList(),
    val filters: Set<String> = setOf("D", "I", "W", "E"),
    val isFilterDialogOpen: Boolean = false,
)

class LogsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    fun loadLogs(context: Context) {
        _uiState.update { it.copy(isLoading = true) }
        FrameworkTool.fetchLoggerData(context) { logData ->
            val reversed = logData.reversed()
            _uiState.update { state ->
                val filtered = reversed.filter { state.filters.contains(it.priority) }
                state.copy(
                    isLoading = false,
                    allLogs = reversed,
                    filteredLogs = filtered,
                )
            }
        }
    }

    fun setFilters(newFilters: Set<String>) {
        _uiState.update { state ->
            val filtered = state.allLogs.filter { newFilters.contains(it.priority) }
            state.copy(
                filters = newFilters,
                filteredLogs = filtered,
            )
        }
    }

    fun setFilterDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isFilterDialogOpen = open) }
    }

    fun getExportContent(): String {
        return ModuleLog.contents(_uiState.value.filteredLogs)
    }
}
