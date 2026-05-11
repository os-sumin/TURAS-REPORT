"""스코어카드 엔진 — 5차원 계산기 결과를 합산해 총점/등급 산출."""
from __future__ import annotations

from datetime import datetime, timezone, timedelta

from ..models import DimensionScore, Grade, PredictionContext, PredictionResult
from . import dimensions, rules

KST = timezone(timedelta(hours=9))


def _pick_grade(score: float, grades: list[rules.GradeRule]) -> rules.GradeRule:
    eligible = [g for g in grades if score >= g.min]
    return max(eligible, key=lambda g: g.min) if eligible else grades[-1]


def run(ctx: PredictionContext, rule_set: rules.RuleSet | None = None) -> PredictionResult:
    rs = rule_set or rules.load()

    dims: list[DimensionScore] = []
    for code, key, calc in dimensions.ALL:
        dim = rs.dimensions.get(key)
        if not dim:
            continue
        dims.append(calc(ctx, dim))

    total = sum(d.score for d in dims)
    total = round(max(0.0, min(100.0, total)), 2)
    g = _pick_grade(total, rs.grades)

    return PredictionResult(
        sbjt_id=ctx.sbjt_id,
        orgn_id=ctx.orgn_id,
        pred_at=ctx.pred_at or datetime.now(KST),
        total_score=total,
        grade=Grade(code=g.code, label=g.label, action=g.action),
        rule_version=rs.rule_version,
        dimensions=dims,
        context=ctx,
    )
