"""PredictionResult → DOCX 섹션 본문 텍스트.

heuristic_sections 가 dict[str, str] 만 다루므로 본문은 plain text(개행 포함).
docx_renderer._split_paragraphs 가 \\n 기준으로 단락 분리 → DOCX 자동 다단락.
"""
from __future__ import annotations

from app.schemas.reports import GenerateReportRequest

from .engine import run as run_engine
from .input_adapter import build_context
from .models import DimensionScore, PredictionResult

# 사업기획서 VII-3: 내부 분석값 → 외부 보고용 표현
_EXTERNAL_PHRASES = [
    (0.8, "강한 긍정 신호"),
    (0.6, "긍정 신호"),
    (0.4, "중립"),
    (0.2, "약한 부정 신호"),
    (0.0, "정보 부족 또는 부정"),
]


def predict_and_write_section(request: GenerateReportRequest) -> str | None:
    """예측 실행 + 섹션 본문 생성. 입력 부족 시 None."""
    ctx = build_context(request)
    if ctx is None:
        return None
    result = run_engine(ctx)
    return write_section(result)


def write_section(result: PredictionResult) -> str:
    """예측 결과를 한국어 섹션 본문(plain text)으로 작성."""
    company = result.context.company_name if result.context else ""
    project = result.context.project_name if result.context else ""
    header_org = company or "본 기업"
    header_prj = project or "본 과제"

    lines: list[str] = []
    lines.append(
        f"{header_org}의 {header_prj}에 대한 기술료 납부 가능성을 룰 기반 스코어카드({result.rule_version})로 평가한 결과, "
        f"총점 {result.total_score:.1f}/100점으로 '{result.grade.label}' 등급에 해당합니다. "
        f"추천 조치: {result.grade.action}."
    )

    lines.append("")
    lines.append("[5대 차원별 점수]")
    for d in result.dimensions:
        phrase = _external_phrase(d)
        lines.append(f"- {d.code}. {d.label}: {d.score:.1f} / {d.max_score:.0f}점 ({phrase})")

    contributors = _top_contributors(result.dimensions, n=3)
    if contributors:
        lines.append("")
        lines.append("[주요 긍정 요인]")
        for label, score in contributors:
            lines.append(f"- {label} (+{score:.1f}점)")

    detractors = _bottom_contributors(result.dimensions, n=3)
    if detractors:
        lines.append("")
        lines.append("[확인 필요 항목]")
        for label in detractors:
            lines.append(f"- {label}")

    lines.append("")
    lines.append(
        "본 점수는 룰 기반 산식 결과이며, 실태조사 우선순위 판단을 위한 보조 지표입니다. "
        "외부 보고 시에는 원시 점수 대신 등급과 근거 위주로 활용하시기 바랍니다."
    )
    return "\n".join(lines)


def _external_phrase(d: DimensionScore) -> str:
    if d.max_score == 0:
        return "정보 부족"
    pct = d.score / d.max_score
    for threshold, phrase in _EXTERNAL_PHRASES:
        if pct >= threshold:
            return phrase
    return _EXTERNAL_PHRASES[-1][1]


def _top_contributors(dims: list[DimensionScore], n: int) -> list[tuple[str, float]]:
    items: list[tuple[str, float]] = []
    for d in dims:
        for r in d.detail:
            if r.score > 0:
                items.append((f"{d.label} · {r.rule_name}", r.score))
    items.sort(key=lambda x: x[1], reverse=True)
    return items[:n]


def _bottom_contributors(dims: list[DimensionScore], n: int) -> list[str]:
    out: list[str] = []
    for d in dims:
        if d.score / d.max_score < 0.3 and d.max_score > 0:
            out.append(f"{d.label} 점수 낮음 — 입력 데이터 보강 검토")
    return out[:n]
