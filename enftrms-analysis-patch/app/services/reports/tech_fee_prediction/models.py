"""예측 입력/결과 데이터 모델 (dataclass).

본 모듈은 외부에 노출되는 pydantic 스키마(`app.schemas.reports`)와는 분리.
요청은 GenerateReportRequest 로 들어와 input_adapter 가 본 모델로 변환.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any


@dataclass
class TfeeHistory:
    """D3 핵심 입력 — 엑셀 양식 PS_ORGN_TFEE_CCLT 에서 산출."""
    has_past_tfee: bool = False
    cumulative_recovery_rate: float | None = None
    recovery_rate_percentile: float | None = None    # 0.0 ~ 1.0
    project_recovery_rate: float | None = None
    months_to_first_tfee: int | None = None
    consecutive_years: int = 0


@dataclass
class OrgnFinancials:
    sales_growth_rate: float | None = None
    operating_margin: float | None = None
    debt_ratio: float | None = None
    cash_to_gov_fund: float | None = None
    rnd_intensity: float | None = None
    asset_growth_rate: float | None = None


@dataclass
class SubjectInfo:
    sbjt_id: str = ""
    sbjt_name: str = ""
    ksic: str = ""
    end_de: datetime | None = None
    total_gov_fund: int = 0


@dataclass
class PredictionContext:
    sbjt_id: str
    orgn_id: str
    pred_at: datetime
    subject: SubjectInfo | None = None
    financials: OrgnFinancials | None = None
    tfee_history: TfeeHistory | None = None
    external_metrics: dict[str, float] = field(default_factory=dict)
    news_event_counts: dict[str, int] = field(default_factory=dict)
    company_name: str = ""
    project_name: str = ""

    def metric(self, key: str, default: float = 0.0) -> float:
        return float(self.external_metrics.get(key, default))


@dataclass
class RuleDetail:
    rule_name: str
    score: float
    max: float
    input_value: Any = None
    note: str = ""


@dataclass
class DimensionScore:
    code: str
    label: str
    score: float
    max_score: float
    detail: list[RuleDetail] = field(default_factory=list)


@dataclass
class Grade:
    code: str
    label: str
    action: str


@dataclass
class PredictionResult:
    sbjt_id: str
    orgn_id: str
    pred_at: datetime
    total_score: float
    grade: Grade
    rule_version: str
    dimensions: list[DimensionScore]
    context: PredictionContext | None = None
    factors_positive: list[str] = field(default_factory=list)
    factors_negative: list[str] = field(default_factory=list)
    factors_check: list[str] = field(default_factory=list)
