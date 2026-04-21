# Play Console — Policy Declarations (Eject Button)

> Play Console 심사 시 "App content" 섹션에서 민감 권한·기능마다
> 서면 정당화 (justification) 를 요구한다. 아래 문구는 각 항목 폼에
> 그대로 붙여넣을 수 있도록 정리한 초안이다.
>
> 앱 ID: `com.simonykim.ejectbutton`
> 앱 이름: **Eject Button — Escape Call**
> Core Purpose: 사용자가 불편한 사회적 상황 (회식·미팅·데이트 등) 에서
>   자연스럽게 빠져나올 수 있도록 **가짜 수신 전화** 를 즉시 트리거.
>   조작은 ① 타이머 ② 흔들기 (가속도 센서) ③ 볼륨 버튼 3가지.

---

## 1. SYSTEM_ALERT_WINDOW (Display over other apps)

**카테고리**: Sensitive Permission — manifest 선언만으로도 리뷰 플래그.

**Justification (EN)**
> This permission is the core mechanism of the app. When the user triggers
> the eject action (timer, shake, or volume button), the app displays a
> full-screen simulated incoming-call UI on top of whichever app or launcher
> is currently in focus. Without SYSTEM_ALERT_WINDOW the simulated call
> cannot appear while the user is in another app or on the lock screen,
> defeating the app's entire purpose. The overlay is only shown while the
> user-initiated eject event is active and is dismissed as soon as the user
> taps "End call". No content is drawn over other apps at any other time.

**Justification (KO)**
> 본 앱의 핵심 동작은 사용자가 타이머 / 흔들기 / 볼륨 버튼으로 가짜 수신
> 전화 트리거를 실행했을 때, 현재 사용 중인 앱이나 런처 위에 전체 화면
> 수신 UI 를 즉시 띄우는 것입니다. SYSTEM_ALERT_WINDOW 권한이 없으면 다른
> 앱 사용 중이거나 잠금 화면에서는 가짜 전화 화면을 띄울 수 없어 앱의
> 목적이 성립하지 않습니다. 오버레이는 사용자가 직접 이벤트를 발동한
> 동안에만 표시되며, "종료" 버튼을 누르면 즉시 해제됩니다. 그 외 시점에
> 타 앱 위에 콘텐츠를 그리는 일은 없습니다.

**제출 증빙 영상**: 권장 20–40초 스크린 레코딩 — 앱 실행 → 설정 (예: "10초 후 울리기") → 홈 버튼 → 다른 앱 사용 → 10초 뒤 수신 화면 overlay → 종료.

---

## 2. FOREGROUND_SERVICE_SPECIAL_USE

**카테고리**: Restricted permission — Android 14+ (API 34+) 부터
`<property>` 선언과 함께 Play 심사 전용 justification 필수.

**매니페스트 현황** (`app/src/main/AndroidManifest.xml`):
```xml
<service
    android:name=".service.FakeCallOverlayService"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        ... />
</service>
```

**Justification (EN)**
> Our foreground service runs continuously while the user has armed the
> "shake to eject" or "volume-button to eject" modes so that accelerometer
> and key events can be captured reliably even when the screen is off or
> another app is in focus. No other foregroundServiceType accurately
> describes this use case: it is not media playback, location, camera,
> microphone, data sync, phone call, remote messaging, health, or
> connected device. It is user-initiated sensor monitoring for the sole
> purpose of triggering the simulated call on demand. The service stops
> itself when the user disarms the mode or the simulated call ends.

**Justification (KO)**
> 본 서비스는 사용자가 "흔들기로 탈출" 또는 "볼륨 버튼으로 탈출" 모드를
> 활성화한 동안에만 실행되며, 화면이 꺼져 있거나 다른 앱이 포커스인
> 상태에서도 가속도 센서·키 이벤트를 안정적으로 감지해 즉시 가짜 수신
> 전화를 띄우기 위한 목적입니다. 이 용도는 mediaPlayback / location /
> camera / microphone / dataSync / phoneCall / remoteMessaging / health /
> connectedDevice 등 기존 type 어느 것과도 부합하지 않아 specialUse 를
> 사용합니다. 사용자가 해당 모드를 끄거나 가짜 전화가 종료되면 서비스도
> 자동 중지됩니다.

---

## 3. READ_PHONE_STATE

**카테고리**: Core Functionality declaration.

**Justification**
> Used only to detect whether a real incoming call is in progress so that
> the app can suppress its simulated call overlay and avoid interfering
> with genuine phone activity. Call log content, caller identity, and
> subscriber info are never read. No phone-state data leaves the device.

