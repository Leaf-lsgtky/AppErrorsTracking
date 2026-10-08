/*
 * AppErrorsTracking - Added more features to app's crash dialog, fixed custom rom deleted dialog, the best experience to Android developer.
 * Copyright (C) 2017 Fankes Studio(qzmmcn@163.com)
 * https://github.com/KitsunePie/AppErrorsTracking
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2022/5/7.
 */
package com.fankes.apperrorstracking.hook

import com.fankes.apperrorstracking.data.ConfigData
import com.fankes.apperrorstracking.hook.entity.FrameworkHooker
import com.fankes.apperrorstracking.utils.tool.ModuleLog
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

class HookEntry : XposedModule() {

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        super.onModuleLoaded(param)
        ModuleLog.xposedInterface = this
        ModuleLog.info("AppErrorsTracking loaded in process: ${param.processName}, isSystemServer: ${param.isSystemServer}")
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        super.onSystemServerStarting(param)
        ModuleLog.info("onSystemServerStarting: initializing AppErrorsTracking")
        runCatching {
            val remotePrefs = getRemotePreferences("default")
            ConfigData.init(remotePrefs)
        }.onFailure {
            ModuleLog.warn("Failed to initialize remote preferences: ${it.message}")
        }
        runCatching {
            FrameworkHooker.init(this, param.classLoader)
        }.onFailure {
            ModuleLog.error("Failed to initialize FrameworkHooker", it)
        }
    }
}