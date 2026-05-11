package com.enftrms.prediction.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * D3 핵심 입력 — 엑셀 양식 PS_ORGN_TFEE_CCLT / PS_TECH_IPMT_CNTR 에서 산출.
 */
@Getter @Builder
public class TfeeHistory {
    /** 동일 기관 과거 기술료 발생 이력 존재 여부 */
    private final boolean hasPastTfee;
    /** 누적 기술료 / 누적 정부지원금  (당해 기관) */
    private final Double cumulativeRecoveryRate;
    /** 동종 과제군 분위순위 (0.0 ~ 1.0). 1.0=상위 */
    private final Double recoveryRatePercentile;
    /** 본 과제 회수율 = TFEE_AM / TTL_USE_GVSTM_AM */
    private final Double projectRecoveryRate;
    /** 과제 종료일 ~ 최초 기술료 발생일 (개월). 미발생이면 null */
    private final Integer monthsToFirstTfee;
    /** 연속 발생 연수 */
    private final int consecutiveYears;
}
