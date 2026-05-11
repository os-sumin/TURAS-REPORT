# TURAS — 기술료 납부 가능성 예측모델

두 가지 산출물이 공존합니다. **`scorecard.yml` 룰셋**의 내용은 두 산출물이 공유 — 룰 수정은 양쪽 다 반영하면 됩니다.

| 산출물 | 위치 | 통합 대상 | 출력 |
|---|---|---|---|
| **enftrms-analysis 패치** (메인) | `enftrms-analysis-patch/` | 기존 FastAPI 내부 AI 서비스 | **DOCX 최종보고서의 TECH_FEE 섹션** |
| EnFTRMS 백엔드 패치 | `enftrms-patch/` | Spring Boot + MyBatis + Oracle 본 레포 | 예측 API + 우선순위 XLSX |

## 1. enftrms-analysis 패치 (Python — 메인)

`/internal/ai/reports/generate` 의 `TECH_FEE` 섹션이 단순 stringify 대신 **실제 5차원 예측**을 수행하도록 추가.

- 신규: `app/services/reports/tech_fee_prediction/` — 엔진, 5개 차원, scorecard.yml, input_adapter, section_writer
- 교체: `app/services/reports/heuristic_sections.py` — TECH_FEE 분기에서 예측 엔진 호출
- 신규: `tests/test_tech_fee_prediction.py` — 13개 테스트
- 신규: `examples/report-tech-fee.json` — 예시 payload

검증: 패치 적용 후 **upstream 전체 27/27 pytest 통과**, 예시 payload로 38KB DOCX 정상 생성.

상세: `enftrms-analysis-patch/README.md`

## 2. EnFTRMS 백엔드 패치 (Java/Spring Boot — 운영 시스템 통합용)

기존 EnFTRMS 본 레포에 `com.enftrms.prediction` 패키지 추가:

- Oracle DDL (TFEE_PRED_* 테이블 + 정답값 view)
- 5차원 스코어카드 엔진 + 차원 계산기 (Java 측 자체 예측 — 운영 화면용)
- 엑셀 업로드 → 정답값 DB 적재
- 예측 API + 우선순위 XLSX 다운로드
- **`integration/`** — Python 최종보고서 서비스 호출 어댑터
  - `TechFeePayloadBuilder` : PS_ORGN_TFEE_CCLT 한 행 → `analysis.techFee` JSON
  - `EnftrmsAnalysisClient` : `/internal/ai/reports/generate` WebClient
  - `TechFeeReportOrchestrator` : 원샷 호출 — DB 행 + 기사 목록 → DOCX bytes

상세: `enftrms-patch/docs/00_overview.md`

## 전체 흐름

```
PS_ORGN_TFEE_CCLT (Oracle)
        │
        ▼
Spring Boot 컨트롤러 (운영 시스템)
        │
        ▼
TechFeeReportOrchestrator.generate(row, articles)
        │  TechFeePayloadBuilder.fromRow(row, articles)
        │      → analysis.techFee = { row, articles }
        ▼
EnftrmsAnalysisClient.generateReport(req)
        │  POST /internal/ai/reports/generate (X-Internal-Token)
        ▼
─────────── Python AI 서비스 (enftrms-analysis) ───────────
heuristic_sections.TECH_FEE 분기
        │  input_adapter.build_context(req)
        │  ├─ row 해석 → TfeeHistory
        │  └─ articles → news_event_classifier → newsEventCounts (D5)
        │  engine.run(ctx)  → 5차원 점수
        │  section_writer  → 한국어 본문
        ▼
docx_renderer  → TECH_FEE 섹션 + 차원별 점수 Word 표
        ▼
DOCX (base64) 반환
─────────────────────────────────────────────────────────
        │
        ▼
Spring Boot → 사용자 다운로드
```

## 룰 단일 출처

두 산출물 모두 동일한 `scorecard.yml` 구조 사용:

- Python: `enftrms-analysis-patch/app/services/reports/tech_fee_prediction/scorecard.yml`
- Java:   `enftrms-patch/src/main/resources/rules/scorecard.yml`

룰을 바꾸려면 두 파일을 함께 갱신.
