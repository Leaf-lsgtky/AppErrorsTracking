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
import androidx.lifecycle.viewModelScope
import com.fankes.apperrorstracking.bean.AppErrorsInfoBean
import com.fankes.apperrorstracking.bean.AppFiltersBean
import com.fankes.apperrorstracking.bean.enum.AppFiltersType
import com.fankes.apperrorstracking.utils.factory.decimal
import com.fankes.apperrorstracking.utils.tool.FrameworkTool
import com.fankes.apperrorstracking.utils.tool.ZipFileTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

@Immutable
data class RecordStatistics(
    val totalErrors: Int = 0,
    val totalApps: Int = 0,
    val mostErrorsPackageName: String = "",
    val mostErrorsType: String = "",
    val errorsPpt: String = "0%",
)

@Immutable
data class RecordsUiState(
    val records: List<AppErrorsInfoBean> = emptyList(),
    val isLoading: Boolean = false,
    val statistics: RecordStatistics? = null,
    val isCalculatingStatistics: Boolean = false,
)

class RecordsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    fun loadRecords(context: Context) {
        _uiState.update { it.copy(isLoading = true) }
        FrameworkTool.fetchAppErrorsInfoData(context) { list ->
            _uiState.update {
                it.copy(
                    records = list.toList(),
                    isLoading = false,
                )
            }
        }
    }

    fun removeRecord(context: Context, bean: AppErrorsInfoBean, onComplete: () -> Unit) {
        FrameworkTool.removeAppErrorsInfoData(context, bean) {
            loadRecords(context)
            onComplete()
        }
    }

    fun clearAllRecords(context: Context, onComplete: () -> Unit) {
        FrameworkTool.clearAppErrorsInfoData(context) {
            loadRecords(context)
            onComplete()
        }
    }

    fun calculateStatistics(context: Context) {
        _uiState.update { it.copy(isCalculatingStatistics = true) }
        val currentRecords = _uiState.value.records
        FrameworkTool.fetchAppListData(context, AppFiltersBean(type = AppFiltersType.ALL)) { allBeans ->
            viewModelScope.launch(Dispatchers.Default) {
                val errorsApps = currentRecords.groupBy { it.packageName }
                    .map { it.key to it.value.size }
                    .sortedByDescending { it.second }
                val mostAppPackageName = errorsApps.firstOrNull()?.first ?: ""
                val mostErrorsType = currentRecords.groupBy { it.exceptionClassName }
                    .map { it.key to it.value.size }
                    .sortedByDescending { it.second }
                    .firstOrNull()?.first?.let { cls ->
                        if (cls.contains(".")) cls.substringAfterLast(".") else cls
                    } ?: ""
                val totalAppsCount = allBeans.size.coerceAtLeast(1)
                val pptCount = (((errorsApps.size.toFloat()) * 100f) / totalAppsCount.toFloat()).decimal()

                val stats = RecordStatistics(
                    totalErrors = currentRecords.size,
                    totalApps = allBeans.size,
                    mostErrorsPackageName = mostAppPackageName,
                    mostErrorsType = mostErrorsType,
                    errorsPpt = "$pptCount%",
                )
                _uiState.update {
                    it.copy(
                        statistics = stats,
                        isCalculatingStatistics = false,
                    )
                }
            }
        }
    }

    fun clearStatistics() {
        _uiState.update { it.copy(statistics = null) }
    }

    suspend fun prepareExportZip(
        context: Context,
        sDeviceBrand: Boolean,
        sDeviceModel: Boolean,
        sDisplay: Boolean,
        sPackageName: Boolean,
    ): File? = withContext(Dispatchers.IO) {
        val fullRecords: List<AppErrorsInfoBean> = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            FrameworkTool.fetchAppErrorsInfoDataFull(context) { list ->
                if (cont.isActive) {
                    cont.resume(list)
                }
            }
        }

        val tempDir = File(context.cacheDir, "export_temp").apply {
            deleteRecursively()
            mkdirs()
        }

        fullRecords.forEachIndexed { index, bean ->
            val pkg = if (sPackageName) bean.packageName else "anonymous_$index"
            val file = File(tempDir, "${pkg}_${bean.utcTime}.log")
            file.writeText(bean.stackOutputFileContent(sDeviceBrand, sDeviceModel, sDisplay, sPackageName))
        }

        val zipFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.zip")
        ZipFileTool.zipMultiFile(tempDir.absolutePath, zipFile.absolutePath)
        tempDir.deleteRecursively()
        if (zipFile.exists()) zipFile else null
    }
}
