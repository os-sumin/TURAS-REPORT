"""스코어카드 엔진 — 5차원 합산 → 등급."""
from __future__ import annotations

from datetime import datetime, timedelta, timezone

from .dimensions import ALL
from .models import DimensionScore, Grade, PredictionContext, PredictionResult
from .rules import GradeRule, RuleSet, load_cached

KST = timezone(timedelta(hours=9))


def _pick_grade(score: float, grades: list[GradeRule]) -> GradeRule:
    eligible = [g for g in grades if score >= g.min]
    return max(eligible, key=lambda g: g.min) if eligible else grades[-1]


def run(ctx: PredictionContext, rule_set: RuleSet | None = None) -> PredictionResult:
    rs = rule_set or load_cached()

    dims: list[DimensionScore] = []
    for code, key, calc in ALL:  # noqa: B007 — code 변수는 의도적 미사용
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
