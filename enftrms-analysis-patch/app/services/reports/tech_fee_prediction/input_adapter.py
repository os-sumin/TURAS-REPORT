"""GenerateReportRequest → PredictionContext 변환 어댑터.

Spring 측이 보내는 `analysis.techFee` payload 를 본 모듈이 해석.
허용 형태:

  1) 단일 객체 — 한 과제 한 기관:
     analysis.techFee = {
       "tfeeHistory": { "hasPastTfee": true, "projectRecoveryRate": 0.15, ... },
       "financials":  { "salesGrowthRate": 0.12, ... },        # 선택
       "externalMetrics": { "g2bBidCount": 5, ... },           # 선택
       "newsEventCounts": { "PRODUCT_LAUNCH": 1, ... },        # 선택
       "predAt": "2026-05-11"                                  # 선택
     }

  2) 엑셀 양식 한 행을 그대로 펼친 형태 — Spring 측에서 DB 한 줄을 그대로 보낼 때:
     analysis.techFee = {
       "row": {
         "ttlPayGvstmAm": 100000000, "ttlUseGvstmAm": 99000000,
         "salesOccurYn": "Y", "rndIncomeAm": 10000000,
         "tfeeAm": 200000, "techCtrbPt": 0.5,
         "techIpmtCntrDe": "2023-06-01",   # 실시계약일 (선택)
         "baseYear": 2026
       }
     }

위 둘 중 하나가 주어지지 않으면 financial_inputs / contract / project 메타데이터로 최대한 fallback.
"""
from __future__ import annotations

from datetime import datetime
from typing import Any

from app.schemas.reports import GenerateReportRequest

from .models import OrgnFinancials, PredictionContext, SubjectInfo, TfeeHistory


def build_context(request: GenerateReportRequest) -> PredictionContext | None:
    """입력이 부족해 예측이 의미 없으면 None 반환."""
    block = _extract_block(request.analysis)
    if block is None and not _has_any_signal(request):
        return None
    block = block or {}

    pred_at = _parse_dt(block.get("predAt")) or datetime.utcnow()
    tfee_history = _build_tfee_history(block, request)
    financials = _build_financials(block, request)
    subject = _build_subject(block, request)

    return PredictionContext(
        sbjt_id=str(request.project.project_code or request.project.project_id or ""),
        orgn_id=str(request.organization.organization_id or ""),
        pred_at=pred_at,
        subject=subject,
        financials=financials,
        tfee_history=tfee_history,
        external_metrics=_as_float_map(block.get("externalMetrics")),
        news_event_counts=_as_int_map(block.get("newsEventCounts")),
        company_name=request.organization.organization_name or "",
        project_name=request.project.project_name or "",
    )


# ---------- private ----------

def _extract_block(analysis: dict[str, Any]) -> dict[str, Any] | None:
    if not analysis:
        return None
    for key in ("techFee", "tech_fee", "techFeePrediction", "techfeePrediction"):
        v = analysis.get(key)
        if isinstance(v, dict):
            return v
    return None


def _has_any_signal(request: GenerateReportRequest) -> bool:
    return bool(request.financial_inputs or request.analysis)


def _build_tfee_history(block: dict[str, Any], request: GenerateReportRequest) -> TfeeHistory:
    raw = block.get("tfeeHistory") if isinstance(block.get("tfeeHistory"), dict) else None
    row = block.get("row") if isinstance(block.get("row"), dict) else None

    if raw:
        return TfeeHistory(
            has_past_tfee=bool(raw.get("hasPastTfee", False)),
            cumulative_recovery_rate=_f(raw.get("cumulativeRecoveryRate")),
            recovery_rate_percentile=_f(raw.get("recoveryRatePercentile")),
            project_recovery_rate=_f(raw.get("projectRecoveryRate")),
            months_to_first_tfee=_i(raw.get("monthsToFirstTfee")),
            consecutive_years=int(raw.get("consecutiveYears") or 0),
        )

    if row:
        sales_yn = str(row.get("salesOccurYn") or "N").upper()
        tfee_am = _f(row.get("tfeeAm")) or 0.0
        use_gov = _f(row.get("ttlUseGvstmAm")) or 0.0
        pay_gov = _f(row.get("ttlPayGvstmAm")) or 0.0
        has_tfee = sales_yn == "Y" and tfee_am > 0
        return TfeeHistory(
            has_past_tfee=has_tfee,
            cumulative_recovery_rate=(tfee_am / pay_gov) if pay_gov > 0 else None,
            project_recovery_rate=(tfee_am / use_gov) if use_gov > 0 else 0.0,
            recovery_rate_percentile=None,
            months_to_first_tfee=None,
            consecutive_years=1 if has_tfee else 0,
        )

    return TfeeHistory()


