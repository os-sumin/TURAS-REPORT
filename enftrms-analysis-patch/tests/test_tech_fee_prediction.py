from datetime import datetime

import pytest

from app.schemas.reports import (
    ContractContext,
    GenerateReportRequest,
    ReportContext,
)
from app.services.reports.heuristic_sections import build_heuristic_sections
from app.services.reports.tech_fee_prediction import (
    PredictionContext,
    TfeeHistory,
    run,
)
from app.services.reports.tech_fee_prediction.dimensions import d3_capital_efficiency
from app.services.reports.tech_fee_prediction.evaluator import evaluate
from app.services.reports.tech_fee_prediction.input_adapter import build_context
from app.services.reports.tech_fee_prediction.rules import (
    Bucket,
    Rule,
    Threshold,
    load_cached,
)
from app.services.reports.tech_fee_prediction.section_writer import (
    predict_and_write_section,
)


# ----- evaluator -----

def test_evaluator_threshold():
    rule = Rule(name="t", max=5, type="threshold", thresholds=[
        Threshold(gte=0.2, score=6),
        Threshold(gte=0.0, score=1),
    ])
    assert evaluate(rule, 0.25) == 5      # capped at max
    assert evaluate(rule, 0.0) == 1


def test_evaluator_bucket_inclusive_lo_exclusive_hi():
    rule = Rule(name="b", max=5, type="bucket", buckets=[
        Bucket(range=[12, 24], score=5),
    ])
    assert evaluate(rule, 12) == 5
    assert evaluate(rule, 24) == 0


def test_evaluator_binary_str_truthy():
    rule = Rule(name="bn", max=6, type="binary", score_when_true=6)
    assert evaluate(rule, "Y") == 6
    assert evaluate(rule, "n") == 0


# ----- D3 (full impl) -----

def test_d3_high_score():
    dim = load_cached().dimensions["D3_capital_efficiency"]
    ctx = PredictionContext(
        sbjt_id="S", orgn_id="O", pred_at=datetime.now(),
        tfee_history=TfeeHistory(
            has_past_tfee=True, recovery_rate_percentile=0.95,
            project_recovery_rate=0.25, months_to_first_tfee=8, consecutive_years=3,
        ),
    )
    r = d3_capital_efficiency.calculate(ctx, dim)
    assert 19.0 <= r.score <= 20.0


def test_d3_low_score():
    dim = load_cached().dimensions["D3_capital_efficiency"]
    ctx = PredictionContext(
        sbjt_id="S", orgn_id="O", pred_at=datetime.now(),
        tfee_history=TfeeHistory(),
    )
    r = d3_capital_efficiency.calculate(ctx, dim)
    assert 0.0 <= r.score <= 1.5     # project_recovery_rate=0 → gte 0 룰 매칭으로 1점


# ----- engine -----

def test_engine_produces_five_dimensions():
    ctx = PredictionContext(
        sbjt_id="S001", orgn_id="O001", pred_at=datetime.now(),
        tfee_history=TfeeHistory(has_past_tfee=True, recovery_rate_percentile=0.5,
                                  project_recovery_rate=0.15, months_to_first_tfee=18,
                                  consecutive_years=2),
    )
    r = run(ctx)
    assert len(r.dimensions) == 5
    assert r.grade.code in {"VERY_HIGH", "HIGH", "MID", "LOW", "VERY_LOW"}
    assert 0.0 <= r.total_score <= 100.0


def test_engine_empty_context_is_very_low():
    ctx = PredictionContext(sbjt_id="S", orgn_id="O", pred_at=datetime.now())
    r = run(ctx)
    assert r.grade.code == "VERY_LOW"


# ----- input adapter -----

def _request(analysis: dict) -> GenerateReportRequest:
    return GenerateReportRequest(
        report_id="AIR-T1",
        report_context=ReportContext(sections=["TECH_FEE"]),
        contract=ContractContext(contract_name="Test Contract"),
        organization={"organizationId": 101, "organizationName": "테스트기업"},
        project={"projectId": 1001, "projectCode": "PJT-T", "projectName": "테스트과제"},
        analysis=analysis,
    )


def test_adapter_accepts_tfee_history_block():
    req = _request({"techFee": {
        "tfeeHistory": {"hasPastTfee": True, "projectRecoveryRate": 0.15,
                        "recoveryRatePercentile": 0.6, "monthsToFirstTfee": 15,
                        "consecutiveYears": 2},
    }})
    ctx = build_context(req)
    assert ctx is not None
    assert ctx.tfee_history.has_past_tfee is True
    assert ctx.tfee_history.project_recovery_rate == 0.15
    assert ctx.company_name == "테스트기업"


def test_adapter_accepts_excel_row_form():
    req = _request({"techFee": {"row": {
        "ttlPayGvstmAm": 100_000_000, "ttlUseGvstmAm": 99_000_000,
        "salesOccurYn": "Y", "tfeeAm": 200_000, "techCtrbPt": 0.5,
        "baseYear": 2026,
    }}})
    ctx = build_context(req)
    assert ctx is not None
    assert ctx.tfee_history.has_past_tfee is True
    assert ctx.tfee_history.project_recovery_rate == pytest.approx(200_000 / 99_000_000)


def test_adapter_derives_financials_from_financial_inputs():
    req = GenerateReportRequest(
        report_id="AIR-T2",
        report_context=ReportContext(sections=["TECH_FEE"]),
        contract=ContractContext(contract_name="C"),
        organization={"organizationId": 1, "organizationName": "X"},
        project={"projectId": 1, "projectCode": "P", "projectName": "Y"},
        analysis={"techFee": {}},
        financial_inputs=[
            {"year": 2024, "revenue": 1000, "operatingIncome": 100},
            {"year": 2025, "revenue": 1200, "operatingIncome": 180},
        ],
    )
    ctx = build_context(req)
    assert ctx.financials.sales_growth_rate == pytest.approx(0.2)
    assert ctx.financials.operating_margin == pytest.approx(0.15)


# ----- end-to-end through heuristic_sections -----

def test_heuristic_tech_fee_uses_prediction():
    req = _request({"techFee": {
        "tfeeHistory": {"hasPastTfee": True, "projectRecoveryRate": 0.20,
                        "recoveryRatePercentile": 0.8, "monthsToFirstTfee": 10,
                        "consecutiveYears": 2},
    }})
    result = build_heuristic_sections(req, ["TECH_FEE"])
    body = result["TECH_FEE"]
    assert "기술료 납부 가능성" in body
    assert "5대 차원별 점수" in body
    assert "D3" in body
    # 기존 fallback 문구가 절대 나오면 안됨
    assert "Spring payload에 기술료 분석 데이터" not in body


def test_heuristic_tech_fee_falls_back_when_no_payload():
    req = _request({})
    # techFee/financial_inputs 모두 없음 → input_adapter 가 None 반환 →
    # _stringify_block 의 default_text 가 노출
    result = build_heuristic_sections(req, ["TECH_FEE"])
    assert "포함되지 않았습니다" in result["TECH_FEE"]


def test_predict_and_write_section_direct():
    req = _request({"techFee": {
        "tfeeHistory": {"hasPastTfee": True, "projectRecoveryRate": 0.15,
                        "recoveryRatePercentile": 0.6, "monthsToFirstTfee": 15,
                        "consecutiveYears": 2},
        "externalMetrics": {"g2bBidCount": 5},
        "newsEventCounts": {"PRODUCT_LAUNCH": 1, "CERTIFICATION": 1},
    }})
    body = predict_and_write_section(req)
    assert body is not None
    assert "총점" in body
    assert "/100점" in body