---

## 4. READ_CONTACTS

**카테고리**: Permissions Declaration (Prominent Disclosure).

**Justification**
> Used only when the user explicitly chooses a contact to impersonate as
> the simulated incoming caller (e.g. "Boss", "Mom"). The selection is
> stored locally on device in SharedPreferences. Contact data is not
> uploaded, synced, or shared with any third party, including the app
> developer. Users can skip the contact picker and use a default caller
> name instead.

---

## 5. REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

**카테고리**: High-impact permission — Play automatically flags apps
requesting this at runtime. Declaration required.

**Justification**
> Requested only after the user enables "shake to eject" or
> "volume-button to eject" modes that rely on the foreground sensor
> service. Android's Doze and App Standby features aggressively suspend
> foreground services, which in our case would silently break the user's
> intended safety trigger. The request is opt-in: users can decline and
> the app still works in timer mode. Justification aligns with the
> approved use-case "**Continuous background service for user-visible
> functionality that cannot be performed in the background**".

---

## 6. POST_NOTIFICATIONS (Android 13+)

**카테고리**: Standard runtime permission.

**Usage**
- Foreground service notification (required by Android 14+).
- Optional: low-priority "call is armed — shake ready" indicator.

No promotional push notifications are sent.

---

## 7. CAMERA + Camera Flash

**카테고리**: Sensitive permission — **주의**: 카메라 권한 선언은 Play 리뷰에서 "사진/동영상 촬영" 으로 해석될 수 있음.

**Justification**
> The camera permission is requested **exclusively to toggle the camera
> flash LED** as an optional visual ring indicator when the simulated
> incoming call arrives. The app does not open the camera preview,
> capture photos or video, or access any camera imagery. If the future
> CameraX API allows flashlight-only access without CAMERA permission,
> we will migrate. The feature is user-opt-in and can be disabled in
> Settings → "Ring flash".

> ⚠️ **대안 검토 필요**: Android 12+ 부터 `CameraManager.setTorchMode()`
> 는 CAMERA 권한 없이도 Flashlight 만 토글 가능 (특정 OEM 제외).
> 가능하면 권한 자체를 제거하는 쪽이 심사 리스크가 낮다.

---

## 8. Advertising ID (AdMob 동반)

Play Console → App Content → **Advertising ID** 섹션에서 반드시 **"Yes"**.

**Declared purposes** (체크):
- [x] Advertising or marketing
- [x] Analytics

Firebase Analytics + AdMob 둘 다 Advertising ID 를 사용한다. 체크 안 하면 AAB 업로드가 거부된다.

---

## 9. Accessibility Service (해당 없음)

본 앱은 AccessibilityService 를 사용하지 않는다. 만약 향후 "앱 위 표시"
권한 자동 설정 UX 를 위해 추가할 계획이라면 별도 서면 심사 (평균 4주) 가
필요하니 미리 공지.

---

## 10. Subscription / Billing

**Play Billing Library 7.0.0** 사용 중. Play Console → Monetization →
In-app products / Subscriptions 에서 아래 항목을 등록해야 한다:

- 월간 구독 SKU (이름·설명·가격 — `PLAY_STORE_ASO.md` 참조)
- 무료 체험 기간 (해당 시)
- 구독 약관·해지 정책 링크 (privacy-policy 페이지 내 섹션으로 포함 권장)

---

## 11. Target Audience & Content (필수 작성)

- **Target age**: 13+ (구독·광고 포함되므로 Children 카테고리 피함)
- **Appeals to children**: No
- **Ads**: Yes (AdMob)
- **In-app purchases**: Yes (Subscription, ≤ ~₩5,900/월)
- **Restricted content**: None
- **Content rating**: IARC 설문 — Social Features 제외, 사용자 커뮤니케이션 없음

---

## 12. Data Safety — 별도 문서

`docs/play-data-safety.md` 참조.

---

## 제출 전 체크

- [ ] targetSdk 35 (2026 년 Play 요구사항 충족)
- [ ] versionCode CI auto-bump 동작 확인
- [ ] Privacy Policy URL 접근 가능 여부 (Cloudflare Pages 404 체크)
- [ ] 릴리즈 AAB 빌드 성공 + GitHub Release artifacts 확인
- [ ] SHA-1 / SHA-256 upload key 지문을 Firebase 콘솔 + AdMob 에 등록
- [ ] AdMob 프로덕션 광고 ID 로 교체 (현재 테스트 ID)
- [ ] Microsoft Clarity project ID 프로덕션 값 확인
