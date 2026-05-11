package com.enftrms.prediction.service;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.ScoreCardRuleSet;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.dto.PredictionResultDto;
import com.enftrms.prediction.service.dimension.DimensionCalculator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 5차원 계산기 결과를 합산해 총점/등급/추천조치를 산출.
 */
@Service
public class ScoreCardEngine {

    private final ScoreCardLoader loader;
    private final List<DimensionCalculator> calculators;

    public ScoreCardEngine(ScoreCardLoader loader, List<DimensionCalculator> calculators) {
        this.loader = loader;
        // D1..D5 순으로 안정 정렬
        this.calculators = new ArrayList<>(calculators);
        this.calculators.sort(Comparator.comparing(DimensionCalculator::code));
    }

    public PredictionResultDto run(PredictionContext ctx) {
        ScoreCardRuleSet ruleSet = loader.getRuleSet();
        List<DimensionScoreDto> dims = new ArrayList<>();
        double total = 0d;

        for (DimensionCalculator c : calculators) {
            ScoreCardRuleSet.Dimension dim = ruleSet.getDimensions().get(c.ruleKey());
            if (dim == null) continue;
            DimensionScoreDto d = c.calculate(ctx, dim);
            dims.add(d);
            total += d.getScore();
        }
        total = round2(Math.max(0d, Math.min(100d, total)));

        ScoreCardRuleSet.Grade g = pickGrade(total, ruleSet.getGrades());

        return PredictionResultDto.builder()
                .sbjtId(ctx.getSbjtId())
                .orgnId(ctx.getOrgnId())
                .predAt(OffsetDateTime.now(ZoneOffset.ofHours(9)))
                .totalScore(total)
                .grade(PredictionResultDto.GradeDto.builder()
                        .code(g.getCode()).label(g.getLabel()).action(g.getAction()).build())
                .ruleVersion(ruleSet.getRuleVersion())
                .dimensions(dims)
                .build();
    }

    private static ScoreCardRuleSet.Grade pickGrade(double score, List<ScoreCardRuleSet.Grade> grades) {
        ScoreCardRuleSet.Grade fallback = grades.get(grades.size() - 1);
        return grades.stream()
                .filter(gr -> score >= gr.getMin())
                .max(Comparator.comparingDouble(ScoreCardRuleSet.Grade::getMin))
                .orElse(fallback);
    }

    private static double round2(double v) { return Math.round(v * 100d) / 100d; }
}
