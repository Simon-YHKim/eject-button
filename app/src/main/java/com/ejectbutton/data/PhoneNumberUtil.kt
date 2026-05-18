package com.ejectbutton.data

import java.util.Locale
import kotlin.random.Random

/**
 * 가짜 수신 전화 화면에 보일 모바일 번호를 랜덤으로 만들어주는 유틸.
 *
 * 포맷 예시:
 *  - KR  : `폰 010-XXXX-XXXX`        (실 Galaxy One UI 레이블)
 *  - JP  : `携帯 090-XXXX-XXXX`
 *  - CN  : `手机 138-XXXX-XXXX`
 *  - TW  : `行動 09XX-XXX-XXX`
 *  - ES  : `Móvil +34 6XX XXX XXX`
 *  - IN  : `Mobile +91 9XXXX-XXXXX`
 *  - else: `Mobile +1 5XX-XXX-XXXX` (or generic 10-digit)
 *
 * v1.6.9 — 한국어 prefix 하드코딩 i18n bug 수정 (`randomKoreanMobileLabel` →
 *   `randomMobileLabel`). 영어/스페인어/힌디 사용자도 한국어 "폰 " 안 봄.
 *   기존 호출처는 호환 위해 `randomKoreanMobileLabel` alias 남김.
 *
 * - Round 30 이후 "엄마"·"아빠" 프리셋(`isRandomPhone = true`) 의 `callerLabel` 이
 *   매 표시마다 달라지도록 UI 레이어에서 호출한다.
 * - 숫자 범위 1000~9999 → `010-0000-XXXX` 같은 비현실적인 번호는 피한다.
 */
fun randomMobileLabel(
    locale: Locale = Locale.getDefault(),
    random: Random = Random.Default,
): String {
    val country = locale.country.uppercase(Locale.ROOT)
    val mid  = random.nextInt(1000, 10000)
    val tail = random.nextInt(1000, 10000)
    return when (country) {
        "KR" -> "폰 010-%04d-%04d".format(mid, tail)
        "JP" -> "携帯 090-%04d-%04d".format(mid, tail)
        "CN" -> "手机 138-%04d-%04d".format(mid, tail)
        "TW" -> "行動 09%02d-%03d-%03d".format(mid % 100, tail % 1000, (mid + tail) % 1000)
        "HK" -> "流動 9%03d-%04d".format(mid % 1000, tail)
        "ES" -> "Móvil +34 6%02d %03d %03d".format(mid % 100, tail % 1000, (mid + tail) % 1000)
        "IN" -> "Mobile +91 9%04d-%04d".format(mid % 10000, tail)
        "US", "CA" -> "Mobile +1 5%02d-%03d-%04d".format(mid % 100, tail % 1000, mid * tail % 10000)
        else -> "Mobile %04d-%04d".format(mid, tail)
    }
}

/** Backward-compat alias (v1.6.9 이전 호출처 호환). 새 코드는 `randomMobileLabel` 사용. */
internal fun randomKoreanMobileLabel(random: Random = Random.Default): String =
    randomMobileLabel(Locale.KOREA, random)

/**
 * 아무 포맷의 전화번호 문자열을 받아 국가별 하이픈 포맷으로 재조립한다.
 *
 * Round 32 — 주소록 NUMBER 컬럼은 공백·괄호·점·국가코드 등이 섞여서 온다.
 * 예: "+82 10-2484-1120", "010 1234 5678", "(032)123-4567".
 * 이걸 그대로 가짜 수신 화면에 박으면 포맷이 들쑥날쑥이라 가짜 통화 몰입이 깨진다.
 *
 * 전략:
 * 1) 숫자만 추출.
 * 2) 국가별 대표 길이 패턴에 맞추어 하이픈 넣기.
 * 3) 매칭 안 되면 원본 숫자만 그대로 반환 (망가뜨리지 않음).
 *
 * 지원 국가: KR (기본), US/CA, JP, CN/TW/HK, ES, IN.
 * 그 외: 11자리 → 3-4-4, 10자리 → 3-3-4 의 한국식 fallback.
 */
fun formatPhoneWithHyphens(raw: String, country: String = "KR"): String {
    if (raw.isBlank()) return raw
    val rawDigits = raw.filter { it.isDigit() }
    val c = country.uppercase()
    val digits = when {
        c == "KR" && rawDigits.startsWith("82") && rawDigits.length in 11..12 ->
            "0" + rawDigits.drop(2)
        else -> rawDigits
    }
    if (digits.isEmpty()) return raw.trim()
    return when (c) {
        "KR" -> when (digits.length) {
            11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
            10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> digits
        }
        "US", "CA" -> when (digits.length) {
            10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            11 -> "+1 ${digits.substring(1, 4)}-${digits.substring(4, 7)}-${digits.substring(7)}"
            else -> digits
        }
        "JP" -> when (digits.length) {
            11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
            10 -> "${digits.take(2)}-${digits.substring(2, 6)}-${digits.substring(6)}"
            else -> digits
        }
        "CN", "TW", "HK" -> when (digits.length) {
            11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
            else -> digits
        }
        "ES" -> when (digits.length) {
            9 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> digits
        }
        "IN" -> when (digits.length) {
            10 -> "${digits.take(5)}-${digits.substring(5)}"
            else -> digits
        }
        else -> when (digits.length) {
            11 -> "${digits.take(3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
            10 -> "${digits.take(3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> digits
        }
    }
}
