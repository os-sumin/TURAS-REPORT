import json
import logging

from app.schemas.reports import GenerateReportRequest
from app.services.reports.tech_fee_prediction import predict_and_write_section

logger = logging.getLogger(__name__)


def build_heuristic_sections(request: GenerateReportRequest, sections: list[str] | None = None) -> dict[str, str]:
    selected_sections = sections or request.report_context.sections or ["SUMMARY"]
    results: dict[str, str] = {}
    org_name = request.organization.organization_name or "Unknown organization"
    project_name = request.project.project_name or "Unknown project"
    contract_name = request.contract.contract_name or "Unknown contract"
    report_year = request.report_context.report_year or "-"

    for section in selected_sections:
        if section == "SUMMARY":
            results[section] = (
                f"{report_year}년 기준 {org_name}의 {project_name} 과제와 "
                f"{contract_name} 계약 정보를 바탕으로 작성한 사업화 검토 초안입니다."
            )
        elif section == "MARKET":
            results[section] = _stringify_block(
                request.analysis.get("market") or request.analysis.get("marketAnalysis"),
                default_text="Spring payload에 시장 분석 데이터가 포함되지 않았습니다.",
            )
        elif section == "COMPETITORS":
            results[section] = _stringify_block(
                request.analysis.get("competitors"),
                default_text="Spring payload에 경쟁사 분석 데이터가 포함되지 않았습니다.",
            )
        elif section == "TECH_FEE":
            results[section] = _build_tech_fee_section(request)
        elif section == "RECOMMENDATIONS":
            results[section] = (
                "재무 입력값, 시장 근거, 사업화 마일스톤을 다시 검토한 뒤 최종 보고서에 반영하는 것을 권장합니다."
            )
        else:
            results[section] = _stringify_block(
                request.analysis.get(section.lower()),
                default_text=f"{section} 섹션 전용 payload가 아직 제공되지 않았습니다.",
            )

    return results


def _build_tech_fee_section(request: GenerateReportRequest) -> str:
    """5차원 스코어카드 예측 결과를 본문으로 반환. 실패 시 stringify fallback."""
    try:
        body = predict_and_write_section(request)
        if body:
            return body
    except Exception:
        logger.exception("Tech fee prediction failed; falling back to stringify.")

    return _stringify_block(
        request.analysis.get("techFee") or request.analysis.get("tech_fee"),
        default_text="Spring payload에 기술료 분석 데이터가 포함되지 않았습니다.",
    )


def _stringify_block(value, default_text: str) -> str:
    if value is None or value == {} or value == []:
        return default_text
    if isinstance(value, str):
        return value
    return json.dumps(value, ensure_ascii=True, default=str)
