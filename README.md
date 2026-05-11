# TURAS — 기술료 납부 가능성 예측모델 패치

기존 EnFTRMS(Spring Boot + MyBatis + Oracle) 레포에 통합할 수 있는 형태의 패치 모듈.

```
enftrms-patch/
├── docs/                         # 통합 개요, DB 스키마, API 명세, 스코어카드 산식
├── db/oracle/                    # DDL V001 ~ V002
├── pom.xml                       # 단독 빌드용 (합본 시 제거)
└── src/
    ├── main/java/com/enftrms/prediction/
    │   ├── controller/           # /api/tech-fee-prediction/*
    │   ├── service/              # ScoreCardEngine + 차원 계산기 + 보고서
    │   ├── collector/            # 외부 신호 수집기 (G2B/KIPRIS/DART 스켈레톤)
    │   ├── domain/, dto/, mapper/
    ├── main/resources/
    │   ├── rules/scorecard.yml   # 5차원 × 20점 = 100점 산식 (외부화)
    │   └── mapper/prediction/    # MyBatis XML
    └── test/                     # D3 + ScoreCardEngine 단위 테스트
```

상세는 `enftrms-patch/docs/00_overview.md` 참조.

## 현재 패치 범위

- ✅ 5차원 스코어카드 엔진 (D1~D5) — 룰 YAML 외부화
- ✅ D3 (R&D 자본효율·기술료 이력) 엔드투엔드 — 엑셀 양식 → 정답값 → 점수
- ✅ 엑셀 업로드 API (`PS_ORGN_TFEE_CCLT` 양식 그대로)
- ✅ 예측 실행/조회 API + HTML 리포트 + 우선순위 XLSX 다운로드
- ✅ 등급/추천조치 (사업기획서 STEP 7 기준)
- ⏳ D1/D2 (재무·KSIC 유사도) — 수식만 외부화, 실제 입력 연결 필요
- ⏳ D4 외부 수집기 — 인터페이스만, 실 API 호출 미구현
- ⏳ D5 (언론분석) — 기존 모듈 재사용 전제
- ⏳ ML 보정 모델 — 정답 1,000건 누적 후
