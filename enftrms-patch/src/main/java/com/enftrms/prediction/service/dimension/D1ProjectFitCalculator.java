package com.enftrms.prediction.service.dimension;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.service.RuleEvaluator;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class D1ProjectFitCalculator implements DimensionCalculator {

    @Override public String code()    { return "D1"; }
    @Override public String ruleKey() { return "D1_project_fit"; }

    @Override
    public DimensionScoreDto calculate(PredictionContext ctx, ScoreCardRuleSet.Dimension dim) {
        List<DimensionScoreDto.RuleDetail> detail = new ArrayList<>();
        double total = 0d;
        for (ScoreCardRuleSet.Rule rule : dim.getRules()) {
            Object input = inputFor(rule.getInput(), ctx);
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

    private Object inputFor(String key, PredictionContext ctx) {
        switch (key) {
            case "ksicSimilarity":
                // TODO: 임베딩 유사도 모듈 연결 전까지 외부 메트릭에서 끌어옴
                return ctx.getMetric("ksicSimilarity", 0d);
            case "productizationKeywordCount":
                return ctx.getMetric("productizationKeywordCount", 0d);
            case "monthsSinceEnd":
                if (ctx.getSubject() == null || ctx.getSubject().getEndDe() == null
                        || ctx.getPredAt() == null) return 0d;
                return (double) ChronoUnit.MONTHS.between(ctx.getSubject().getEndDe(), ctx.getPredAt());
            default: return null;
        }
    }
}
