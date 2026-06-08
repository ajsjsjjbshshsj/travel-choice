"""拥挤度模型 — 基于人气指数计算拥挤度评分"""

from decimal import Decimal

from utils.helpers import clamp


def compute_crowd_score(popularity_score):
    """
    基于人气指数计算拥挤度评分

    人气越高 → 越拥挤 → 得分越低

    :param popularity_score: 人气指数 (Decimal or numeric)
    :return: (crowd_index, crowd_score) 元组，均为 Decimal
    """
    popularity = Decimal(str(popularity_score))
    crowd_index = popularity
    crowd_score = clamp(Decimal("100") - popularity * Decimal("0.5"))
    return crowd_index, crowd_score
