package com.enftrms.prediction.collector;

import java.time.LocalDate;
import java.util.Map;

/**
 * 외부 신호 수집기 공통 인터페이스.
 *  - G2B (나라장터), KIPRIS (특허), OpenDART (공시), KOSIS (통계), NTIS (R&D)
 *  본 패치는 스켈레톤만 정의. 실 구현은 출처별로 별도 작업.
 */
public interface ExternalSignalCollector {
    /** 출처 코드: G2B / KIPRIS / DART / KOSIS / NTIS */
    String sourceCode();

    /**
     * @param orgnId  기업 ID (또는 사업자번호)
     * @param sbjtId  과제 ID (선택)
     * @param from    수집 시작 날짜
     * @param to      수집 종료 날짜
     * @return 정규화된 메트릭 맵 — D4MarketSignalCalculator 가 읽는 키와 매칭
     */
    Map<String, Object> collect(String orgnId, String sbjtId, LocalDate from, LocalDate to);
}
