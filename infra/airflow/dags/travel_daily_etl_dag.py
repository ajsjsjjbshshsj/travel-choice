import os
from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator


API_BASE_URL = os.getenv("TRAVEL_API_BASE_URL", "http://host.docker.internal:8080/api")

default_args = {
    "owner": "travel-platform",
    "depends_on_past": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="travel_daily_etl_dag",
    default_args=default_args,
    description="Daily travel ETL: weather, hotel price, data quality",
    schedule="@daily",
    start_date=datetime(2026, 6, 1),
    catchup=False,
    tags=["travel", "etl", "v4"],
) as dag:
    collect_weather = BashOperator(
        task_id="collect_weather",
        bash_command=f"curl -fsS -X POST {API_BASE_URL}/weather/collect",
    )

    generate_hotel_price = BashOperator(
        task_id="generate_hotel_price",
        bash_command=f"curl -fsS -X POST {API_BASE_URL}/hotel/generate",
    )

    run_quality_check = BashOperator(
        task_id="run_quality_check",
        bash_command=f"curl -fsS -X POST {API_BASE_URL}/quality/run",
    )

    collect_weather >> generate_hotel_price >> run_quality_check
