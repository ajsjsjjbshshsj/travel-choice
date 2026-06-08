"""综合评分模型 — 加权总分计算、推荐等级判定、推荐理由生成"""

from decimal import Decimal

from utils.helpers import clamp

# 各评分维度权重
WEIGHTS = {
    "scenery": Decimal("0.25"),
    "weather": Decimal("0.20"),
    "hotel":   Decimal("0.15"),
    "traffic": Decimal("0.15"),
    "cost":    Decimal("0.15"),
    "crowd":   Decimal("0.10"),
}


def compute_final_score(scenery_score, weather_score, hotel_score,
                        traffic_score, cost_score, crowd_score):
    """
    计算加权总分

    :return: Decimal, clamp 到 [0, 100]，保留 2 位小数
    """
    final_score = (
        scenery_score * WEIGHTS["scenery"] +
        weather_score * WEIGHTS["weather"] +
        hotel_score   * WEIGHTS["hotel"] +
        traffic_score * WEIGHTS["traffic"] +
        cost_score    * WEIGHTS["cost"] +
        crowd_score   * WEIGHTS["crowd"]
    )
    return round(clamp(final_score), 2)


def determine_level(final_score):
    """
    根据总分判定推荐等级

    :return: "HIGH" | "MEDIUM" | "LOW"
    """
    if final_score >= Decimal("85"):
        return "HIGH"
    elif final_score >= Decimal("70"):
        return "MEDIUM"
    else:
        return "LOW"


def generate_reasons(scenery_score, weather_score, cost_score,
                     crowd_score, traffic_score):
    """
    根据各维度得分阈值生成推荐理由

    :return: str, 中文推荐理由，用 "；" 连接
    """
    reasons = []
    if scenery_score >= 90:
        reasons.append("景观评分极高")
    if weather_score >= 80:
        reasons.append("天气数据来自高德天气查询 API，旅行期间天气良好")
    if cost_score >= 80:
        reasons.append("费用在预算范围内")
    if crowd_score >= 75:
        reasons.append("游客相对较少，体验更佳")
    if traffic_score >= 85:
        reasons.append("路线耗时来自高德路径规划 API，交通便利")
    return "；".join(reasons) if reasons else "综合评分一般"
