package com.enftrms.prediction.collector;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * OpenDART — 공시보고서 원문 및 주요 재무계정 API.
 * 상장 경쟁사 분석에 사용. 경쟁사 매출/신규사업/설비투자/주요 제품 등.
 */
@Component
public class OpenDartCollector implements ExternalSignalCollector {
    @Override public String sourceCode() { return "DART"; }

    @Override
    public Map<String, Object> collect(String orgnId, String sbjtId, LocalDate from, LocalDate to) {
        Map<String, Object> out = new HashMap<>();
        out.put("industrySalesGrowth", 0d);
        return out;
    }
}
