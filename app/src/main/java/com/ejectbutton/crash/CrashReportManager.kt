package com.ejectbutton.crash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 앱 크래시 발생 시 로그를 저장하고, 다음 실행 시 개발자에게 이메일로 전송.
 *
 * Flow:
 * 1. Application.onCreate()에서 initialize() → UncaughtExceptionHandler 등록
 * 2. 크래시 발생 → 스택 트레이스 + 디바이스 정보를 파일에 저장
 * 3. 다음 앱 실행 시 hasPendingReport() 확인
 * 4. sendPendingReport()로 이메일 전송 (Intent.ACTION_SEND)
 */
object CrashReportManager {

    private const val CRASH_DIR = "crash_reports"
    // v1.0.9 — 모든 사용자 향 이메일을 simonkim250405@gmail.com 으로 통일
    // (피드백 페이지 / fastlane 풀 디스크립션 / Play Console 연락처와 동일).
    private const val DEV_EMAIL = "simonkim250405@gmail.com"

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun initialize(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrashLog(context, thread, throwable)
            // 기본 핸들러 호출 (시스템 크래시 다이얼로그)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun hasPendingReport(context: Context): Boolean {
        val dir = File(context.filesDir, CRASH_DIR)
        return dir.exists() && dir.listFiles()?.isNotEmpty() == true
    }

    /**
     * 저장된 크래시 로그를 이메일로 전송.
     * 이메일 앱이 열리면서 발송 내용이 자동으로 채워짐.
     */
    fun sendPendingReport(activity: Activity) {
        val dir = File(activity.filesDir, CRASH_DIR)
        if (!dir.exists()) return

        val files = dir.listFiles() ?: return
        if (files.isEmpty()) return

        val body = buildString {
            files.sortedByDescending { it.lastModified() }.take(5).forEach { f ->
                appendLine(f.readText())
                appendLine()
                appendLine("─".repeat(40))
                appendLine()
            }
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "[Eject Button] Crash Report — ${
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    }"
                )
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(Intent.createChooser(intent, "Send crash report"))
        } catch (_: Exception) {
            // 이메일 앱이 없는 경우 무시
        }

        // 전송 시도 후 로그 삭제
        files.forEach { it.delete() }
    }

    /** 크래시 로그 모두 삭제 (사용자가 "보내지 않기" 선택 시) */
    fun clearPendingReports(context: Context) {
        val dir = File(context.filesDir, CRASH_DIR)
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun saveCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val dir = File(context.filesDir, CRASH_DIR)
            dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "crash_$timestamp.txt")

            val stackTrace = StringWriter().also { sw ->
                throwable.printStackTrace(PrintWriter(sw))
            }.toString()

            val report = buildString {
                appendLine("═══ EJECT BUTTON CRASH REPORT ═══")
                appendLine()
                appendLine("Time     : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())}")
                appendLine("Device   : ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android  : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Thread   : ${thread.name}")
                appendLine()
                appendLine("═══ EXCEPTION ═══")
                appendLine(throwable.javaClass.name + ": " + throwable.message)
                appendLine()
                appendLine("═══ STACK TRACE ═══")
                appendLine(stackTrace)
                appendLine()
                // 원인 체인
                var cause = throwable.cause
                var depth = 0
                while (cause != null && depth < 5) {
                    appendLine("═══ CAUSED BY (${depth + 1}) ═══")
                    val causeTrace = StringWriter().also { sw ->
                        cause!!.printStackTrace(PrintWriter(sw))
                    }.toString()
                    appendLine(causeTrace)
                    cause = cause.cause
                    depth++
                }
            }

            file.writeText(report)
        } catch (_: Exception) {
            // 로그 저장 실패 시 무시 — 크래시 핸들러 내에서 추가 예외 방지
        }
    }
}
