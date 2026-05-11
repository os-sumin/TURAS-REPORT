package com.enftrms.prediction.service.dimension;

import com.enftrms.prediction.domain.OrgnFinancials;
import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.service.RuleEvaluator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class D2FinancialCapacityCalculator implements DimensionCalculator {

    @Override public String code()    { return "D2"; }
    @Override public String ruleKey() { return "D2_financial_capacity"; }

    @Override
    public DimensionScoreDto calculate(PredictionContext ctx, ScoreCardRuleSet.Dimension dim) {
        OrgnFinancials f = ctx.getFinancials();
        List<DimensionScoreDto.RuleDetail> detail = new ArrayList<>();
        double total = 0d;
        for (ScoreCardRuleSet.Rule rule : dim.getRules()) {
            Object input = inputFor(rule.getInput(), f);
            double s = RuleEvaluator.evaluate(rule, input);
            total += s;
            detail.add(DimensionScoreDto.RuleDetail.builder()
                    .ruleName(rule.getName()).score(s).max(rule.getMax()).inputValue(input).build());
        }
        return DimensionScoreDto.builder()
                .code(code()).label(dim.getLabel())
                .score(Math.min(total, dim.getWeight()))
                .maxScore(dim.getWeight()).detail(detail).build();
    }

    private Object inputFor(String key, OrgnFinancials f) {
        if (f == null) return null;
        switch (key) {
            case "salesGrowthRate":  return f.getSalesGrowthRate();
            case "operatingMargin":  return f.getOperatingMargin();
            case "debtRatio":        return f.getDebtRatio();
            case "cashToGovFund":    return f.getCashToGovFund();
            case "rndIntensity":     return f.getRndIntensity();
            case "assetGrowthRate":  return f.getAssetGrowthRate();
            default:                 return null;
        }
    }
}
