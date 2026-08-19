package com.vfconv.app

data class ConvertOptions(
    val codec: String,
    val preset: String = "medium",
    val crf: Int = 23,
    val resolution: String?,
    val bitrate: String?,
    val outputFormat: String
)
