"""数据库连接配置"""

import os
from pathlib import Path

from dotenv import load_dotenv
from sqlalchemy import create_engine

# Load .env from project root (travel-analysis-python 的上一级)
load_dotenv(Path(__file__).resolve().parent.parent.parent / ".env")

DB_USER = os.getenv("MYSQL_USER", "root")
DB_PASSWORD = os.getenv("MYSQL_PASSWORD", "1234")
DB_HOST = os.getenv("MYSQL_HOST", "127.0.0.1")
DB_PORT = int(os.getenv("MYSQL_PORT", "3306"))
DB_NAME = os.getenv("MYSQL_DATABASE", "travel_decision_platform")


def get_engine():
    """创建 SQLAlchemy 数据库引擎"""
    url = f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}?charset=utf8mb4"
    return create_engine(url)
