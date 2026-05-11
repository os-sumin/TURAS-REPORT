"""PS_ORGN_TFEE_CCLT 엑셀 양식 → PredictionContext 리스트.

양식 컬럼 인덱스(0-based)는 본 파일 상단 상수에서 관리.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime
from pathlib import Path
from typing import Iterator

from openpyxl import load_workbook
from openpyxl.cell.cell import Cell

from ..models import PredictionContext, SubjectInfo, TfeeHistory

# 양식 분석 결과에 기반한 컬럼 매핑 (수정 시 본 상수만 변경)
HEADER_ROW_INDEX = 6    # 1-based  (양식 6행)
DATA_START_ROW   = 7    # 1-based  (양식 7행)

COL = {
    "전문기관명":         2,
    "연구개발기간_시작":   3,
    "연구개발기간_종료":   4,
    "과제명":             8,
    "주관연구개발기관명":  9,
    "성과소유기관명":     11,
    "성과소유사업자번호": 12,
    "과제ID":             15,
    "지급정부지원금":      20,
    "실사용정부지원금":    25,
    "기술실시기관사업자":  27,
    "기준보고년도":        39,
    "매출발생여부":        42,
    "RD수익금액":          43,
    "기술기여도":          44,
    "기술료금액":          47,
}


@dataclass
class ExcelRow:
    sbjt_id: str
    sbjt_name: str
    company_name: str
    orgn_biz_no: str
    end_de: date | None
    total_gov_fund: int
    use_gov_fund: int
    base_year: int | None
    sales_occur_yn: str
    rnd_income_am: int
    tech_ctrb_pt: float
    tfee_am: int


def _cell(row, idx: int):
    """openpyxl 은 1-based. 본 상수도 1-based 로 통일."""
    try:
        return row[idx - 1]
    except IndexError:
        return None


def _str(c: Cell | None) -> str:
    if c is None or c.value is None:
        return ""
    return str(c.value).strip()


def _int(c: Cell | None) -> int:
    if c is None or c.value in (None, ""):
        return 0
    try:
        return int(float(c.value))
    except (TypeError, ValueError):
        return 0


def _float(c: Cell | None) -> float:
    if c is None or c.value in (None, ""):
        return 0.0
    try:
        return float(c.value)
    except (TypeError, ValueError):
        return 0.0


def _date(c: Cell | None) -> date | None:
    if c is None or c.value in (None, ""):
        return None
    v = c.value
    if isinstance(v, datetime):
        return v.date()
    if isinstance(v, date):
        return v
    for fmt in ("%Y-%m-%d", "%Y/%m/%d", "%Y.%m.%d"):
        try:
            return datetime.strptime(str(v), fmt).date()
        except ValueError:
            continue
    return None


def iter_rows(path: str | Path) -> Iterator[ExcelRow]:
    wb = load_workbook(path, data_only=True)
    ws = wb.active
    for row in ws.iter_rows(min_row=DATA_START_ROW):
        sbjt_id = _str(_cell(row, COL["과제ID"]))
        orgn = _str(_cell(row, COL["성과소유사업자번호"]))
        if not sbjt_id and not orgn:
            continue
        yield ExcelRow(
            sbjt_id=sbjt_id or f"UNKNOWN-{row[0].row}",
            sbjt_name=_str(_cell(row, COL["과제명"])),
            company_name=_str(_cell(row, COL["성과소유기관명"])) or _str(_cell(row, COL["주관연구개발기관명"])),
            orgn_biz_no=orgn,
            end_de=_date(_cell(row, COL["연구개발기간_종료"])),
            total_gov_fund=_int(_cell(row, COL["지급정부지원금"])),
            use_gov_fund=_int(_cell(row, COL["실사용정부지원금"])),
            base_year=_int(_cell(row, COL["기준보고년도"])) or None,
            sales_occur_yn=_str(_cell(row, COL["매출발생여부"])).upper() or "N",
            rnd_income_am=_int(_cell(row, COL["RD수익금액"])),
            tech_ctrb_pt=_float(_cell(row, COL["기술기여도"])),
            tfee_am=_int(_cell(row, COL["기술료금액"])),
        )


def to_context(row: ExcelRow, pred_at: datetime) -> PredictionContext:
    """엑셀 1행 → 예측 컨텍스트. D3 입력은 본 행에서 직접 도출."""
    has_tfee = row.sales_occur_yn == "Y" and row.tfee_am > 0
    project_recovery = (row.tfee_am / row.use_gov_fund) if row.use_gov_fund > 0 else 0.0

    tfee_history = TfeeHistory(
        has_past_tfee=has_tfee,
        project_recovery_rate=project_recovery,
        cumulative_recovery_rate=project_recovery,    # 단일 행 가정. 다행 집계는 호출자에서 보강.
        recovery_rate_percentile=None,                # 동종군 분포 데이터 필요
        months_to_first_tfee=None,                    # 실시계약일 정보 추가 시 보강
        consecutive_years=1 if has_tfee else 0,
    )

    subject = SubjectInfo(
        sbjt_id=row.sbjt_id,
        sbjt_name=row.sbjt_name,
        ksic="",
        end_de=datetime.combine(row.end_de, datetime.min.time()) if row.end_de else None,
        total_gov_fund=row.total_gov_fund,
    )

    return PredictionContext(
        sbjt_id=row.sbjt_id,
        orgn_id=row.orgn_biz_no,
        pred_at=pred_at,
        subject=subject,
        tfee_history=tfee_history,
        company_name=row.company_name,
        project_name=row.sbjt_name,
    )


def load_contexts(path: str | Path, pred_at: datetime | None = None) -> list[PredictionContext]:
    ref = pred_at or datetime.now()
    return [to_context(r, ref) for r in iter_rows(path)]
