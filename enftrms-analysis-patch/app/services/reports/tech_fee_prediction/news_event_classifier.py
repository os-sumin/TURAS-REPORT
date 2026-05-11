"""뉴스 기사 → 이벤트 타입 분류기.

D5 (기업 실행 신호) 입력 산출용.
스코어카드의 eventScores 키 (PRODUCT_LAUNCH 등) 와 일치하는 카운트 맵을 생성.

두 가지 모드:
- 휴리스틱 (기본, 의존성 없음): 키워드 매칭
- LLM (선택): 기존 OpenAI 클라이언트 활용. 휴리스틱 결과를 LLM 이 보강.
"""
from __future__ import annotations

import json
import logging
import re
from typing import Iterable

logger = logging.getLogger(__name__)

# 이벤트 코드는 scorecard.yml 의 D5_execution_signal.eventScores 와 동기화 유지.
EVENT_CODES = [
    "PRODUCT_LAUNCH",
    "SUPPLY_CONTRACT",
    "MASS_PRODUCTION",
    "CERTIFICATION",
    "INVESTMENT",
    "FACILITY_EXPAND",
    "PATENT_FILED",
    "LAWSUIT",
    "INSOLVENCY",
    "WITHDRAWAL",
]

# 키워드 휴리스틱 — 한/영 혼합 + 부분일치 (lowercase 비교)
KEYWORD_MAP: dict[str, tuple[str, ...]] = {
    "PRODUCT_LAUNCH":   ("출시", "론칭", "런칭", "공개", "launch", "unveil", "release"),
    "SUPPLY_CONTRACT":  ("수주", "공급계약", "납품", "계약 체결", "공급 계약", "체결", "supply contract", "contract"),
    "MASS_PRODUCTION":  ("양산", "양산 개시", "mass production"),
    "CERTIFICATION":    ("인증", "허가", "승인", "인허가", "혁신의료기기", "fda", "ce", "certification", "approval"),
    "INVESTMENT":       ("투자유치", "시리즈", "투자 유치", "series a", "series b", "series c", "투자 라운드", "investment", "funding"),
    "FACILITY_EXPAND":  ("공장 증설", "증설", "설비투자", "설비 투자", "신축", "facility expansion"),
    "PATENT_FILED":     ("특허 출원", "특허출원", "상표 출원", "특허 등록", "특허등록", "patent filing", "patent granted"),
    "LAWSUIT":          ("소송", "피소", "고소", "고발", "분쟁", "lawsuit"),
    "INSOLVENCY":       ("회생", "파산", "부도", "폐업", "휴업", "상장폐지", "insolvency", "bankruptcy"),
    "WITHDRAWAL":       ("사업 철수", "철수", "사업철수", "구조조정", "withdrawal", "exit"),
}


def classify_articles(
    articles: Iterable[dict],
    use_llm: bool = False,
    llm_client=None,
) -> dict[str, int]:
    """기사 리스트 → 이벤트 타입별 카운트.

    각 기사는 한 번에 한 이벤트만 카운트 (가장 강한 매치). 부정 이벤트는 우선순위.
    """
    article_list = [a for a in (articles or []) if isinstance(a, dict)]
    counts = _heuristic_counts(article_list)

    if use_llm and llm_client is not None and article_list:
        try:
            llm_counts = _llm_counts(article_list, llm_client)
            # 둘을 합산하지 않고, LLM 결과가 있으면 LLM 우선 (덮어쓰기)
            if llm_counts:
                return llm_counts
        except Exception:
            logger.exception("LLM event classification failed; using heuristic only")

    return counts


def _heuristic_counts(articles: list[dict]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for article in articles:
        text = _article_text(article)
        if not text:
            continue
        event = _best_match(text)
        if event:
            counts[event] = counts.get(event, 0) + 1
    return counts


def _article_text(article: dict) -> str:
    parts = []
    for key in ("title", "summary", "publishedAt"):
        v = article.get(key)
        if v:
            parts.append(str(v))
    # snake_case fallback
    for key in ("published_at",):
        v = article.get(key)
        if v:
            parts.append(str(v))
    return " ".join(parts).lower()


# 부정 이벤트(LAWSUIT 등) 가 같은 기사에 섞여 있으면 부정을 우선시한다.
_NEGATIVE = {"LAWSUIT", "INSOLVENCY", "WITHDRAWAL"}


def _best_match(text: str) -> str | None:
    matches: list[str] = []
    for event, keywords in KEYWORD_MAP.items():
        if any(kw in text for kw in keywords):
            matches.append(event)
    if not matches:
        return None
    for neg in _NEGATIVE:
        if neg in matches:
            return neg
    # 비-부정 매치들 중 첫 번째 등록 순서를 우선
    for event in EVENT_CODES:
        if event in matches and event not in _NEGATIVE:
            return event
    return matches[0]


# ---------- LLM 분류 (선택) ----------

_LLM_SYSTEM = (
    "You classify Korean business news articles into one event code. "
    "Allowed codes: PRODUCT_LAUNCH, SUPPLY_CONTRACT, MASS_PRODUCTION, CERTIFICATION, "
    "INVESTMENT, FACILITY_EXPAND, PATENT_FILED, LAWSUIT, INSOLVENCY, WITHDRAWAL, NONE. "
    "Return JSON: an array of strings, one per article, in input order. "
    "If an article does not match any code, output NONE."
)


def _llm_counts(articles: list[dict], llm_client) -> dict[str, int]:
    payload = [
        {"title": a.get("title") or "", "summary": a.get("summary") or ""}
        for a in articles
    ]
    user_msg = (
        "Classify each article. Return only a JSON array of event codes (no prose).\n"
        f"Articles:\n{json.dumps(payload, ensure_ascii=False)}"
    )
    # llm_client 는 openai.OpenAI 인스턴스로 가정 (기존 app/clients/openai_client.py 사용)
    response = llm_client.chat.completions.create(
        model="gpt-4o-mini",
        temperature=0.0,
        messages=[
            {"role": "system", "content": _LLM_SYSTEM},
            {"role": "user", "content": user_msg},
        ],
    )
    raw = response.choices[0].message.content or ""
    codes = _parse_codes(raw, len(articles))
    counts: dict[str, int] = {}
    for code in codes:
        if code in EVENT_CODES:
            counts[code] = counts.get(code, 0) + 1
    return counts


def _parse_codes(raw: str, expected: int) -> list[str]:
    raw = raw.strip()
    match = re.search(r"\[.*\]", raw, re.DOTALL)
    if not match:
        return []
    try:
        arr = json.loads(match.group(0))
    except json.JSONDecodeError:
        return []
    return [str(item).strip().upper() for item in arr][:expected]
