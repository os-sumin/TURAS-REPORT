package com.enftrms.prediction.service.dimension;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.dto.DimensionScoreDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * D5. 기업 실행 신호 — 기존 언론분석 모듈이 채운 newsEventCounts(이벤트 타입 → 건수) 합산.
 *  eventScores * count 합 후 [0, 20] 클리핑.
 */
@Component
public class D5ExecutionSignalCalculator implements DimensionCalculator {

    @Override public String code()    { return "D5"; }
    @Override public String ruleKey() { return "D5_execution_signal"; }

    @Override
    public DimensionScoreDto calculate(PredictionContext ctx, ScoreCardRuleSet.Dimension dim) {
        Map<String, Double> scores = dim.getEventScores();
        Map<String, Integer> counts = ctx.getNewsEventCounts();
        List<DimensionScoreDto.RuleDetail> detail = new ArrayList<>();

        double total = 0d;
        if (scores != null && counts != null) {
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                Double per = scores.get(e.getKey());
                if (per == null) continue;
                double contrib = per * e.getValue();
                total += contrib;
                detail.add(DimensionScoreDto.RuleDetail.builder()
                        .ruleName(e.getKey()).score(contrib).max(dim.getWeight())
                        .inputValue(e.getValue()).build());
            }
        }
        double clipped = Math.max(0d, Math.min(dim.getWeight(), total));
        return DimensionScoreDto.builder()
                .code(code()).label(dim.getLabel())
                .score(clipped).maxScore(dim.getWeight()).detail(detail).build();
    }
}
