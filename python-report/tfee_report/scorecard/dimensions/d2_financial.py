"""D2. 기업 재무·사업화 체력."""
from __future__ import annotations

from ...models import DimensionScore, OrgnFinancials, PredictionContext, RuleDetail
from ..evaluator import evaluate
from ..rules import Dimension


def _input(key: str, f: OrgnFinancials | None):
    if f is None:
        return None
    return {
        "salesGrowthRate": f.sales_growth_rate,
        "operatingMargin": f.operating_margin,
        "debtRatio": f.debt_ratio,
        "cashToGovFund": f.cash_to_gov_fund,
        "rndIntensity": f.rnd_intensity,
        "assetGrowthRate": f.asset_growth_rate,
    }.get(key)


def calculate(ctx: PredictionContext, dim: Dimension) -> DimensionScore:
    detail: list[RuleDetail] = []
    total = 0.0
    for rule in dim.rules:
        val = _input(rule.input, ctx.financials)
        score = evaluate(rule, val)
        total += score
        detail.append(RuleDetail(rule_name=rule.name, score=round(score, 2),
                                 max=rule.max, input_value=val))
    return DimensionScore(code="D2", label=dim.label,
                          score=round(min(total, dim.weight), 2),
                          max_score=dim.weight, detail=detail)
