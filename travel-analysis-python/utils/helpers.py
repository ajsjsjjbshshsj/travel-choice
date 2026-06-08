"""通用工具函数"""

from decimal import Decimal


def clamp(value, min_value=0, max_value=100):
    """将值限制在 [min_value, max_value] 范围内，None 或 NaN 返回默认值"""
    if value is None:
        return Decimal(str(min_value))
    try:
        if not isinstance(value, Decimal):
            value = Decimal(str(value))
    except Exception:
        return Decimal(str(min_value))
    min_val = Decimal(str(min_value))
    max_val = Decimal(str(max_value))
    return max(min_val, min(max_val, value))
