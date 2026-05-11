# tfee-report — 기술료 납부 가능성 예측 최종보고서 생성기

엑셀 양식(`PS_ORGN_TFEE_CCLT`) → 5차원 스코어카드 → **PDF 최종보고서**.

**중요**: 스코어카드 룰은 Java 패치(`enftrms-patch/src/main/resources/rules/scorecard.yml`)와 **같은 YAML 파일을 공유**합니다. 룰을 수정하면 양쪽 모두 반영됩니다.

## 빠른 사용

```bash
pip install -e .
tfee-report --excel samples/sample_input.xlsx --out report.pdf
# 또는
python -m tfee_report.cli --excel samples/sample_input.xlsx --out report.pdf
```

## 디렉토리

```
python-report/
├── tfee_report/
│   ├── models.py                  # PredictionResult / DimensionScore / Grade dataclass
│   ├── scorecard/
│   │   ├── rules.py               # scorecard.yml 로딩
│   │   ├── evaluator.py           # threshold/bucket/linear/binary/percentile/log_scaled
│   │   ├── engine.py              # ScoreCardEngine
│   │   └── dimensions/            # D1~D5 (D3 풀구현)
│   ├── data/
│   │   ├── excel_loader.py        # PS_ORGN_TFEE_CCLT 양식 → DataFrame
│   │   └── db_loader.py           # 옵션 (oracledb, 추후)
│   ├── report/
│   │   ├── pdf.py                 # WeasyPrint HTML→PDF
│   │   └── templates/             # Jinja2 + CSS
│   └── cli.py
└── tests/
```

## 데이터 입력

기본은 사용자가 제공한 엑셀 양식. 컬럼 인덱스 매핑은 `data/excel_loader.py` 상단 상수.

| 양식 컬럼 | 모델에서의 의미 |
|---|---|
| 과제ID | 예측 단위 |
| 성과소유기관 사업자번호 | 기관 키 |
| 지급/실사용 정부지원금 | D3 분모 |
| 매출발생여부 + 기술료 금액 | **정답 라벨 Y** |
| 기술기여도 | 회수율 보정 |

DB 직결이 필요하면 `pip install tfee-report[db]` 후 `data/db_loader.py` 활성화.

## 출력

`weasyprint` 가 한글 PDF 생성. 시스템에 `pango`, `cairo`, 한글 글꼴(나눔고딕 등) 설치 필요:

```bash
# Ubuntu
sudo apt-get install -y libpango-1.0-0 libpangoft2-1.0-0 fonts-nanum
```
