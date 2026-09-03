# Eject Button ProGuard 규칙

# Compose
# v1.7.6 — blanket keep 제거. `-keep class androidx.compose.** { *; }` 가 전체 클래스
#   28,807개 중 18,308개(63.6%)를 난독화에서 빼서 Play "앱 최적화" 난독화 비율이
#   23%(기준 25%) 로 떨어져 있었다(1091/1.7.4 카드, 해결 기한 2027-02).
#   Compose 가 리플렉션으로 접근하는 것은 ui AAR 이 동봉한 consumer 규칙
#   (proguard.txt) 3건뿐이고 AGP 가 이를 자동 적용하므로 blanket keep 은 중복이었다:
#     - ViewLayerContainer.dispatchGetDisplayList()
#     - AndroidComposeView.findViewByAccessibilityIdTraversal(int)
#     - * extends ModifierNodeElement (keep,allowshrinking)
-dontwarn androidx.compose.**

# Kotlin
# v1.7.6 — blanket keep 제거. kotlin-stdlib 은 consumer 규칙을 싣지 않는다(필요가 없어서).
#   coroutines 는 META-INF/com.android.tools/proguard/coroutines.pro 를 자체 동봉해
#   AGP 가 자동 적용한다. 둘 다 명시 keep 이 불필요.

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

# v1.6.11 — Google Play Core (In-App Update Flexible flow).
# AAR consumer rules 가 자동 적용되지만, v1.0.9 의 Firebase Analytics 사례
# ("이전에 keep 누락으로 release 빌드에서 silent failure 위험 있던 것 명시 추가")
# 와 동일한 defensive 패턴. R8 full-mode 또는 미래 AGP 업그레이드 시
# InstallStateUpdatedListener SAM 이 reflection-invoked 라 strip 되면 listener
# silent-fail → 다운로드 완료 알림이 영원히 안 뜸.
-keep class com.google.android.play.core.** { *; }
-keep interface com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**
