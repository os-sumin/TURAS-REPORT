# API 명세 — 기술료 예측

기존 EnFTRMS 규약(`success` / `data` / `pagination`) 준수. 인증 필요, 권한: `SYSTEM_ADMIN`.

## 1. 예측 실행

`POST /api/tech-fee-prediction/run`

```json
// Request
{ "sbjtId": "S2022-00141998", "orgnId": "O00012345", "forceRefresh": false }

// Response 200
{
  "success": true,
  "data": {
    "predId": 10001,
    "sbjtId": "S2022-00141998",
    "orgnId": "O00012345",
    "predAt": "2026-05-11T14:00:00+09:00",
    "totalScore": 72.5,
    "grade": { "code": "HIGH", "label": "높음", "action": "기술실시 여부 확인" },
    "probabilityPt": null,
    "ruleVersion": "v1.0.0",
    "dimensions": [
      { "code": "D1", "label": "과제 사업화 적합성",     "score": 14.0, "maxScore": 20, "detail": [...] },
      { "code": "D2", "label": "기업 재무·사업화 체력",  "score": 12.5, "maxScore": 20, "detail": [...] },
      { "code": "D3", "label": "R&D 자본효율·기술료 이력","score": 18.0, "maxScore": 20, "detail": [...] },
      { "code": "D4", "label": "시장·산업 신호",         "score": 16.0, "maxScore": 20, "detail": [...] },
      { "code": "D5", "label": "기업 실행 신호",         "score": 12.0, "maxScore": 20, "detail": [...] }
    ],
    "factors": {
      "positive": ["관련 시장 공공입찰 증가", "매출 증가", "제품 인증 뉴스"],
      "negative": ["영업이익 적자 지속", "과거 기술료 납부 이력 없음"],
      "check":    ["기술실시 및 매출 발생 여부 확인"]
    }
  }
}
```

## 2. 예측 결과 조회 (최근 1건 — 캐시)

`GET /api/tech-fee-prediction/result?sbjtId=...&orgnId=...`

응답은 1번과 동일. 없으면 404 `NOT_FOUND`.

## 3. 배치 예측

`POST /api/tech-fee-prediction/batch`

```json
{ "sbjtIds": [...], "orgnIds": [...], "predAt": "2026-05-11" }
```

```json
{ "success": true, "data": { "jobId": "pred-20260511-001", "total": 250, "status": "PROCESSING" } }
```

## 4. 정답값 엑셀 업로드

`POST /api/tech-fee-prediction/answers/upload` (multipart)

- 파일: 사용자가 제공한 양식(`PS_ORGN_TFEE_CCLT` 시트)
- 처리: 헤더 자동 탐지(2~6행), 데이터 행 적재 → `TFEE_PRED_ANSWER`

```json
{ "success": true, "data": { "batchId": "EXL-20260511-001", "inserted": 137, "skipped": 4, "errors": [] } }
```

## 5. 보고서

| 경로 | 형식 | 설명 |
|---|---|---|
| `GET /api/tech-fee-prediction/{predId}/report.pdf` | PDF | 과제별 예측 리포트 (외부 보고용 등급·문구 변환 적용) |
| `GET /api/tech-fee-prediction/priority.xlsx?grade=HIGH&grade=VERY_HIGH` | XLSX | 우선순위 목록 |

## 6. 우선순위 / 대시보드

| 경로 | 설명 |
|---|---|
| `GET /api/tech-fee-prediction/dashboard/summary` | 전체현황 (등급별 카운트, 산업별 분포) |
| `GET /api/tech-fee-prediction/dashboard/priority?grade=...&page=&size=` | 우선순위 페이징 목록 |
| `GET /api/tech-fee-prediction/dashboard/industry-compare` | 산업별 평균 점수/발생률 |
