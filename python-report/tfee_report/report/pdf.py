"""PDF 보고서 생성기 (WeasyPrint)."""
from __future__ import annotations

from pathlib import Path

from jinja2 import Environment, FileSystemLoader, select_autoescape

from ..models import DimensionScore, PredictionResult

TEMPLATE_DIR = Path(__file__).parent / "templates"


def _external_phrase(d: DimensionScore) -> str:
    """사업기획서 VII-3 — 내부 분석값 → 외부 표현."""
    if d.max_score == 0:
        return "정보 부족"
    pct = d.score / d.max_score
    if pct >= 0.8:
        return "강한 긍정 신호"
    if pct >= 0.6:
        return "긍정 신호"
    if pct >= 0.4:
        return "중립"
    if pct >= 0.2:
        return "약한 부정 신호"
    return "정보 부족 또는 부정"


def render_html(result: PredictionResult) -> str:
    env = Environment(
        loader=FileSystemLoader(str(TEMPLATE_DIR)),
        autoescape=select_autoescape(["html", "xml", "j2"]),
    )
    env.globals["external_phrase"] = _external_phrase
    template = env.get_template("prediction_report.html.j2")
    return template.render(result=result, context=result.context or _empty_ctx(result))


def render_pdf(result: PredictionResult, out_path: str | Path) -> Path:
    from weasyprint import HTML, CSS  # 지연 import — pytest 시 시스템 의존성 회피
    html_str = render_html(result)
    base_url = str(TEMPLATE_DIR)
    css_path = TEMPLATE_DIR / "report.css"
    out = Path(out_path)
    out.parent.mkdir(parents=True, exist_ok=True)
    HTML(string=html_str, base_url=base_url).write_pdf(
        target=str(out),
        stylesheets=[CSS(filename=str(css_path))],
    )
    return out


def _empty_ctx(result: PredictionResult):
    """템플릿이 context 필드를 참조하므로 안전 fallback 제공."""
    from types import SimpleNamespace
    return SimpleNamespace(company_name="", project_name="")
