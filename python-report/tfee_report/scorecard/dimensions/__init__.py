from .d1_project_fit import calculate as d1
from .d2_financial import calculate as d2
from .d3_capital_efficiency import calculate as d3
from .d4_market_signal import calculate as d4
from .d5_execution_signal import calculate as d5

__all__ = ["d1", "d2", "d3", "d4", "d5", "ALL"]

ALL = [
    ("D1", "D1_project_fit", d1),
    ("D2", "D2_financial_capacity", d2),
    ("D3", "D3_capital_efficiency", d3),
    ("D4", "D4_market_signal", d4),
    ("D5", "D5_execution_signal", d5),
]
