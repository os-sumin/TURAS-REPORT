# 기술료 납부 가능성 예측모델 — 통합 개요

> 본 패치는 기존 EnFTRMS(Spring Boot + MyBatis + Oracle) 레포에 그대로 이식하는 것을 전제로 작성됨.
> 패키지: `com.enftrms.prediction.*` / DB 객체: `TFEE_PRED_*` 접두

## 1. 위치

| 화면 | 위치 | 설명 |
|---|---|---|
| ANL002 기업상세분석 | 기존 | "언론분석" 탭 옆에 **"기술료예측"** 탭 신설 |
| 신규 대시보드 | ANL003 (가칭) | 전체 현황 / 우선순위 / 산업별 비교 |
| 보고서 | 다운로드 버튼 | 과제별 PDF, 우선순위 XLSX |

## 2. 처리 흐름

```
[엑셀 업로드]                                 (TfeeAnswerUploadController)
     │  PS_ORGN_TFEE_CCLT / PS_TECH_IPMT_CNTR 컬럼 양식 그대로
     ▼
[TFEE_PRED_ANSWER]  ── 정답값 Y={0,1,제외} 도출
     │
     ▼
[POST /api/tech-fee-prediction/run]           (TechFeePredictionController)
     │
     ├─ 내부지표:   PS_SBJT, PS_ORGN, 재무자료, 기술료이력
     ├─ 외부지표:   TFEE_PRED_EXT_SIGNAL  (나라장터/KIPRIS/OpenDART/KOSIS)
     └─ 실행신호:   기존 company_news_analysis 재사용 (D5)
                  │
                  ▼
           [ScoreCardEngine]  5차원 × 20점 = 100점
                  │
                  ▼
           [TFEE_PRED_RESULT] + [TFEE_PRED_DIM_SCORE] + [TFEE_PRED_FACTOR]
                  │
        ┌─────────┴──────────┐
        ▼                    ▼
  [ANL002 탭 렌더링]    [보고서 생성기]
                       /report/pdf, /report/xlsx
```

## 3. 5차원 (TURAS 사업기획서 V장)

| # | 차원 | 배점 | 핵심 데이터 |
|---|---|---|---|
| D1 | 과제 사업화 적합성 | 20 | 과제명/KSIC 유사도, 종료 후 경과기간 |
| D2 | 기업 재무·사업화 체력 | 20 | 매출 증가율, 영업이익률, 부채비율 |
| D3 | R&D 자본효율·기술료 이력 | 20 | **엑셀 양식 = 정답값 원천** |
| D4 | 시장·산업 신호 | 20 | 나라장터 입찰, KIPRIS 출원, KOSIS |
| D5 | 기업 실행 신호 | 20 | 기존 언론분석 이벤트 분류 |

## 4. 등급 (사업기획서 STEP 7)

| 총점 | 등급 | 실무 조치 |
|---|---|---|
| 80 이상 | 매우 높음 | 우선 확인 및 증빙 요청 |
| 65 ~ 80 | 높음 | 기술실시 여부 확인 |
| 50 ~ 65 | 보통 | 모니터링 |
| 35 ~ 50 | 낮음 | 미실시 사유 확인 |
| 35 미만 | 매우 낮음 | 후순위 관리 |

## 5. 응답 형식

기존 EnFTRMS 규약을 그대로 따름.

```json
{ "success": true, "data": { ... }, "pagination": null }
```

## 6. 구현 단계

1. **(현재 패치)** D3 엔드투엔드 + 스코어카드 엔진 + 룰 YAML 외부화 + 엑셀 업로드 + 보고서 스켈레톤
2. D1/D2 룰 채우기 (재무 API 연결 후)
3. D4 외부 수집기 (나라장터 → KIPRIS → OpenDART 순)
4. D5 = 기존 언론분석 모듈에 `event_type` 분류 추가
5. 정답값 1,000건 누적 후 XGBoost 보정 모델 학습
