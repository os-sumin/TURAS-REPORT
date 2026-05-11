from datetime import datetime

import pytest

from tfee_report.models import PredictionContext, TfeeHistory
from tfee_report.scorecard import rules
from tfee_report.scorecard.dimensions import d3_capital_efficiency


@pytest.fixture(scope="module")
def dim():
    return rules.load().dimensions["D3_capital_efficiency"]


def _ctx(history: TfeeHistory) -> PredictionContext:
    return PredictionContext(sbjt_id="S", orgn_id="O", pred_at=datetime.now(),
                             tfee_history=history)


def test_d3_all_high_near_max(dim):
    h = TfeeHistory(has_past_tfee=True, recovery_rate_percentile=0.95,
                    project_recovery_rate=0.25, months_to_first_tfee=8,
                    consecutive_years=3)
    r = d3_capital_efficiency.calculate(_ctx(h), dim)
    # 6 + 4.75 + 4 + 3 + 2 = 19.75
    assert 19.0 <= r.score <= 20.0
    assert r.code == "D3"


def test_d3_all_low(dim):
    h = TfeeHistory(has_past_tfee=False, recovery_rate_percentile=0.0,
                    project_recovery_rate=0.0, months_to_first_tfee=None,
                    consecutive_years=0)
    r = d3_capital_efficiency.calculate(_ctx(h), dim)
    # project_recovery_rate=0 → gte 0 threshold 매칭 → 1점
    assert 0.0 <= r.score <= 1.5


def test_d3_mid_range(dim):
    h = TfeeHistory(has_past_tfee=True, recovery_rate_percentile=0.40,
                    project_recovery_rate=0.07, months_to_first_tfee=20,
                    consecutive_years=1)
    r = d3_capital_efficiency.calculate(_ctx(h), dim)
    # 6 + 2 + 2 + 2 + 1 = 13.0
    assert 12.0 <= r.score <= 14.0


def test_d3_null_history(dim):
    r = d3_capital_efficiency.calculate(
        PredictionContext(sbjt_id="S", orgn_id="O", pred_at=datetime.now()), dim)
    assert r.score == 0.0
