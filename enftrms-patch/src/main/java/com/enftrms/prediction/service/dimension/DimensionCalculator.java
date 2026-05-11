package com.enftrms.prediction.service.dimension;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.dto.DimensionScoreDto;

public interface DimensionCalculator {
    /** D1 ~ D5 */
    String code();

    /** scorecard.yml 의 키 (예: D3_capital_efficiency) */
    String ruleKey();

    DimensionScoreDto calculate(PredictionContext ctx, ScoreCardRuleSet.Dimension dim);
}
