package com.enftrms.prediction.collector;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 나라장터 입찰공고정보서비스 — Open API.
 * 실 구현 시:
 *   - getBidPblancListInfoServc01  (입찰공고)
 *   - getScsbidListSttusServc01    (낙찰)
 *  결과 → TFEE_PRED_EXT_SIGNAL 적재 + g2bBidCount, g2bContractAmount 메트릭 산출
 */
@Component
public class G2bCollector implements ExternalSignalCollector {
    @Override public String sourceCode() { return "G2B"; }

    @Override
    public Map<String, Object> collect(String orgnId, String sbjtId, LocalDate from, LocalDate to) {
        Map<String, Object> out = new HashMap<>();
        out.put("g2bBidCount", 0d);
        out.put("g2bContractAmount", 0d);
        return out;
    }
}
