package com.enftrms.prediction.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Builder
public class DimensionScoreDto {
    private final String code;        // D1 ~ D5
    private final String label;
    private final double score;
    private final double maxScore;
    private final List<RuleDetail> detail;

    /** MyBatis 저장용 — JSON 직렬화한 detail. API 응답에는 노출하지 않음 */
    @Setter @JsonIgnore
    private String detailJson;

    @Getter @Builder
    public static class RuleDetail {
        private final String ruleName;
        private final double score;
        private final double max;
        private final Object inputValue;
        private final String note;
    }
}
