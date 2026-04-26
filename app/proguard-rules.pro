# Eject Button ProGuard 규칙

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }

# 앱 데이터 클래스 유지 (난독화 방지)
-keep class com.ejectbutton.data.** { *; }

# Service 유지
-keep class com.ejectbutton.service.** { *; }

# WindowManager overlay가 리플렉션으로 접근하는 클래스 보호
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}

# MS Clarity SDK
-keep class com.microsoft.clarity.** { *; }
-dontwarn com.microsoft.clarity.**

# Crash reporter
-keep class com.ejectbutton.crash.** { *; }

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Google AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Billing Manager
-keep class com.ejectbutton.billing.** { *; }

# Firebase Analytics — measurement-api 가 리플렉션으로 접근하는 클래스 보호.
# v1.0.9 — 이전에 keep 누락으로 release 빌드에서 silent failure 위험 있던 것 명시 추가.
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.firebase.analytics.**
-dontwarn com.google.android.gms.measurement.**

# v1.0.10 — Firebase Crashlytics: 스택 trace deobfuscation 위해 SourceFile + LineNumber 보존.
# Crashlytics gradle plugin 이 mapping.txt 를 자동 업로드해 콘솔에서 deobfuscate.
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**
