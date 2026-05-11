package com.enftrms.prediction.service;

import com.enftrms.prediction.domain.PredictionContext;
import com.enftrms.prediction.domain.SubjectInfo;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.dto.PredictionResultDto;
import com.enftrms.prediction.mapper.PredictionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PredictionService {

    private final PredictionMapper mapper;
    private final ScoreCardEngine engine;
    private final ScoreCardLoader loader;
    private final ObjectMapper json = new ObjectMapper();

    public PredictionService(PredictionMapper mapper, ScoreCardEngine engine, ScoreCardLoader loader) {
        this.mapper = mapper; this.engine = engine; this.loader = loader;
    }

    @Transactional
    public PredictionResultDto run(String sbjtId, String orgnId, LocalDate predAt, boolean forceRefresh) {
        LocalDate ref = predAt != null ? predAt : LocalDate.now();

        SubjectInfo subject = mapper.selectSubjectInfo(sbjtId);
        PredictionContext ctx = PredictionContext.builder()
                .sbjtId(sbjtId).orgnId(orgnId).predAt(ref)
                .subject(subject)
                .financials(mapper.selectOrgnFinancials(orgnId, ref))
                .tfeeHistory(mapper.selectTfeeHistory(sbjtId, orgnId, ref))
                .build();

        PredictionResultDto result = engine.run(ctx);
        long predId = mapper.nextPredId();

        for (DimensionScoreDto d : result.getDimensions()) {
            try { d.setDetailJson(json.writeValueAsString(d.getDetail())); }
            catch (JsonProcessingException ignored) { d.setDetailJson("[]"); }
        }

        mapper.insertRuleVersionIfAbsent(loader.getRuleVersion(), loader.getRawYaml());
        mapper.insertResult(predId, sbjtId, orgnId,
                result.getTotalScore(), result.getGrade().getCode(),
                result.getRuleVersion(), result.getGrade().getAction());
        mapper.insertDimensionScores(predId, result.getDimensions());

        return PredictionResultDto.builder()
                .predId(predId)
                .sbjtId(result.getSbjtId())
                .orgnId(result.getOrgnId())
                .predAt(result.getPredAt())
                .totalScore(result.getTotalScore())
                .grade(result.getGrade())
                .ruleVersion(result.getRuleVersion())
                .dimensions(result.getDimensions())
                .build();
    }

    public PredictionResultDto latest(String sbjtId, String orgnId) {
        PredictionResultDto base = mapper.selectLatestResult(sbjtId, orgnId);
        if (base == null) return null;
        List<DimensionScoreDto> dims = mapper.selectDimensionScores(base.getPredId());
        return PredictionResultDto.builder()
                .predId(base.getPredId())
                .sbjtId(base.getSbjtId())
                .orgnId(base.getOrgnId())
                .predAt(base.getPredAt())
                .totalScore(base.getTotalScore())
                .ruleVersion(base.getRuleVersion())
                .dimensions(dims)
                .build();
    }
}
