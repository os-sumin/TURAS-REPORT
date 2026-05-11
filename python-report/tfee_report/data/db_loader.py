"""Oracle 직결 옵션 — `pip install tfee-report[db]` 후 사용.

본 패치 단계에서는 인터페이스만 정의. 실 EnFTRMS 합본 시 본 모듈 채울 것.
"""
from __future__ import annotations

from datetime import datetime
from typing import Iterable

from ..models import PredictionContext


def fetch_contexts(sbjt_orgn_pairs: Iterable[tuple[str, str]],
                   pred_at: datetime | None = None) -> list[PredictionContext]:
    raise NotImplementedError(
        "DB 직결은 미구현. 본 패치는 엑셀 입력을 사용하세요. "
        "EnFTRMS 합본 단계에서 oracledb 로 연결합니다."
    )
