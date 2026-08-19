package com.vfconv.app

import android.app.Application
import com.arthenica.ffmpegkit.FFmpegKit

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FFmpegKit.initialize(this)
    }
}
