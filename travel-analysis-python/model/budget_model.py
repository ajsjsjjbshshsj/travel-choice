"""预算/费用模型 — 费用估算与预算匹配度评分"""

from decimal import Decimal

from utils.helpers import clamp

# 每日餐饮估算
FOOD_COST_PER_DAY = Decimal("150")
# 门票估算（固定）
TICKET_COST = Decimal("200")


def estimate_cost(traffic_cost, hotel_nightly, travel_days):
    """
    估算总旅行费用

    :param traffic_cost: 交通费用（单程）
    :param hotel_nightly: 酒店每晚价格
    :param travel_days: 旅行天数
    :return: Decimal 预估总费用
    """
    traffic_cost = Decimal(str(traffic_cost))
    hotel_nightly = Decimal(str(hotel_nightly))
    hotel_cost = hotel_nightly * travel_days
    food_cost = FOOD_COST_PER_DAY * travel_days
    estimated_total_cost = traffic_cost + hotel_cost + food_cost + TICKET_COST
    return estimated_total_cost, hotel_cost, food_cost, TICKET_COST


def compute_cost_score(estimated_total_cost, user_budget):
    """
    预算匹配度评分：费用越低于预算得分越高

    :param estimated_total_cost: 预估总费用 (Decimal)
    :param user_budget: 用户预算 (float or Decimal)
    :return: Decimal 费用得分 (0-100)
    """
    if user_budget > 0 and estimated_total_cost > 0:
        cost_ratio = estimated_total_cost / Decimal(str(user_budget))
        if cost_ratio <= Decimal("0.7"):
            cost_score = Decimal("95")
        elif cost_ratio <= Decimal("1.0"):
            cost_score = Decimal("80")
        elif cost_ratio <= Decimal("1.3"):
            cost_score = Decimal("60")
        else:
            cost_score = clamp(Decimal("100") - (cost_ratio - Decimal("1")) * Decimal("50"))
    else:
        cost_score = Decimal("70")
    return cost_score
