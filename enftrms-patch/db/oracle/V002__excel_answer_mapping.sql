-- ====================================================================
-- 정답값 산출 — PS_ORGN_TFEE_CCLT (실 운영 테이블) ↔ TFEE_PRED_ANSWER
-- ====================================================================
-- 본 레포 이식 시: TFEE_PRED_ANSWER 테이블을 아래 view 로 대체 가능.
-- 패치 단계에서는 엑셀 업로드로 TFEE_PRED_ANSWER 에 직접 적재함.

CREATE OR REPLACE VIEW V_TFEE_PRED_ANSWER_FROM_OPS AS
SELECT
    O.SBJT_ID,
    O.FRUT_OWN_ORGN_ID,
    C.TECH_IPMT_ORGN_ID,
    EXTRACT(YEAR FROM O.FRST_REG_DT)               AS BASE_YEAR,
    O.TTL_PAY_GVSTM_AM,
    O.TTL_USE_GVSTM_AM,
    CASE WHEN NVL(F.TFEE_AM, 0) > 0 THEN 'Y' ELSE 'N' END  AS SALES_OCCUR_YN,
    NVL(F.RND_INCOME_AM, 0)                        AS RND_INCOME_AM,
    NVL(C.TFEE_BNDS_PT, 0)                         AS TECH_CTRB_PT,
    NVL(F.TFEE_AM, 0)                              AS TFEE_AM,
    -- 정답값 라벨:
    --   1 = 발생, 0 = 미발생, NULL = 관찰 제외 (종료 후 12개월 미만)
    CASE
      WHEN NVL(F.TFEE_AM, 0) > 0                                   THEN 1
      WHEN S.SBJT_END_DE IS NULL                                   THEN NULL
      WHEN MONTHS_BETWEEN(SYSDATE, S.SBJT_END_DE) < 12             THEN NULL
      ELSE 0
    END                                            AS ANSWER_Y,
    NULL                                           AS EXCEL_BATCH_ID,
    O.FRST_REG_DT,
    O.LAST_MODF_DT
FROM PS_ORGN_TFEE_CCLT O
LEFT JOIN PS_TECH_IPMT_CNTR C
       ON C.SBJT_ID = O.SBJT_ID
      AND C.TFEE_CCLT_PRG_SN = O.TFEE_CCLT_PRG_SN
LEFT JOIN (
    -- 연도별 실적 보고 (PS_TFEE_PAYM_RPRT 가정)
    SELECT SBJT_ID, IPMT_CNTR_PRG_SN,
           SUM(NVL(RND_INCOME_AM, 0)) AS RND_INCOME_AM,
           SUM(NVL(TFEE_AM, 0))       AS TFEE_AM
      FROM PS_TFEE_PAYM_RPRT
     GROUP BY SBJT_ID, IPMT_CNTR_PRG_SN
) F ON F.SBJT_ID = C.SBJT_ID AND F.IPMT_CNTR_PRG_SN = C.IPMT_CNTR_PRG_SN
LEFT JOIN PS_SBJT S ON S.SBJT_ID = O.SBJT_ID;
