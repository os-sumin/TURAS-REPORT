package com.enftrms.prediction.service.dimension;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.domain.TfeeHistory;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.service.RuleEvaluator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * D3. R&D 자본효율·기술료 이력 (20점).
 * 엑셀 양식(TFEE_PRED_ANSWER) 의 집계값을 직접 사용.
 */
@Component
public class D3CapitalEfficiencyCalculator implements DimensionCalculator {

    @Override public String code()    { return "D3"; }
    @Override public String ruleKey() { return "D3_capital_efficiency"; }

    @Override
    public DimensionScoreDto calculate(PredictionContext ctx, ScoreCardRuleSet.Dimension dim) {
        TfeeHistory h = ctx.getTfeeHistory();
        List<DimensionScoreDto.RuleDetail> detail = new ArrayList<>();
        double total = 0d;

        for (ScoreCardRuleSet.Rule rule : dim.getRules()) {
            Object input = inputFor(rule.getInput(), h);
            double s = RuleEvaluator.evaluate(rule, input);
            total += s;
            detail.add(DimensionScoreDto.RuleDetail.builder()
                    .ruleName(rule.getName())
                    .score(s)
                    .max(rule.getMax())
                    .inputValue(input)
                    .build());
        }
        total = Math.min(total, dim.getWeight());

        return DimensionScoreDto.builder()
                .code(code())
                .label(dim.getLabel())
                .score(round2(total))
                .maxScore(dim.getWeight())
                .detail(detail)
                .build();
    }

    private Object inputFor(String key, TfeeHistory h) {
        if (h == null) return null;
        switch (key) {
            case "hasPastTfee":            return h.isHasPastTfee();
            case "recoveryRatePercentile": return h.getRecoveryRatePercentile();
            case "projectRecoveryRate":    return h.getProjectRecoveryRate();
            case "monthsToFirstTfee":      return h.getMonthsToFirstTfee();
            case "consecutiveYears":       return h.getConsecutiveYears();
            default:                       return null;
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100d) / 100d;
    }
}
