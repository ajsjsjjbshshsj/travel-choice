"""CLI 入口 — 旅行目的地推荐评分"""

import io
import sys

# Fix Windows console encoding
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

from jobs.build_recommend_result_job import build_recommend_result
from repository.recommend_repository import save_results


def main():
    """参数: originCity startDate endDate userBudget requestId [destinationCodes]"""
    if len(sys.argv) < 6:
        print("用法: python recommend_score_job.py originCity startDate endDate userBudget requestId [destinationCodes]")
        sys.exit(1)

    origin_city = sys.argv[1]
    start_date  = sys.argv[2]
    end_date    = sys.argv[3]
    user_budget = float(sys.argv[4])
    request_id  = sys.argv[5]

    destination_codes = []
    if len(sys.argv) >= 7 and sys.argv[6].strip():
        destination_codes = [x.strip() for x in sys.argv[6].split(",") if x.strip()]

    print(f"[INFO] 开始推荐计算: 出发城市={origin_city}, 日期={start_date}~{end_date}, "
          f"预算={user_budget}, requestId={request_id}, "
          f"候选目的地={destination_codes if destination_codes else '全部'}")

    results = build_recommend_result(
        origin_city, start_date, end_date, user_budget,
        request_id=request_id,
        destination_codes=destination_codes
    )

    if results:
        print(f"\n{'='*60}")
        print(f"{'排名':<4} {'目的地':<20} {'总分':<8} {'等级':<8} {'预估费用':<12}")
        print(f"{'='*60}")
        for r in results:
            print(f"{r['recommend_rank']:<4} {r['destination_name']:<20} "
                  f"{r['final_score']:<8} {r['recommend_level']:<8} "
                  f"¥{r['estimated_total_cost']:<11}")
        print(f"{'='*60}\n")

        save_results(results, request_id)
    else:
        print("[WARN] 未生成任何推荐结果")


if __name__ == "__main__":
    main()
