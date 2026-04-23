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
