"""예측 결과 데이터 모델.

Java DTO(`PredictionResultDto`, `DimensionScoreDto`)와 1:1 대응.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Optional


@dataclass
class TfeeHistory:
    """D3 핵심 입력 — 엑셀 양식에서 직접 산출."""
    has_past_tfee: bool = False
    cumulative_recovery_rate: Optional[float] = None
    recovery_rate_percentile: Optional[float] = None  # 0.0 ~ 1.0
    project_recovery_rate: Optional[float] = None
    months_to_first_tfee: Optional[int] = None
    consecutive_years: int = 0


@dataclass
class OrgnFinancials:
    sales_growth_rate: Optional[float] = None
    operating_margin: Optional[float] = None
    debt_ratio: Optional[float] = None
    cash_to_gov_fund: Optional[float] = None
    rnd_intensity: Optional[float] = None
    asset_growth_rate: Optional[float] = None


@dataclass
class SubjectInfo:
    sbjt_id: str = ""
    sbjt_name: str = ""
    ksic: str = ""
    end_de: Optional[datetime] = None
    total_gov_fund: int = 0


@dataclass
class PredictionContext:
    """차원 계산기들이 입력으로 받는 컨테이너."""
    sbjt_id: str
    orgn_id: str
    pred_at: datetime
    subject: Optional[SubjectInfo] = None
    financials: Optional[OrgnFinancials] = None
    tfee_history: Optional[TfeeHistory] = None
    external_metrics: dict[str, float] = field(default_factory=dict)
    news_event_counts: dict[str, int] = field(default_factory=dict)

    # 외부 보고서 표시에 쓰는 부가 정보
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
    context: Optional[PredictionContext] = None
    factors_positive: list[str] = field(default_factory=list)
    factors_negative: list[str] = field(default_factory=list)
    factors_check: list[str] = field(default_factory=list)
