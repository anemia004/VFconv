package com.vfconv.app

data class ConvertOptions(
    val codec: String = "libx264",
    val crf: Int = 23,
    val preset: String = "ultrafast",
    val resolution: String? = null,
    val bitrate: String? = null,
    val outputFormat: String = "mp4"
)
