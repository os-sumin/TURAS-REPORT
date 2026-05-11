package com.enftrms.prediction.service.dimension;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.service.RuleEvaluator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * D4. 시장·산업 신호 — 외부 수집기(G2B/KIPRIS/DART/KOSIS)가 채운 externalMetrics 를 입력으로 사용.
 */
@Component
public class D4MarketSignalCalculator implements DimensionCalculator {

    @Override public String code()    { return "D4"; }
    @Override public String ruleKey() { return "D4_market_signal"; }

    @Override
    public DimensionScoreDto calculate(PredictionContext ctx, ScoreCardRuleSet.Dimension dim) {
        List<DimensionScoreDto.RuleDetail> detail = new ArrayList<>();
        double total = 0d;
        for (ScoreCardRuleSet.Rule rule : dim.getRules()) {
            double input = ctx.getMetric(rule.getInput(), 0d);
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
}
