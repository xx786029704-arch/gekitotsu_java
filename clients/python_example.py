"""
gekitotsu_java 对战模拟器 Python 调用示例

前置步骤：
1. 先编译打包 Java 端：在项目根目录运行 `mvn package`
2. 启动服务：双击 start.bat（Windows）或运行 ./start.sh（Linux/Mac）
3. 看到输出 "监听: http://localhost:8080" 后再运行本脚本

依赖：requests 库（pip install requests）
"""

import requests

BASE = "http://localhost:8080"

def health_check():
    """健康检查，确认服务可用"""
    r = requests.get(f"{BASE}/health", timeout=5)
    r.raise_for_status()
    return r.json()


def battle(fort1_name, fort1_code, fort2_name, fort2_code, max_frames=None):
    """单局对战（每次传完整代码）"""
    payload = {
        "fort1": {"name": fort1_name, "code": fort1_code},
        "fort2": {"name": fort2_name, "code": fort2_code},
    }
    if max_frames is not None:
        payload["max_frames"] = max_frames
    r = requests.post(f"{BASE}/battle", json=payload, timeout=300)
    if r.status_code != 200:
        print(f"对战失败 [{r.status_code}]: {r.text}")
        return None
    return r.json()


def batch_battles(battles):
    """批量对战。battles 是 list of dict，每个 dict 含 fort1/fort2/可选 max_frames"""
    r = requests.post(f"{BASE}/battles", json={"battles": battles}, timeout=600)
    r.raise_for_status()
    return r.json()


def load_slot(side, forts):
    """上传阵容到槽位。side=1 或 2，forts 是 list of {'name':..., 'code':...}"""
    r = requests.put(f"{BASE}/p{side}", json={"forts": forts}, timeout=60)
    r.raise_for_status()
    return r.json()


def get_slot(side):
    """查看当前槽位"""
    r = requests.get(f"{BASE}/p{side}", timeout=5)
    r.raise_for_status()
    return r.json()


def clear_slot(side):
    """清空槽位"""
    r = requests.delete(f"{BASE}/p{side}", timeout=5)
    r.raise_for_status()
    return r.json()


def run(max_frames=None):
    """触发 1P × 2P 笛卡尔积循环匹配对战"""
    payload = {} if max_frames is None else {"max_frames": max_frames}
    r = requests.post(f"{BASE}/run", json=payload, timeout=600)
    r.raise_for_status()
    return r.json()


# 示例阵型代码（从 1P.txt 复制的真实代码，玩家可替换为自己的）
SAMPLE_FORT = "000j9pin6SySlfaoEVj0RaNw60dlClNpDb154ogJw1a4tFvma4tCOnamoe13amogH1p00t5Op00ESw7kieXT7kdPLe7kmG8Q7kvwvu7kr9DO9o7PDo9o7Hje9obXbMajuCDkt006vN"


def main():
    print("=== 1. 健康检查 ===")
    info = health_check()
    print(f"服务状态: {info['status']}, 线程数: {info['threads']}")
    print()

    print("=== 2. 单局对战 /battle ===")
    result = battle("测试1", SAMPLE_FORT, "测试2", SAMPLE_FORT)
    if result:
        print(f"  status={result['status']}, hp={result['winner_hp']}, "
              f"frames={result['frames']}, {result['time_ms']:.2f}ms")
    print()

    print("=== 3. 批量对战 /battles ===")
    batch_result = batch_battles([
        {"fort1": {"name": "AI1", "code": SAMPLE_FORT},
         "fort2": {"name": "基准", "code": SAMPLE_FORT}},
        {"fort1": {"name": "AI2", "code": SAMPLE_FORT},
         "fort2": {"name": "基准", "code": SAMPLE_FORT}},
    ])
    print(f"  批量总用时: {batch_result['total_time_ms']:.2f} ms")
    for r in batch_result["results"]:
        print(f"  status={r['status']}, hp={r['winner_hp']}")
    print()

    print("=== 4. 持久化槽位模式（推荐训练用）===")
    # 训练循环示意：基准 2P 一次上传，AI 1P 每轮换新
    print("  [4.1] 上传基准 2P（仅一次）")
    print("    ", load_slot(2, [{"name": "基准", "code": SAMPLE_FORT}]))

    print("  [4.2] 第 1 轮：上传 2 个 AI 阵型")
    print("    ", load_slot(1, [
        {"name": "AI-v1-001", "code": SAMPLE_FORT},
        {"name": "AI-v1-002", "code": SAMPLE_FORT},
    ]))

    print("  [4.3] 触发 /run（2 AI × 1 基准 = 2 局）")
    run_result = run()
    print(f"  总用时: {run_result['total_time_ms']:.2f} ms")
    for r in run_result["results"]:
        print(f"  {r['fort1_name']} vs {r['fort2_name']}: status={r['status']}, hp={r['winner_hp']}")

    print("  [4.4] 第 2 轮：换新 AI（2P 不变）")
    print("    ", load_slot(1, [
        {"name": "AI-v2-001", "code": SAMPLE_FORT},
        {"name": "AI-v2-002", "code": SAMPLE_FORT},
        {"name": "AI-v2-003", "code": SAMPLE_FORT},
    ]))
    run_result = run(max_frames=30000)   # 可以每轮调帧数
    print(run_result)

    print("  [4.5] 查询当前槽位状态")
    print("    1P:", get_slot(1))
    print("    2P:", get_slot(2))

    print("  [4.6] 清理")
    print("    ", clear_slot(1))
    print("    ", clear_slot(2))
    print()

    print("=== 5. 错误处理 ===")
    print("  阵型代码过短:")
    bad = battle("坏阵", "abc", "基准", SAMPLE_FORT)
    if bad is None:
        print("    正确返回 400")
    print("  PUT 槽位时阵型非法（槽位应保持不变）:")
    resp = requests.put(f"{BASE}/p1", json={"forts": [{"name": "坏", "code": "abc"}]})
    print(f"    HTTP {resp.status_code}: {resp.json()}")
    print(f"    当前 1P 槽位（应为空）: {get_slot(1)}")
    print()

    print("所有测试完成！")


if __name__ == "__main__":
    main()
