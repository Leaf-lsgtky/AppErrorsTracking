package com.fankes.apperrorstracking.data

import java.io.Serializable

/**
 * 键值数据存储模板
 */
data class PrefsData<T>(
    val key: String,
    val value: T
) : Serializable
