# enftrms-analysis 패치 — 기술료 납부 가능성 예측 (TECH_FEE 섹션)

기존 `enftrms-analysis` (FastAPI 기반 내부 AI 서비스) 의 **`/internal/ai/reports/generate`** 엔드포인트에서
`TECH_FEE` 섹션을 실제 5차원 스코어카드 예측으로 채우도록 하는 패치.

## 패치 적용

```bash
# enftrms-analysis 레포 루트에서
cp -R enftrms-analysis-patch/app/services/reports/tech_fee_prediction  app/services/reports/
cp    enftrms-analysis-patch/app/services/reports/heuristic_sections.py app/services/reports/
cp    enftrms-analysis-patch/tests/test_tech_fee_prediction.py          tests/
cp    enftrms-analysis-patch/examples/report-tech-fee.json              examples/

# 의존성 추가 — pyproject.toml
pip install pyyaml
```

`pyproject.toml`의 `dependencies` 에 `pyyaml>=6.0,<7.0` 추가.

## 변경 사항 (요약)

| 파일 | 종류 | 설명 |
|---|---|---|
| `app/services/reports/tech_fee_prediction/` | **신규** | 5차원 스코어카드 엔진 |
| `app/services/reports/tech_fee_prediction/scorecard.yml` | **신규** | 룰 외부화 (Java 패치와 내용 동일) |
| `app/services/reports/heuristic_sections.py` | **교체** | `TECH_FEE` 분기에서 예측 엔진 호출 |
| `tests/test_tech_fee_prediction.py` | **신규** | 11개 테스트 |
| `examples/report-tech-fee.json` | **신규** | 예시 payload |

## 흐름

```
POST /internal/ai/reports/generate
       │
       ▼ ReportService.generate
       │
       ▼ ReportLangGraphWorkflow.invoke
       │   ↓ heuristic 노드
       │     build_heuristic_sections()
       │       ↓ TECH_FEE 분기
       │         tech_fee_prediction.predict_and_write_section(request)
       │           ├─ input_adapter.build_context(request)
       │           │     analysis.techFee 또는 financial_inputs 에서 PredictionContext 생성
       │           ├─ engine.run(ctx)
       │           │     5차원 × 20점 = 100점 + 등급
       │           └─ section_writer.write_section(result)
       │                 한국어 plain text 본문 (개행 포함)
       │
       ▼ docx_renderer
              TECH_FEE 섹션을 DOCX 단락으로 변환
```

## 입력 payload 두 가지 형태

### 1) 명시적 형태 (권장)

```json
"analysis": {
  "techFee": {
    "tfeeHistory": {
      "hasPastTfee": true,
      "projectRecoveryRate": 0.18,
      "recoveryRatePercentile": 0.72,
      "monthsToFirstTfee": 14,
      "consecutiveYears": 2
    },
    "financials": { "salesGrowthRate": 0.18, "operatingMargin": 0.06 },
    "externalMetrics": { "g2bBidCount": 7, "patentFilingGrowth": 0.15 },
    "newsEventCounts": { "PRODUCT_LAUNCH": 1, "CERTIFICATION": 2 }
  }
}
```

### 2) 엑셀 양식 한 줄을 그대로

```json
"analysis": {
  "techFee": {
    "row": {
      "ttlPayGvstmAm": 100000000,
      "ttlUseGvstmAm": 99000000,
      "salesOccurYn": "Y",
      "tfeeAm": 200000,
      "techCtrbPt": 0.5,
      "baseYear": 2026
    }
  }
}
```

둘 다 없으면 기존 동작과 동일 ("Spring payload에 기술료 분석 데이터가 포함되지 않았습니다.")

## 5차원 (사업기획서 V장)

| # | 차원 | 배점 | 주요 입력 |
|---|---|---|---|
| D1 | 과제 사업화 적합성 | 20 | KSIC 유사도, 종료 후 경과기간 |
| D2 | 기업 재무·사업화 체력 | 20 | 매출 증가율, 영업이익률 (financial_inputs 자동 추출 지원) |
| D3 | R&D 자본효율·기술료 이력 | 20 | **엑셀 양식 직결** |
| D4 | 시장·산업 신호 | 20 | externalMetrics (나라장터/KIPRIS/DART) |
| D5 | 기업 실행 신호 | 20 | newsEventCounts (언론분석 서비스 결과 매핑) |

## 등급 (사업기획서 STEP 7)

| 총점 | 등급 | 추천 조치 |
|---|---|---|
| ≥ 80 | 매우 높음 | 우선 확인 및 증빙 요청 |
| ≥ 65 | 높음    | 기술실시 여부 확인 |
| ≥ 50 | 보통    | 모니터링 |
| ≥ 35 | 낮음    | 미실시 사유 확인 |
| < 35 | 매우 낮음 | 후순위 관리 |

## 룰 수정

`scorecard.yml` 만 수정. `ruleVersion` 을 올리는 것을 권장.
(별도 Java 패치 `enftrms-patch/.../scorecard.yml` 과는 내용 동기화 권장.)
