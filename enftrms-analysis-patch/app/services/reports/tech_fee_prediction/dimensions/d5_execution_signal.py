"""D5. 기업 실행 신호 — news_event_counts(이벤트→건수) 합산.

언론분석 서비스(app/services/news/) 결과를 그대로 이벤트 타입별 카운트로 매핑하여 입력.
"""
from __future__ import annotations

from ..models import DimensionScore, PredictionContext, RuleDetail
from ..rules import Dimension


def calculate(ctx: PredictionContext, dim: Dimension) -> DimensionScore:
    detail: list[RuleDetail] = []
    total = 0.0
    for event_type, count in (ctx.news_event_counts or {}).items():
        per = dim.event_scores.get(event_type)
        if per is None or not count:
            continue
        contrib = per * count
        total += contrib
        detail.append(RuleDetail(rule_name=event_type, score=round(contrib, 2),
                                 max=dim.weight, input_value=count))
    return DimensionScore(code="D5", label=dim.label,
                          score=round(max(0.0, min(dim.weight, total)), 2),
                          max_score=dim.weight, detail=detail)
