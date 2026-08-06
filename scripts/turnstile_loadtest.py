#!/usr/bin/env python3
"""Turnstile 캡차 적용 전/후 로그인 엔드포인트 부하 비교.

실행 방법:
    pip install requests matplotlib bcrypt
    cd backend && mvn spring-boot:run          # :8080 에서 기동, TURNSTILE_SECRET_KEY 설정 필요
    python3 scripts/turnstile_loadtest.py

무엇을 비교하나:
  AFTER  (실측)       실제 로컬 백엔드의 POST /api/branch-auth/db-login 에 캡차 토큰 없이 요청.
                      TurnstileVerifier.verify()가 DB/BCrypt 이전에 즉시 막으므로(400) 실제 서버가
                      감당하는 부하를 그대로 측정한다.
  BEFORE (시뮬레이션)   캡차가 없었다면 매 요청이 반드시 거쳤을 비용(DB 조회 + 비밀번호 검증)을
                      로컬에서 동일 알고리즘(BCrypt cost=10, Spring의 기본 BCryptPasswordEncoder())으로
                      재현해서 측정한다. 실서버 인증 코드는 건드리지 않는다 - Turnstile은 애초에
                      스크립트로 자동 우회가 안 되게 설계된 것이라 "캡차 없음"을 실서버로 그대로
                      재현할 방법이 없다.

결과: 콘솔에 표로 출력 + <out> 경로에 PNG 차트 저장.
"""
import argparse
import json
import time
from concurrent.futures import ThreadPoolExecutor

import bcrypt
import requests

AFTER_COLOR = "#2a78d6"   # 팔레트 slot 1 (blue) - Turnstile 적용 후, 실측
BEFORE_COLOR = "#eb6834"  # 팔레트 slot 2 (orange) - 캡차 없음, 시뮬레이션
INK_PRIMARY = "#0b0b0b"
INK_SECONDARY = "#52514e"
INK_MUTED = "#898781"
GRIDLINE = "#e1e0d9"
SURFACE = "#fcfcfb"


def _selftest():
    """비순수 코드(스레드/네트워크)를 건드리기 전에 집계 로직만 빠르게 검증."""
    assert p95([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]) == 10
    assert p95([10]) == 10
    stats = aggregate([(0.01, True), (0.02, True), (0.03, False)], wall_seconds=0.05)
    assert stats["fail"] == 1
    assert round(stats["avg_ms"], 2) == round((10 + 20 + 30) / 3, 2)
    assert stats["throughput"] == 3 / 0.05


def p95(values):
    s = sorted(values)
    idx = min(int(len(s) * 0.95), len(s) - 1)
    return s[idx]


def aggregate(results, wall_seconds):
    latencies_ms = [r[0] * 1000 for r in results]
    fails = sum(1 for _, ok in results if not ok)
    return {
        "avg_ms": sum(latencies_ms) / len(latencies_ms),
        "p95_ms": p95(latencies_ms),
        "fail": fails,
        "throughput": len(results) / wall_seconds,
    }


def make_before_fn(bcrypt_cost, db_latency_s):
    # login_id는 UNIQUE 인덱스(단건 조회)라 DB 지연은 상수로 모델링해도 충분하고,
    # 실제 비용은 BCrypt 쪽(cost=10에서 수십 ms)이 지배적이다.
    dummy_hash = bcrypt.hashpw(b"real-password", bcrypt.gensalt(bcrypt_cost))

    def _before():
        start = time.perf_counter()
        time.sleep(db_latency_s)
        bcrypt.checkpw(b"attacker-guess", dummy_hash)
        return time.perf_counter() - start, True

    return _before


def make_after_fn(url):
    payload = {"loginId": "loadtest-probe", "password": "wrong-password", "turnstileToken": ""}

    def _after():
        start = time.perf_counter()
        try:
            resp = requests.post(url, json=payload, timeout=5)
            ok = resp.status_code == 400  # 토큰 없음 -> 즉시 차단(400)이 "정상"
        except requests.RequestException:
            ok = False
        return time.perf_counter() - start, ok

    return _after


def run_level(fn, concurrency, total_requests):
    with ThreadPoolExecutor(max_workers=concurrency) as pool:
        wall_start = time.perf_counter()
        results = list(pool.map(lambda _: fn(), range(total_requests)))
        wall = time.perf_counter() - wall_start
    return aggregate(results, wall)


def run_all(levels, requests_per_level, before_fn, after_fn):
    rows = []
    for c in levels:
        before = run_level(before_fn, c, requests_per_level)
        after = run_level(after_fn, c, requests_per_level)
        rows.append({"concurrency": c, "before": before, "after": after})
        print(
            f"동시요청 {c:>4} | BEFORE avg {before['avg_ms']:7.2f}ms p95 {before['p95_ms']:7.2f}ms "
            f"{before['throughput']:8.1f} req/s | "
            f"AFTER avg {after['avg_ms']:6.2f}ms p95 {after['p95_ms']:6.2f}ms "
            f"{after['throughput']:8.1f} req/s (fail {after['fail']})"
        )
    return rows


