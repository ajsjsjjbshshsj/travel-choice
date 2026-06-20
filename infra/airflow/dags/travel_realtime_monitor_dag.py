import os
from datetime import datetime, timedelta

from airflow import DAG
from airflow.operators.bash import BashOperator


KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "travel-kafka:29092")
CLICKHOUSE_URL = os.getenv("CLICKHOUSE_HTTP_URL", "http://clickhouse:8123")

default_args = {
    "owner": "travel-platform",
    "depends_on_past": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=2),
}

with DAG(
    dag_id="travel_realtime_monitor_dag",
    default_args=default_args,
    description="Monitor Kafka and ClickHouse realtime travel pipeline",
    schedule="*/10 * * * *",
    start_date=datetime(2026, 6, 1),
    catchup=False,
    tags=["travel", "realtime", "v5"],
) as dag:
    kafka_host, kafka_port = KAFKA_BOOTSTRAP_SERVERS.split(":", 1)

    check_kafka = BashOperator(
        task_id="check_kafka",
        bash_command=f"timeout 5 bash -c '</dev/tcp/{kafka_host}/{kafka_port}'",
    )

    check_clickhouse_table = BashOperator(
        task_id="check_clickhouse_table",
        bash_command=(
            "curl -fsS "
            f"'{CLICKHOUSE_URL}/?query=EXISTS%20TABLE%20travel_realtime.ads_hot_destination_topn'"
        ),
    )

    check_kafka >> check_clickhouse_table
