package com.ejectbutton.data

import kotlin.random.Random

/**
 * 가짜 수신 전화 화면에 보일 **한국식 모바일 번호** 를 랜덤으로 만들어주는 유틸.
 *
 * 포맷: `폰 010-XXXX-XXXX`
 *
 * - 실 Galaxy One UI 8.5 수신 화면에서 연락처명 아래에 보이는 기본 라벨을 그대로 따른다
 *   (`Scenario.callerLabel` 과 동일한 접두사 "폰 ").
 * - 숫자는 7000~9999 범위에서 뽑아서 `010-0000-XXXX` 같은 비현실적인 번호는 피한다.
 * - Round 30 이후 "엄마"·"아빠" 프리셋(`isRandomPhone = true`) 의 `callerLabel` 이
 *   매 표시마다 달라지도록 UI 레이어에서 호출한다.
 *
 * 범위를 좁혀둔 이유 — 실제 KT·SKT·LGU+ 서브스크라이버 번호는 010 뒤 네 자리가
 * 0000 부터 시작하지 않는 경우가 대부분이라, 0000~0999 를 배제해두면 좀 더 실 번호처럼 보인다.
 */
internal fun randomKoreanMobileLabel(random: Random = Random.Default): String {
    val mid  = random.nextInt(1000, 10000)   // 1000..9999
    val tail = random.nextInt(1000, 10000)   // 1000..9999
    return "폰 010-%04d-%04d".format(mid, tail)
}

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
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return raw.trim()
    val c = country.uppercase()
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
