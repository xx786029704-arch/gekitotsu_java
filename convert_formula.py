"""将 formula.xlsx 转换为 formula.json，供 Java 程序读取。

用法: python convert_formula.py

输出结构:
{
  "px": [sp1][sp2][seg][4]  1P要塞x坐标分段公式
  "qx": [sp1][sp2][seg][4]  2P要塞x坐标分段公式
  "py": [sp1][sp2][seg][4]  1P要塞y坐标分段公式
  "qy": [sp1][sp2][seg][4]  2P要塞y坐标分段公式
}

每段为 [t_start, a, v0, x0]，表示公式:
  x(t) = a*(t-t_start)²/2 + v0*(t-t_start) + x0
"""

import json
import re
import openpyxl

SHEET_MAP = {"1P x": "px", "2P x": "qx", "1P y": "py", "2P y": "qy"}
SPEED_LEVELS = 16


def parse_cell(cell_text):
    """解析一个单元格的分段公式字符串，返回段列表。

    每段格式: A<=t<B: FORMULA
    FORMULA 可能是:
      - 纯数字 (常数段，a=0, v0=0)
      - a*t²/2 + v*t + x  (t0=0 的匀加速)
      - a*(t-N)²/2 + v*(t-N) + x  (t0=N 的匀加速)
    """
    segments = []
    parts = cell_text.split(", ")

    for part in parts:
        part = part.strip()
        if not part:
            continue

        # 分离范围和公式
        m = re.match(r"(\d+)<=t<(\d+):\s*(.+)", part)
        if not m:
            continue
        t_start = int(m.group(1))
        formula = m.group(3).strip()

        if "t" not in formula:
            # 常数段
            segments.append([t_start, 0.0, 0.0, float(formula)])
        else:
            # 提取 t0: 查找 (t-N) 模式
            t0_match = re.search(r"\(t-(\d+)\)", formula)
            t0 = int(t0_match.group(1)) if t0_match else 0

            # 提取系数 a: (t-N)²/2 或 t²/2 前的数字
            a_match = re.search(
                r"([+-]?\d+\.\d+)\s*(?:\(t-\d+\)|t)\^2/2", formula
            )
            a = float(a_match.group(1)) if a_match else 0.0

            # 提取系数 v0: ^2/2 + ... (t-N) 或 + ... t 中间的数字
            v_match = re.search(
                r"\^2/2\s*\+\s*([+-]?\d+\.\d+)\s*(?:\(t-\d+\)|t)", formula
            )
            v0 = float(v_match.group(1)) if v_match else 0.0

            # 提取 x0: 最后一个浮点数
            x_match = re.search(r"([+-]?\d+\.\d+)\s*$", formula)
            x0 = float(x_match.group(1)) if x_match else 0.0

            segments.append([t_start, a, v0, x0])

    return segments


def main():
    wb = openpyxl.load_workbook("formula.xlsx")
    result = {}

    for sheet_name, key in SHEET_MAP.items():
        ws = wb[sheet_name]
        # ws: 行3-18 = sp1 0-15, 列B-Q(2-17) = sp2 0-15
        dim_data = []
        for sp1_row in range(3, 3 + SPEED_LEVELS):
            sp1_data = []
            for sp2_col in range(2, 2 + SPEED_LEVELS):
                cell_val = str(ws.cell(sp1_row, sp2_col).value or "")
                segs = parse_cell(cell_val)
                sp1_data.append(segs)
            dim_data.append(sp1_data)
        result[key] = dim_data

    with open("formula.json", "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False)

    # 统计
    total_segs = 0
    for dim_key in ["px", "qx", "py", "qy"]:
        dim_segs = sum(
            len(result[dim_key][sp1][sp2])
            for sp1 in range(SPEED_LEVELS)
            for sp2 in range(SPEED_LEVELS)
        )
        print(f"  {dim_key}: {dim_segs} 段")
        total_segs += dim_segs
    print(f"  总计: {total_segs} 段")
    print("已生成 formula.json")


if __name__ == "__main__":
    main()
