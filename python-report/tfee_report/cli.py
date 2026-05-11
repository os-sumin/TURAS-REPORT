"""tfee-report CLI.

사용 예:
    tfee-report --excel input.xlsx --out reports/
        → 엑셀의 각 행마다 reports/{과제ID}_{사업자번호}.pdf 생성

    tfee-report --excel input.xlsx --out single.pdf --row 0
        → 0번 행만 단일 PDF
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

from .data import excel_loader
from .report import pdf
from .scorecard import engine, rules


def _sanitize(s: str) -> str:
    keep = ("-", "_", ".")
    return "".join(c if c.isalnum() or c in keep else "_" for c in s)[:80] or "row"


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(prog="tfee-report",
                                description="기술료 납부 가능성 예측 최종보고서 생성기")
    p.add_argument("--excel", required=True, help="입력 엑셀 (PS_ORGN_TFEE_CCLT 양식)")
    p.add_argument("--out", required=True, help="출력 경로 (.pdf 또는 디렉토리)")
    p.add_argument("--rules", help="scorecard.yml 경로 (생략 시 Java 패치와 공유)")
    p.add_argument("--row", type=int, help="특정 행만 처리 (0-based)")
    p.add_argument("--pred-at", help="예측 시점 ISO8601 (기본: 현재시각)")
    p.add_argument("--json", action="store_true", help="PDF 대신 JSON 출력")
    args = p.parse_args(argv)

    rule_set = rules.load(Path(args.rules)) if args.rules else rules.load()
    pred_at = datetime.fromisoformat(args.pred_at) if args.pred_at else datetime.now()
    contexts = excel_loader.load_contexts(args.excel, pred_at=pred_at)

    if not contexts:
        print("입력 엑셀에서 유효한 행을 찾지 못했습니다.", file=sys.stderr)
        return 2

    if args.row is not None:
        contexts = [contexts[args.row]]

    out = Path(args.out)
    is_dir_out = out.suffix == "" or out.is_dir()
    if is_dir_out:
        out.mkdir(parents=True, exist_ok=True)

    for ctx in contexts:
        result = engine.run(ctx, rule_set)
        if args.json:
            print(json.dumps(_serialize(result), ensure_ascii=False, indent=2, default=str))
            continue
        target = (out / f"{_sanitize(ctx.sbjt_id)}_{_sanitize(ctx.orgn_id)}.pdf") \
                 if is_dir_out else out
        pdf.render_pdf(result, target)
        print(f"✓ {target}  ({result.grade.label}, {result.total_score})")

    return 0


def _serialize(result) -> dict:
    return {
        "sbjtId": result.sbjt_id,
        "orgnId": result.orgn_id,
        "predAt": result.pred_at.isoformat(),
        "totalScore": result.total_score,
        "grade": {"code": result.grade.code, "label": result.grade.label,
                  "action": result.grade.action},
        "ruleVersion": result.rule_version,
        "dimensions": [
            {"code": d.code, "label": d.label, "score": d.score, "maxScore": d.max_score,
             "detail": [{"rule": r.rule_name, "score": r.score, "max": r.max,
                         "input": r.input_value} for r in d.detail]}
            for d in result.dimensions
        ],
    }


if __name__ == "__main__":
    sys.exit(main())
