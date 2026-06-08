"""编排层 — 组合数据查询与评分模型，生成推荐结果"""

import uuid
from decimal import Decimal

import pandas as pd

from repository.recommend_repository import (
    query_routes, query_destinations, query_weather, query_hotels
)
from model.budget_model import estimate_cost, compute_cost_score
from model.crowd_index_model import compute_crowd_score
from model.score_model import compute_final_score, determine_level, generate_reasons


def build_recommend_result(origin_city, start_date, end_date, user_budget,
                           request_id=None, destination_codes=None):
    """
    构建目的地推荐结果

    1. 查询交通路线、目的地信息、天气、酒店价格
    2. 合并数据
    3. 对每个目的地计算各维度得分和加权总分
    4. 按总分排序并设置排名

    :param request_id: Java 传入的请求 ID，用于结果隔离
    :param destination_codes: 候选目的地编码列表，为空则查询所有
    :return: list[dict] 推荐结果列表
    """
    travel_days = (pd.to_datetime(end_date) - pd.to_datetime(start_date)).days
    if travel_days <= 0:
        print(f"[WARN] 无效旅行天数: {start_date} ~ {end_date}")
        return []

    # 1. 查询从出发城市可达的目的地及交通信息
    route_df = query_routes(origin_city, start_date, destination_codes)
    if route_df.empty:
        print(f"[WARN] 未找到从 {origin_city} 出发、日期 {start_date} 的交通路线")
        return []

    dest_codes = route_df["destination_code"].tolist()

    # 2. 查询目的地基础信息
    dest_df = query_destinations(dest_codes)

    # 3. 查询天气
    weather_df = query_weather(dest_codes, start_date, end_date)

    # 4. 查询酒店价格
    hotel_df = query_hotels(dest_codes, start_date, end_date)

    # 5. 合并所有维度
    merged = route_df.merge(dest_df, on="destination_code", how="inner") \
                     .merge(weather_df, on="destination_code", how="left") \
                     .merge(hotel_df, on="destination_code", how="left")

    # 6. 计算各项得分 & 总费用
    results = []
    # 使用 Java 传入的 request_id，如果没有则自动生成
    if not request_id:
        request_id = str(uuid.uuid4())
    model_version = "v3.0"

    for _, row in merged.iterrows():
        dest_code = row["destination_code"]
        dest_name = row.get("destination_name", "")

        # 费用估算
        traffic_cost_raw = row.get("traffic_cost", 0)
        traffic_cost_val = Decimal(str(traffic_cost_raw)) if traffic_cost_raw is not None and not pd.isna(traffic_cost_raw) else Decimal("0")
        hotel_nightly_raw = row.get("avg_price", 0)
        hotel_nightly = Decimal(str(hotel_nightly_raw)) if hotel_nightly_raw is not None and not pd.isna(hotel_nightly_raw) else Decimal("0")
        estimated_total_cost, hotel_cost, food_cost, ticket_cost = estimate_cost(
            traffic_cost_val, hotel_nightly, travel_days
        )

        # 各维度评分（处理 NaN 和 None）
        def safe_decimal(val, default=60):
            if val is None or (hasattr(val, '__float__') and pd.isna(val)):
                return Decimal(str(default))
            return Decimal(str(val))

        scenery_score = safe_decimal(row.get("scenery_score", 60), 60)
        weather_score = safe_decimal(row.get("avg_weather_score", 60), 60)
        hotel_score   = safe_decimal(row.get("hotel_score", 70), 70)
        traffic_score = safe_decimal(row.get("convenience_score", 60), 60)

        # 拥挤度评分
        pop_raw = row.get("popularity_score", 60)
        popularity = pop_raw if pop_raw is not None and not pd.isna(pop_raw) else 60
        crowd_index, crowd_score = compute_crowd_score(popularity)

        # 预算匹配度评分
        cost_score = compute_cost_score(estimated_total_cost, user_budget)

        # 加权总分
        final_score = compute_final_score(
            scenery_score, weather_score, hotel_score,
            traffic_score, cost_score, crowd_score
        )

        # 推荐等级
        recommend_level = determine_level(final_score)

        # 推荐理由
        recommend_reason = generate_reasons(
            scenery_score, weather_score, cost_score,
            crowd_score, traffic_score
        )

        results.append({
            "request_id": request_id,
            "origin_city": origin_city,
            "destination_code": dest_code,
            "destination_name": dest_name,
            "travel_start_date": start_date,
            "travel_end_date": end_date,
            "travel_days": travel_days,
            "user_budget": Decimal(str(user_budget)),
            "estimated_total_cost": round(estimated_total_cost, 2),
            "traffic_cost": round(traffic_cost_val, 2),
            "hotel_cost": round(hotel_cost, 2),
            "food_cost": round(food_cost, 2),
            "ticket_cost": round(ticket_cost, 2),
            "scenery_score": round(scenery_score, 2),
            "traffic_score": round(traffic_score, 2),
            "hotel_score": round(hotel_score, 2),
            "weather_score": round(weather_score, 2),
            "cost_score": round(cost_score, 2),
            "crowd_index": round(crowd_index, 2),
            "crowd_score": round(crowd_score, 2),
            "final_score": final_score,
            "recommend_level": recommend_level,
            "recommend_reason": recommend_reason,
            "model_version": model_version,
        })

    # 7. 按 final_score 排序并设置排名
    results.sort(key=lambda x: x["final_score"], reverse=True)
    for rank, item in enumerate(results, start=1):
        item["recommend_rank"] = rank

    return results
