"""scorecard.yml 로딩.

기본은 Java 패치와 같은 파일을 공유:
    enftrms-patch/src/main/resources/rules/scorecard.yml
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Optional

import yaml

DEFAULT_YAML_PATH = (
    Path(__file__).resolve().parents[3]
    / "enftrms-patch"
    / "src"
    / "main"
    / "resources"
    / "rules"
    / "scorecard.yml"
)


@dataclass
class Threshold:
    gte: Optional[float] = None
    lte: Optional[float] = None
    score: float = 0.0


@dataclass
class Bucket:
    range: list[float] = field(default_factory=list)
    score: float = 0.0


@dataclass
class Rule:
    name: str
    max: float
    type: str = "threshold"
    input: str = ""
    score_when_true: Optional[float] = None
    slope: Optional[float] = None
    cap: Optional[float] = None
    thresholds: list[Threshold] = field(default_factory=list)
    buckets: list[Bucket] = field(default_factory=list)


@dataclass
class Dimension:
    label: str
    weight: float
    rules: list[Rule] = field(default_factory=list)
    source: str = ""
    event_scores: dict[str, float] = field(default_factory=dict)


@dataclass
class GradeRule:
    min: float
    code: str
    label: str
    action: str


@dataclass
class RuleSet:
    rule_version: str
    effective_from: str
    dimensions: dict[str, Dimension]
    grades: list[GradeRule]
    raw_yaml: str = ""


def _camel(snake: str) -> str:
    parts = snake.split("_")
    return parts[0] + "".join(p.title() for p in parts[1:])


def _rule_from_dict(d: dict[str, Any]) -> Rule:
    return Rule(
        name=d["name"],
        max=float(d["max"]),
        type=d.get("type", "threshold"),
        input=d.get("input", ""),
        score_when_true=d.get("scoreWhenTrue"),
        slope=d.get("slope"),
        cap=d.get("cap"),
        thresholds=[Threshold(**t) for t in d.get("thresholds", [])],
        buckets=[Bucket(**b) for b in d.get("buckets", [])],
    )


def _dim_from_dict(d: dict[str, Any]) -> Dimension:
    return Dimension(
        label=d["label"],
        weight=float(d["weight"]),
        rules=[_rule_from_dict(r) for r in d.get("rules", [])],
        source=d.get("source", ""),
        event_scores={k: float(v) for k, v in (d.get("eventScores") or {}).items()},
    )


def load(path: Optional[Path] = None) -> RuleSet:
    p = Path(path) if path else DEFAULT_YAML_PATH
    raw = p.read_text(encoding="utf-8")
    data = yaml.safe_load(raw)
    return RuleSet(
        rule_version=data["ruleVersion"],
        effective_from=data.get("effectiveFrom", ""),
        dimensions={k: _dim_from_dict(v) for k, v in data["dimensions"].items()},
        grades=[GradeRule(**g) for g in data["grades"]],
        raw_yaml=raw,
    )
