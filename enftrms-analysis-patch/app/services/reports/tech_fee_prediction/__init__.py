"""기술료 납부 가능성 예측 — TECH_FEE 섹션 산출 모듈.

호출 진입점:
    from app.services.reports.tech_fee_prediction import predict_and_write_section
    section_body = predict_and_write_section(request)   # GenerateReportRequest 입력

스코어카드 룰: app/services/reports/tech_fee_prediction/scorecard.yml
"""
from .engine import run
from .input_adapter import build_context
from .models import (
    DimensionScore,
    Grade,
    OrgnFinancials,
    PredictionContext,
    PredictionResult,
    RuleDetail,
    SubjectInfo,
    TfeeHistory,
)
from .section_writer import predict_and_write_section, write_section

__all__ = [
    "run",
    "build_context",
    "predict_and_write_section",
    "write_section",
    "PredictionContext",
    "PredictionResult",
    "DimensionScore",
    "Grade",
    "RuleDetail",
    "TfeeHistory",
    "OrgnFinancials",
    "SubjectInfo",
]
