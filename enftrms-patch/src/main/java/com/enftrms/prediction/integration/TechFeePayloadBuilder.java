package com.enftrms.prediction.integration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * enftrms-analysis (Python FastAPI) `/internal/ai/reports/generate` 호출용
 * `analysis.techFee` 블록을 PS_ORGN_TFEE_CCLT 한 행 + 부가정보에서 조립.
 *
 * 본 빌더는 외부 라이브러리 의존성을 두지 않음 — 순수 Map 으로 출력.
 * 호출 측에서 ObjectMapper 로 직렬화하면 됨.
 */
public final class TechFeePayloadBuilder {

    private TechFeePayloadBuilder() {}

    /**
     * 엑셀 양식 한 행 + 선택 입력(재무 / 외부지표 / 뉴스 기사) → analysis.techFee 블록.
     */
    public static Map<String, Object> build(TechFeePayloadInput input) {
        Map<String, Object> techFee = new LinkedHashMap<>();

        if (input.row != null) {
            techFee.put("row", input.row.toMap());
        } else if (input.tfeeHistory != null) {
            techFee.put("tfeeHistory", input.tfeeHistory);
        }

        if (input.financials != null && !input.financials.isEmpty()) {
            techFee.put("financials", input.financials);
        }
        if (input.externalMetrics != null && !input.externalMetrics.isEmpty()) {
            techFee.put("externalMetrics", input.externalMetrics);
        }
        if (input.newsEventCounts != null && !input.newsEventCounts.isEmpty()) {
            techFee.put("newsEventCounts", input.newsEventCounts);
        } else if (input.articles != null && !input.articles.isEmpty()) {
            List<Map<String, Object>> serialized = new ArrayList<>();
            for (NewsArticle a : input.articles) {
                serialized.add(a.toMap());
            }
            techFee.put("articles", serialized);
        }

        if (input.predAt != null) {
            techFee.put("predAt", input.predAt.toString());
        }
        return techFee;
    }

    // ------------------------------------------------------------------
    //  Input value-objects
    // ------------------------------------------------------------------

    /** PS_ORGN_TFEE_CCLT 한 행. 컬럼명은 Python 측 input_adapter 와 1:1 매칭. */
    public static final class TfeeRow {
        public long ttlPayGvstmAm;          // 지급 정부지원금 (A)
        public long ttlUseGvstmAm;          // 실사용 정부지원금 (E = A - B - C - D)
        public String salesOccurYn = "N";   // 매출발생여부
        public long rndIncomeAm;            // R&D 수익금액 (L1)
        public double techCtrbPt;           // 기술기여도 (M1)
        public long tfeeAm;                 // 기술료 금액 (O)
        public Integer baseYear;            // 기준보고년도
        public LocalDate techIpmtCntrDe;    // 기술실시 계약일 (선택)

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ttlPayGvstmAm", ttlPayGvstmAm);
            m.put("ttlUseGvstmAm", ttlUseGvstmAm);
            m.put("salesOccurYn", salesOccurYn == null ? "N" : salesOccurYn);
            m.put("rndIncomeAm", rndIncomeAm);
            m.put("techCtrbPt", techCtrbPt);
            m.put("tfeeAm", tfeeAm);
            if (baseYear != null) m.put("baseYear", baseYear);
            if (techIpmtCntrDe != null) m.put("techIpmtCntrDe", techIpmtCntrDe.toString());
            return m;
        }
    }

    /** Python 측 분류기에 그대로 전달될 단순 기사 객체. */
    public static final class NewsArticle {
        public String title;
        public String summary;
        public String publishedAt;     // ISO yyyy-MM-dd
        public String link;

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (title       != null) m.put("title", title);
            if (summary     != null) m.put("summary", summary);
            if (publishedAt != null) m.put("publishedAt", publishedAt);
            if (link        != null) m.put("link", link);
            return m;
        }
    }

    /** 빌더 입력 컨테이너 — 필요한 필드만 채워 사용. */
    public static final class TechFeePayloadInput {
        public TfeeRow row;                                 // 엑셀 양식 한 행
        public Map<String, Object> tfeeHistory;             // row 대신 직접 제공할 때
        public Map<String, Object> financials;              // 매출증가율 등 — 없으면 financialInputs 로 자동
        public Map<String, Object> externalMetrics;         // 나라장터/KIPRIS/DART 수치
        public Map<String, Integer> newsEventCounts;        // 미리 분류된 카운트
        public List<NewsArticle> articles;                  // 분류 전 기사 목록 (Python 측 분류 위임)
        public LocalDate predAt;
    }

    // ------------------------------------------------------------------
    //  편의 빌더 — 가장 흔한 케이스 (DB 한 행 그대로)
    // ------------------------------------------------------------------

    public static Map<String, Object> fromRow(TfeeRow row) {
        TechFeePayloadInput in = new TechFeePayloadInput();
        in.row = row;
        return build(in);
    }

    public static Map<String, Object> fromRow(TfeeRow row, List<NewsArticle> articles) {
        TechFeePayloadInput in = new TechFeePayloadInput();
        in.row = row;
        in.articles = articles == null ? Collections.emptyList() : articles;
        return build(in);
    }
}
