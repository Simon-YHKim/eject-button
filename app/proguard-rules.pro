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