def plot(rows, out_path):
    import matplotlib.pyplot as plt

    # 기본 폰트(DejaVu Sans)는 한글 글리프가 없어 라벨이 깨진다 - 설치된 OS별 한글 폰트 중 있는 것만 사용.
    from matplotlib import font_manager
    installed = {f.name for f in font_manager.fontManager.ttflist}
    korean_font = next((f for f in ("AppleGothic", "Malgun Gothic", "NanumGothic") if f in installed), "sans-serif")
    plt.rcParams["font.family"] = korean_font
    plt.rcParams["axes.unicode_minus"] = False

    levels = [r["concurrency"] for r in rows]
    x = range(len(levels))
    before_avg = [r["before"]["avg_ms"] for r in rows]
    after_avg = [r["after"]["avg_ms"] for r in rows]
    before_tp = [r["before"]["throughput"] for r in rows]
    after_tp = [r["after"]["throughput"] for r in rows]

    fig, (ax_latency, ax_throughput) = plt.subplots(1, 2, figsize=(11, 4.5), facecolor=SURFACE)
    fig.suptitle("Turnstile 적용 전/후 로그인 엔드포인트 부하 비교", color=INK_PRIMARY, fontsize=13, fontweight="bold")

    line_kwargs = dict(linewidth=2, marker="o", markersize=8, markeredgewidth=2, markeredgecolor=SURFACE)

    ax_latency.set_facecolor(SURFACE)
    ax_latency.plot(x, before_avg, color=BEFORE_COLOR, label="BEFORE (캡차 없음, 시뮬레이션)", **line_kwargs)
    ax_latency.plot(x, after_avg, color=AFTER_COLOR, label="AFTER (Turnstile 적용, 실측)", **line_kwargs)
    ax_latency.set_yscale("log")
    ax_latency.set_title("평균 응답 지연시간 (ms, log)", color=INK_SECONDARY, fontsize=10)
    ax_latency.annotate(f"{before_avg[-1]:.0f}ms", (x[-1], before_avg[-1]), textcoords="offset points",
                         xytext=(6, 4), color=INK_PRIMARY, fontsize=9)
    ax_latency.annotate(f"{after_avg[-1]:.1f}ms", (x[-1], after_avg[-1]), textcoords="offset points",
                         xytext=(6, -10), color=INK_PRIMARY, fontsize=9)

    ax_throughput.set_facecolor(SURFACE)
    ax_throughput.plot(x, before_tp, color=BEFORE_COLOR, label="BEFORE (캡차 없음, 시뮬레이션)", **line_kwargs)
    ax_throughput.plot(x, after_tp, color=AFTER_COLOR, label="AFTER (Turnstile 적용, 실측)", **line_kwargs)
    ax_throughput.set_title("처리량 (req/s)", color=INK_SECONDARY, fontsize=10)
    ax_throughput.annotate(f"{before_tp[-1]:.0f}", (x[-1], before_tp[-1]), textcoords="offset points",
                            xytext=(6, 4), color=INK_PRIMARY, fontsize=9)
    ax_throughput.annotate(f"{after_tp[-1]:.0f}", (x[-1], after_tp[-1]), textcoords="offset points",
                            xytext=(6, -10), color=INK_PRIMARY, fontsize=9)

    for ax in (ax_latency, ax_throughput):
        ax.set_xticks(list(x))
        ax.set_xticklabels([str(c) for c in levels])
        ax.set_xlabel("동시요청 수", color=INK_MUTED, fontsize=9)
        ax.grid(axis="y", color=GRIDLINE, linewidth=0.8)
        ax.set_axisbelow(True)
        for spine in ("top", "right"):
            ax.spines[spine].set_visible(False)
        for spine in ("left", "bottom"):
            ax.spines[spine].set_color(GRIDLINE)
        ax.tick_params(colors=INK_MUTED, labelsize=8)
        ax.legend(frameon=False, labelcolor=INK_SECONDARY, fontsize=8, loc="upper left")

    fig.tight_layout(rect=(0, 0, 1, 0.94))
    fig.savefig(out_path, dpi=180, facecolor=SURFACE)
    print(f"\n차트 저장: {out_path}")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--endpoint", default="/api/branch-auth/db-login")
    parser.add_argument("--levels", default="1,10,50,100,200", help="비교할 동시요청 수 목록 (콤마 구분)")
    parser.add_argument("--requests-per-level", type=int, default=200)
    parser.add_argument("--db-latency-ms", type=float, default=5.0, help="BEFORE 시뮬레이션의 DB 왕복 모델링 값")
    parser.add_argument("--bcrypt-cost", type=int, default=10, help="Spring 기본 BCryptPasswordEncoder()와 동일")
    parser.add_argument("--out", default="scripts/turnstile_loadtest_result.png")
    parser.add_argument("--out-json", default=None, help="원시 결과를 JSON으로도 저장하고 싶으면 경로 지정")
    args = parser.parse_args()

    _selftest()

    levels = [int(c) for c in args.levels.split(",")]
    before_fn = make_before_fn(args.bcrypt_cost, args.db_latency_ms / 1000)
    after_fn = make_after_fn(args.base_url.rstrip("/") + args.endpoint)

    print(f"대상: {args.base_url}{args.endpoint} (turnstileToken 없이 요청 -> 400 기대)")
    print(f"BEFORE 모델: DB {args.db_latency_ms}ms + BCrypt cost={args.bcrypt_cost}\n")

    rows = run_all(levels, args.requests_per_level, before_fn, after_fn)
    plot(rows, args.out)

    if args.out_json:
        with open(args.out_json, "w", encoding="utf-8") as f:
            json.dump(rows, f, ensure_ascii=False, indent=2)
        print(f"원시 데이터 저장: {args.out_json}")


if __name__ == "__main__":
    main()
