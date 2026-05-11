package com.enftrms.prediction.service;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.TfeeHistory;
import com.enftrms.prediction.dto.PredictionResultDto;
import com.enftrms.prediction.service.dimension.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreCardEngineTest {

    static ScoreCardLoader loader;
    static ScoreCardEngine engine;

    @BeforeAll
    static void setup() throws Exception {
        loader = new ScoreCardLoader();
        ReflectionTestUtils.setField(loader, "ruleResource",
                new ClassPathResource("rules/scorecard.yml"));
        loader.load();
        engine = new ScoreCardEngine(loader, List.of(
                new D1ProjectFitCalculator(),
                new D2FinancialCapacityCalculator(),
                new D3CapitalEfficiencyCalculator(),
                new D4MarketSignalCalculator(),
                new D5ExecutionSignalCalculator()
        ));
    }

    @Test
    void runProducesFiveDimensionsAndGrade() {
        PredictionContext ctx = PredictionContext.builder()
                .sbjtId("S001").orgnId("O001")
                .tfeeHistory(TfeeHistory.builder()
                        .hasPastTfee(true).recoveryRatePercentile(0.5)
                        .projectRecoveryRate(0.15).monthsToFirstTfee(18).consecutiveYears(2).build())
                .build();

        PredictionResultDto r = engine.run(ctx);

        assertThat(r.getDimensions()).hasSize(5);
        assertThat(r.getGrade()).isNotNull();
        assertThat(r.getGrade().getCode()).isIn("VERY_LOW", "LOW", "MID", "HIGH", "VERY_HIGH");
        assertThat(r.getTotalScore()).isBetween(0.0, 100.0);
    }

    @Test
    void gradeBoundaries() {
        // 룰셋의 grade 임계값 확인
        var grades = loader.getRuleSet().getGrades();
        assertThat(grades).extracting("code")
                .containsExactly("VERY_HIGH", "HIGH", "MID", "LOW", "VERY_LOW");
    }
}
