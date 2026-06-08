"""数据访问层 — 封装所有数据库查询和写入操作"""

import pandas as pd
from sqlalchemy import text

from config.db_config import get_engine


def query_routes(origin_city, start_date, destination_codes=None):
    """
    查询从出发城市可达的目的地及交通信息

    :param destination_codes: 候选目的地编码列表，为空则查询所有
    :return: DataFrame (destination_code, transport_type, traffic_cost,
             duration_minutes, convenience_score)
    """
    engine = get_engine()

    base_sql = """
        SELECT r.destination_code,
               r.transport_type,
               r.traffic_cost,
               r.duration_minutes,
               r.convenience_score
        FROM dwd_route_detail r
        WHERE r.origin_city = :origin_city
          AND r.travel_date = :start_date
    """

    params = {"origin_city": origin_city, "start_date": start_date}

    if destination_codes:
        placeholders = []
        for i, code in enumerate(destination_codes):
            key = f"code_{i}"
            placeholders.append(f":{key}")
            params[key] = code
        base_sql += f" AND r.destination_code IN ({', '.join(placeholders)})"

    return pd.read_sql(text(base_sql), engine, params=params)


def query_destinations(dest_codes):
    """
    查询目的地基础信息

    :param dest_codes: list of destination_code strings
    :return: DataFrame (destination_code, destination_name, destination_type,
             scenery_score, popularity_score, facility_score)
    """
    engine = get_engine()
    code_placeholders = ", ".join([f"'{c}'" for c in dest_codes])
    dest_sql = text(f"""
        SELECT destination_code, destination_name, destination_type,
               scenery_score, popularity_score, facility_score
        FROM dwd_destination
        WHERE destination_code IN ({code_placeholders}) AND is_active = 1
    """)
    return pd.read_sql(dest_sql, engine)


def query_weather(dest_codes, start_date, end_date):
    """
    查询旅行期间天气，按目的地聚合取平均

    :return: DataFrame (destination_code, avg_weather_score,
             avg_temp_high, avg_temp_low)
    """
    engine = get_engine()
    code_placeholders = ", ".join([f"'{c}'" for c in dest_codes])
    weather_sql = text(f"""
        SELECT destination_code,
               AVG(weather_score)  AS avg_weather_score,
               AVG(temp_max) AS avg_temp_high,
               AVG(temp_min)  AS avg_temp_low
        FROM dwd_weather_detail
        WHERE destination_code IN ({code_placeholders})
          AND weather_date BETWEEN :start_date AND :end_date
        GROUP BY destination_code
    """)
    return pd.read_sql(weather_sql, engine,
                       params={"start_date": start_date, "end_date": end_date})


def query_hotels(dest_codes, start_date, end_date):
    """
    查询酒店价格

    :return: DataFrame (destination_code, avg_price, hotel_score)
    """
    engine = get_engine()
    code_placeholders = ", ".join([f"'{c}'" for c in dest_codes])
    hotel_sql = text(f"""
        SELECT destination_code, avg_price, hotel_score
        FROM dwd_hotel_price_detail
        WHERE destination_code IN ({code_placeholders})
          AND checkin_date = :start_date
          AND checkout_date = :end_date
    """)
    return pd.read_sql(hotel_sql, engine,
                       params={"start_date": start_date, "end_date": end_date})


def save_results(results, request_id=None):
    """
    将推荐结果写入 ads_destination_recommend_result 表

    先删除同 request_id 的旧数据，再批量插入新数据
    """
    if not results:
        print("[INFO] 无推荐结果可写入")
        return

    engine = get_engine()
    result_df = pd.DataFrame(results)

    # 使用传入的 request_id 或从结果中获取
    rid = request_id if request_id else results[0]["request_id"]

    with engine.begin() as conn:
        conn.execute(
            text("DELETE FROM ads_destination_recommend_result WHERE request_id = :rid"),
            {"rid": rid}
        )

        result_df.to_sql(
            "ads_destination_recommend_result",
            con=conn,
            if_exists="append",
            index=False,
            method="multi",
        )
    print(f"[INFO] 成功写入 {len(result_df)} 条推荐结果 (request_id={rid})")
