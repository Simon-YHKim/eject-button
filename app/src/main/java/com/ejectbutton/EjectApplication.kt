package com.ejectbutton

import android.app.Application
import com.ejectbutton.crash.CrashReportManager

class EjectApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 크래시 리포팅 초기화 (UncaughtExceptionHandler 등록)
        CrashReportManager.initialize(this)
    }
}
