package com.enftrms.prediction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 한 건의 예측 실행에 필요한 모든 입력의 컨테이너.
 * 차원 계산기들이 이 컨텍스트에서 자신이 필요한 값을 꺼내 점수를 산출함.
 */
@Getter @Setter @Builder
public class PredictionContext {
    private String sbjtId;
    private String orgnId;
    private LocalDate predAt;

    private SubjectInfo subject;
    private OrgnFinancials financials;
    private TfeeHistory tfeeHistory;

    @Builder.Default
    private Map<String, Object> externalMetrics = new HashMap<>();    // D4

    @Builder.Default
    private Map<String, Integer> newsEventCounts = new HashMap<>();   // D5

    public double getMetric(String key, double defaultVal) {
        Object v = externalMetrics.get(key);
        if (v == null) return defaultVal;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return defaultVal; }
    }
}
