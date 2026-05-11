package com.enftrms.prediction.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Builder
public class PredictionResultDto {
    private final Long predId;
    private final String sbjtId;
    private final String orgnId;
    private final OffsetDateTime predAt;
    private final double totalScore;
    private final GradeDto grade;
    private final Double probabilityPt;
    private final String ruleVersion;
    private final List<DimensionScoreDto> dimensions;
    private final FactorsDto factors;

    @Getter @Builder
    public static class GradeDto {
        private final String code;
        private final String label;
        private final String action;
    }

    @Getter @Builder
    public static class FactorsDto {
        private final List<String> positive;
        private final List<String> negative;
        private final List<String> check;
    }
}
