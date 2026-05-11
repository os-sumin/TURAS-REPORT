package com.enftrms.prediction.service.dimension;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.domain.TfeeHistory;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.service.ScoreCardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D3 — 엑셀 양식 직접 매핑 차원. 회수율·이력 입력 → 점수 산출 검증.
 */
class D3CapitalEfficiencyCalculatorTest {

    static ScoreCardLoader loader;

    @BeforeAll
    static void setup() throws Exception {
        loader = new ScoreCardLoader();
        ReflectionTestUtils.setField(loader, "ruleResource",
                new ClassPathResource("rules/scorecard.yml"));
        loader.load();
    }

    private ScoreCardRuleSet.Dimension dim() {
        return loader.getRuleSet().getDimensions().get("D3_capital_efficiency");
    }

    @Test
    void allHigh_shouldBeNear20() {
        TfeeHistory h = TfeeHistory.builder()
                .hasPastTfee(true)
                .cumulativeRecoveryRate(0.30)
                .recoveryRatePercentile(0.95)     // 상위 5%
                .projectRecoveryRate(0.25)
                .monthsToFirstTfee(8)
                .consecutiveYears(3)
                .build();
        PredictionContext ctx = PredictionContext.builder().tfeeHistory(h).build();

        DimensionScoreDto result = new D3CapitalEfficiencyCalculator().calculate(ctx, dim());

        // 6(이력) + 5(percentile=0.95→4.75) + 4(>=0.2) + 3(<=12mo) + 2(2년+)
        // ≈ 19.75 → cap 20
        assertThat(result.getScore()).isBetween(19.0, 20.0);
        assertThat(result.getMaxScore()).isEqualTo(20.0);
        assertThat(result.getCode()).isEqualTo("D3");
    }

    @Test
    void allLow_shouldBeZero() {
        TfeeHistory h = TfeeHistory.builder()
                .hasPastTfee(false)
                .cumulativeRecoveryRate(0d)
                .recoveryRatePercentile(0d)
                .projectRecoveryRate(0d)
                .monthsToFirstTfee(null)
                .consecutiveYears(0)
                .build();
        PredictionContext ctx = PredictionContext.builder().tfeeHistory(h).build();

        DimensionScoreDto result = new D3CapitalEfficiencyCalculator().calculate(ctx, dim());
        // projectRecoveryRate=0 → threshold "{gte:0.0, score:1}" 매칭하므로 +1
        assertThat(result.getScore()).isBetween(0.0, 1.5);
    }

    @Test
    void midRange_shouldBeBetween8and14() {
        TfeeHistory h = TfeeHistory.builder()
                .hasPastTfee(true)              // +6
                .recoveryRatePercentile(0.40)   // 40%ile → 2.0
                .projectRecoveryRate(0.07)      // gte 0.05 → +2
                .monthsToFirstTfee(20)          // 12~24 → +2
                .consecutiveYears(1)            // 1년만 → +1
                .build();
        PredictionContext ctx = PredictionContext.builder().tfeeHistory(h).build();

        DimensionScoreDto result = new D3CapitalEfficiencyCalculator().calculate(ctx, dim());
        // 6 + 2 + 2 + 2 + 1 = 13.0
        assertThat(result.getScore()).isBetween(12.0, 14.0);
    }

    @Test
    void nullHistory_shouldNotThrow() {
        PredictionContext ctx = PredictionContext.builder().tfeeHistory(null).build();
        DimensionScoreDto result = new D3CapitalEfficiencyCalculator().calculate(ctx, dim());
        assertThat(result.getScore()).isEqualTo(0.0);
    }
}
