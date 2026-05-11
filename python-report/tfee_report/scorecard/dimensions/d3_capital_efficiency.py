"""D3. R&D 자본효율·기술료 이력 — 엑셀 양식 직접 매핑 (풀구현)."""
from __future__ import annotations

from ...models import DimensionScore, PredictionContext, RuleDetail, TfeeHistory
from ..evaluator import evaluate
from ..rules import Dimension


def _input(key: str, h: TfeeHistory | None):
    if h is None:
        return None
    table = {
        "hasPastTfee": h.has_past_tfee,
        "recoveryRatePercentile": h.recovery_rate_percentile,
        "projectRecoveryRate": h.project_recovery_rate,
        "monthsToFirstTfee": h.months_to_first_tfee,
        "consecutiveYears": h.consecutive_years,
    }
    return table.get(key)


def calculate(ctx: PredictionContext, dim: Dimension) -> DimensionScore:
    h = ctx.tfee_history
    detail: list[RuleDetail] = []
    total = 0.0

    for rule in dim.rules:
        val = _input(rule.input, h)
        score = evaluate(rule, val)
        total += score
        detail.append(RuleDetail(
            rule_name=rule.name,
            score=round(score, 2),
            max=rule.max,
            input_value=val,
        ))

    total = min(total, dim.weight)
    return DimensionScore(
        code="D3",
        label=dim.label,
        score=round(total, 2),
        max_score=dim.weight,
        detail=detail,
    )
