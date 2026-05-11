from datetime import datetime

from tfee_report.models import PredictionContext, TfeeHistory
from tfee_report.scorecard import engine, rules


def test_engine_runs_five_dimensions():
    ctx = PredictionContext(
        sbjt_id="S001", orgn_id="O001", pred_at=datetime.now(),
        tfee_history=TfeeHistory(has_past_tfee=True, recovery_rate_percentile=0.5,
                                  project_recovery_rate=0.15, months_to_first_tfee=18,
                                  consecutive_years=2),
    )
    r = engine.run(ctx)
    assert len(r.dimensions) == 5
    assert r.grade.code in {"VERY_HIGH", "HIGH", "MID", "LOW", "VERY_LOW"}
    assert 0.0 <= r.total_score <= 100.0


def test_grade_order():
    rs = rules.load()
    codes = [g.code for g in rs.grades]
    assert codes == ["VERY_HIGH", "HIGH", "MID", "LOW", "VERY_LOW"]


def test_grade_picks_highest_min_below_score():
    rs = rules.load()
    ctx = PredictionContext(sbjt_id="S", orgn_id="O", pred_at=datetime.now())
    r = engine.run(ctx, rs)
    # 빈 컨텍스트 → 0점 → VERY_LOW
    assert r.grade.code == "VERY_LOW"
