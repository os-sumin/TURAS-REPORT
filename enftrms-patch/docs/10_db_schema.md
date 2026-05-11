# DB 스키마 — 기술료 예측모델

Oracle 기준. 기존 EnFTRMS 테이블(`PS_SBJT`, `PS_ORGN`, `PS_ORGN_TFEE_CCLT`, `PS_TECH_IPMT_CNTR` 등)은 **읽기 전용**으로 참조하며 별도 DDL 변경 없음.

## 신규 테이블

### TFEE_PRED_ANSWER (정답값 — 엑셀 업로드 적재)

엑셀 양식 컬럼이 그대로 들어옴. PS_ORGN_TFEE_CCLT의 컬럼명을 유지하여 본 레포 이식 시 view로 대체 가능.

| 컬럼 | 타입 | 비고 |
|---|---|---|
| SBJT_ID | VARCHAR2(50) | PK1 |
| FRUT_OWN_ORGN_ID | VARCHAR2(50) | PK2 (성과소유기관) |
| TECH_IPMT_ORGN_ID | VARCHAR2(50) | 기술실시기관 |
| BASE_YEAR | NUMBER(4) | 기준보고년도 |
| TTL_PAY_GVSTM_AM | NUMBER(18) | 지급 정부지원금 |
| TTL_USE_GVSTM_AM | NUMBER(18) | 실사용 정부지원금 |
| SALES_OCCUR_YN | CHAR(1) | 매출발생여부 |
| RND_INCOME_AM | NUMBER(18) | R&D 수익금액 L1 |
| TECH_CTRB_PT | NUMBER(7,4) | 기술기여도 M1 |
| TFEE_AM | NUMBER(18) | 기술료 금액 O |
| ANSWER_Y | NUMBER(1) | **라벨**: 1=발생, 0=미발생, NULL=관찰제외 |
| EXCEL_BATCH_ID | VARCHAR2(50) | 업로드 배치 추적 |
| FRST_REG_DT | TIMESTAMP | |
| LAST_MODF_DT | TIMESTAMP | |

```sql
ANSWER_Y 산정 규칙:
  CASE
    WHEN SALES_OCCUR_YN = 'Y' AND NVL(TFEE_AM,0) > 0 THEN 1
    WHEN MONTHS_BETWEEN(SYSDATE, SBJT.END_DE) < 12 THEN NULL  -- 관찰제외
    ELSE 0
  END
```

### TFEE_PRED_RESULT (예측 결과 헤더)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| PRED_ID | NUMBER(18) | PK (SEQ) |
| SBJT_ID | VARCHAR2(50) | |
| ORGN_ID | VARCHAR2(50) | |
| PRED_AT | TIMESTAMP | 예측 시점 (데이터 누수 방지 기준) |
| TOTAL_SCORE | NUMBER(5,2) | 0~100 |
| GRADE_CD | VARCHAR2(20) | VERY_HIGH/HIGH/MID/LOW/VERY_LOW |
| PROBABILITY_PT | NUMBER(5,4) | ML 보정값 (룰만일 땐 NULL) |
| RULE_VERSION | VARCHAR2(20) | scorecard.yml 버전 |
| RECOMM_ACTION | VARCHAR2(200) | |
| FRST_REG_DT | TIMESTAMP | |

### TFEE_PRED_DIM_SCORE (차원별 점수 — 5행/예측)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| PRED_ID | NUMBER(18) | FK |
| DIM_CD | VARCHAR2(10) | D1~D5 |
| DIM_SCORE | NUMBER(5,2) | 0~20 |
| RULE_DETAIL | CLOB | 점수 산출 내역 JSON |

### TFEE_PRED_FACTOR (긍정/부정/확인필요 요인)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| PRED_ID | NUMBER(18) | FK |
| FACTOR_SEQ | NUMBER(4) | |
| FACTOR_TP | VARCHAR2(10) | POSITIVE/NEGATIVE/CHECK |
| FACTOR_TEXT | VARCHAR2(500) | |
| SOURCE | VARCHAR2(50) | INTERNAL/NEWS/G2B/KIPRIS/DART |

### TFEE_PRED_EXT_SIGNAL (외부 신호 — 정규화 적재)

| 컬럼 | 타입 | 비고 |
|---|---|---|
| SIGNAL_ID | NUMBER(18) | PK |
| ORGN_ID | VARCHAR2(50) | |
| SBJT_ID | VARCHAR2(50) | NULL 허용 |
| SOURCE_CD | VARCHAR2(20) | G2B/KIPRIS/DART/KOSIS/NTIS |
| SIGNAL_TP | VARCHAR2(30) | BID_AWARDED/PATENT_FILED/REVENUE_UP ... |
| SIGNAL_DT | DATE | |
| AMOUNT | NUMBER(18) | |
| RAW_JSON | CLOB | 원본 응답 |
| COLLECTED_AT | TIMESTAMP | |

### TFEE_PRED_RULE_VER (룰셋 스냅샷)

스코어카드 룰을 변경할 때마다 YAML 전체를 행으로 보관. 재현성 확보용.

| 컬럼 | 타입 | 비고 |
|---|---|---|
| RULE_VERSION | VARCHAR2(20) | PK |
| RULE_YAML | CLOB | scorecard.yml 본문 |
| EFFECTIVE_FROM | DATE | |
| FRST_REG_DT | TIMESTAMP | |

## 시퀀스 / 인덱스

```sql
CREATE SEQUENCE SEQ_TFEE_PRED_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_TFEE_PRED_EXT START WITH 1 INCREMENT BY 1;
CREATE INDEX IX_TFEE_PRED_RESULT_01 ON TFEE_PRED_RESULT(SBJT_ID, ORGN_ID, PRED_AT DESC);
CREATE INDEX IX_TFEE_PRED_EXT_01 ON TFEE_PRED_EXT_SIGNAL(ORGN_ID, SIGNAL_DT DESC);
```