def _build_financials(block: dict[str, Any], request: GenerateReportRequest) -> OrgnFinancials:
    f = block.get("financials") if isinstance(block.get("financials"), dict) else {}
    out = OrgnFinancials(
        sales_growth_rate=_f(f.get("salesGrowthRate")),
        operating_margin=_f(f.get("operatingMargin")),
        debt_ratio=_f(f.get("debtRatio")),
        cash_to_gov_fund=_f(f.get("cashToGovFund")),
        rnd_intensity=_f(f.get("rndIntensity")),
        asset_growth_rate=_f(f.get("assetGrowthRate")),
    )
    if out.sales_growth_rate is None and out.operating_margin is None:
        # request.financial_inputs 로 fallback — 직전 2년 비교
        gr, om = _derive_from_financial_inputs(request.financial_inputs)
        if out.sales_growth_rate is None:
            out.sales_growth_rate = gr
        if out.operating_margin is None:
            out.operating_margin = om
    return out


def _derive_from_financial_inputs(rows: list[dict[str, Any]]) -> tuple[float | None, float | None]:
    if not rows:
        return None, None
    by_year = {int(r["year"]): r for r in rows if "year" in r}
    if not by_year:
        return None, None
    years = sorted(by_year)
    last = by_year[years[-1]]
    prev = by_year[years[-2]] if len(years) > 1 else None
    revenue = _f(last.get("revenue")) or 0.0
    op_income = _f(last.get("operatingIncome"))
    operating_margin = (op_income / revenue) if revenue and op_income is not None else None
    sales_growth = None
    if prev:
        prev_rev = _f(prev.get("revenue")) or 0.0
        if prev_rev > 0:
            sales_growth = (revenue - prev_rev) / prev_rev
    return sales_growth, operating_margin


def _build_subject(block: dict[str, Any], request: GenerateReportRequest) -> SubjectInfo:
    return SubjectInfo(
        sbjt_id=str(request.project.project_code or ""),
        sbjt_name=request.project.project_name or "",
        ksic="",
        end_de=_parse_dt(block.get("subjectEndDate") or block.get("endDate")),
        total_gov_fund=int(_f(block.get("totalGovFund")) or 0),
    )


def _as_float_map(v: Any) -> dict[str, float]:
    if not isinstance(v, dict):
        return {}
    out: dict[str, float] = {}
    for k, raw in v.items():
        f = _f(raw)
        if f is not None:
            out[str(k)] = f
    return out


def _as_int_map(v: Any) -> dict[str, int]:
    if not isinstance(v, dict):
        return {}
    out: dict[str, int] = {}
    for k, raw in v.items():
        try:
            out[str(k)] = int(raw)
        except (TypeError, ValueError):
            continue
    return out


def _f(v: Any) -> float | None:
    if v is None:
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def _i(v: Any) -> int | None:
    f = _f(v)
    return int(f) if f is not None else None


def _parse_dt(v: Any) -> datetime | None:
    if v is None or v == "":
        return None
    if isinstance(v, datetime):
        return v
    for fmt in ("%Y-%m-%dT%H:%M:%S", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d", "%Y/%m/%d"):
        try:
            return datetime.strptime(str(v), fmt)
        except ValueError:
            continue
    try:
        return datetime.fromisoformat(str(v))
    except ValueError:
        return None
