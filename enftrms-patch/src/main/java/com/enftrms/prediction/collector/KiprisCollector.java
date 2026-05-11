package com.enftrms.prediction.collector;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * KIPRIS Plus — 특허·실용신안 공개/등록공보 REST API.
 *   서지정보(출원번호, IPC, 출원인) → patentFilingGrowth 산출.
 */
@Component
public class KiprisCollector implements ExternalSignalCollector {
    @Override public String sourceCode() { return "KIPRIS"; }

    @Override
    public Map<String, Object> collect(String orgnId, String sbjtId, LocalDate from, LocalDate to) {
        Map<String, Object> out = new HashMap<>();
        out.put("patentFilingGrowth", 0d);
        return out;
    }
}
