package com.enftrms.prediction.mapper;

import com.enftrms.prediction.domain.OrgnFinancials;
import com.enftrms.prediction.domain.SubjectInfo;
import com.enftrms.prediction.domain.TfeeHistory;
import com.enftrms.prediction.dto.DimensionScoreDto;
import com.enftrms.prediction.dto.PredictionResultDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PredictionMapper {

    /* ===== 입력 데이터 ===== */
    SubjectInfo selectSubjectInfo(@Param("sbjtId") String sbjtId);

    OrgnFinancials selectOrgnFinancials(@Param("orgnId") String orgnId,
                                        @Param("predAt") java.time.LocalDate predAt);

    /** D3 핵심 — TFEE_PRED_ANSWER 기반 집계 */
    TfeeHistory selectTfeeHistory(@Param("sbjtId") String sbjtId,
                                  @Param("orgnId") String orgnId,
                                  @Param("predAt") java.time.LocalDate predAt);

    /** 동종 과제군 누적 회수율 분포 — percentile 계산용 */
    List<Double> selectPeerRecoveryRates(@Param("ksic") String ksic);

    /* ===== 결과 저장 ===== */
    long nextPredId();

    int insertResult(@Param("predId") long predId,
                     @Param("sbjtId") String sbjtId,
                     @Param("orgnId") String orgnId,
                     @Param("totalScore") double totalScore,
                     @Param("gradeCd") String gradeCd,
                     @Param("ruleVersion") String ruleVersion,
                     @Param("recommAction") String recommAction);

    int insertDimensionScores(@Param("predId") long predId,
                              @Param("dims") List<DimensionScoreDto> dims);

    int insertFactors(@Param("predId") long predId,
                      @Param("factors") List<Map<String, Object>> factors);

    /* ===== 조회 ===== */
    PredictionResultDto selectLatestResult(@Param("sbjtId") String sbjtId,
                                           @Param("orgnId") String orgnId);

    List<DimensionScoreDto> selectDimensionScores(@Param("predId") long predId);

    List<Map<String, Object>> selectFactors(@Param("predId") long predId);

    List<PredictionResultDto> selectPriorityList(@Param("grades") List<String> grades,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    long countPriorityList(@Param("grades") List<String> grades);

    /* ===== 엑셀 업로드 ===== */
    int insertAnswerBatch(@Param("rows") List<Map<String, Object>> rows);

    /* ===== 룰셋 스냅샷 ===== */
    int insertRuleVersionIfAbsent(@Param("version") String version,
                                  @Param("yaml") String yaml);
}
