# 贡献分析板块设计

日期：2026-09-13
状态：已实现（2026-09-13）

## 概述

为激突Kit新增「贡献分析」标签页：给定玩家阵型（1P）和一套评判阵集（2P），通过批量对战计算删除若干单位前/后的胜率变化，帮助玩家找出对阵型强度贡献小（摸鱼）的单位，从而腾出军资金。

工具只做数据统计，不做自动修改阵型的决策；不考虑单位之间复杂的相互作用，定位为基础筛选工具。

## 目标

- 计算玩家阵型对评判阵集的基线胜率
- 枚举（或按探索率采样）所有「删除 n 个单位」的组合，逐个组合跑完整评判阵集
- 结果表点击表头自由排序（初始按胜率变化量降序），analyzer 内部保留 2 维度 × 2 方向排序能力
- 提供组合排行与单单位汇总两种视角
- 结果行可一键复制删除后的新阵型代码（保留 HP 后缀），便于玩家拿去实战验证

## 非目标

- 不做迭代式贪心删除（每次只评估一次性删除 n 个的方案）
- 不做自动改阵/自动优化建议
- 不做结果文件导出（只需剪贴板复制）
- 不修改模拟引擎（`GameTask`、`elements/`、`Main.compileFort`、`Main.to_xyr` 等）

## 架构

### 新增文件

- `src/main/java/org/example/ContributionAnalyzer.java`
  - 纯逻辑类，无 Swing 依赖
  - 位于 `org.example` 包，可直接读取/构造 `CompiledFort` 的包内字段（裁剪 SoA 数组）
  - 可引用 `org.example.GUI` 中的 `Unit`（费用表）、`Formation`（解析/重编码）
- `src/main/java/org/example/GUI/ContributionTab.java`
  - Swing 界面：数据输入面板 + 参数调节面板 + 结果显示面板 + 进度/状态
- `src/main/java/org/example/GUI/WrapLayout.java`

### 修改文件

- `src/main/java/org/example/GUI/MainGUI.java`
  - 注册「贡献分析」标签页（放在「对战模拟」之后）
  - 在 `updateDarkMode()` 中传播深色模式到 `ContributionTab`
- `src/main/java/org/example/Setting.java`
  - 将 `CompileForts(String fileName)` 的文本解析逻辑抽取为 `public static List<CompiledFort> parseForts(String content)`，原方法委托之
  - 解析时先剥离 `#HP` 后缀再校验长度（现有实现会把 HP 字符当作单位代码导致长度错误而跳过该阵型）

### 数据流

```
玩家阵型文本(1P) ──解析/编译──> 基线 CompiledFort ──> 基线胜率（对评判阵集 M 场）
评判阵集文本 ──Setting.parseForts──> List<CompiledFort>（2P 侧）
参数 (m, n, 线程数) ──枚举 C(N,n) 组合──> 采样 k 个 ──裁剪 CompiledFort──> 每组合 M 场
                                              ↓
                                   组合结果 + 单单位聚合 ──> 排序 ──> 表格展示
```

## 详细设计

### 1. 输入解析与验证

**玩家阵型（1P）**
- 输入文本按 `/` 分割，必须恰好 1 个非空段，否则报错「玩家阵型只能包含一个阵型」
- 用 `Setting.parseForts` 编译得到基线 `CompiledFort`（公开 API，不改模拟逻辑）
- 用 `Formation.decode` 取单位元数据：`units[0]` 为核心（不参与删除），`units[1..]` 与 `CompiledFort.type[0..]` 一一对应
- 实时显示解析结果：阵名、可删单位数 N、总军资金

**评判阵集（2P）**
- `Setting.parseForts(evalText)` 解析；解析成功数量实时显示；畸形条目由 `parseForts` 跳过并打印提示
- 结果为空则拒绝开始

**校验规则**
- `n` 上限联动为 N（1 ≤ n ≤ N）；越界禁止开始并提示
- 单位 `cost ≤ 0`（异常数据）：计算每费指标时按 1 兜底
- 评判阵集或玩家阵型解析失败：在输入面板下方显示错误文本，开始按钮不可用

### 2. 组合枚举与采样

- 可删单位数为 N，删除数为 n，组合总数 `C = C(N, n)`（long 计算，溢出时按极大值处理并提示）
- **m = 100%（或 k ≥ C）**：流式枚举全部组合
- **m < 100%**：`k = max(1, round(C × m / 100))`
  - 用按字典序排名的无重复随机抽样（Floyd 算法）直接生成 k 个组合，不枚举全量组合（无论 C 大小）
- 每次运行独立随机（不做固定种子）；状态行显示实际采样 `k / C`，重跑可换样本
- 组合按删除单位的原始索引升序表示；裁剪只过滤 SoA 数组，保留单位相对顺序与随机种子

### 3. 对战执行与调度

- 独立线程池（`Executors.newFixedThreadPool(threads)`），不占用/修改 `Main.pool`；线程数由参数面板控制（1~256，默认 `Main.MAX_THREADS`）
- 复用 `GameTask.run_single(f1, f2)`，每场对战新建 `GameTask` 实例（与 `Main.runAllBattles` 一致，线程安全）
- 阶段 1：基线 M 场，完成后立即回传基线胜率与进度
- 阶段 2：k × M 场；任务以滑动窗口提交（约 `max(64, 4 × 线程数)` 个在途任务），控制内存
- 进度 = 已完成场次 / 总场次 `(1 + k) × M`；ETA 用阶段 1 实测速度估算
- 取消：`volatile` 取消标志 + 停止提交新任务；已提交任务跑完即停，**不生成结果**（避免部分采样数据误导），状态栏提示「已取消」
- 单场对战抛异常：计入该组合的异常计数（相当于超时处理，不进胜率分母）；异常场次占比超过 10% 时状态栏提示

### 4. 指标与排序

- 胜率沿用项目公式（与 `Main.FortStats.winRate()` 一致）：
  `winRate = (2 × 胜 + 平) × 50 / (胜 + 负 + 平)`；超时/异常不计入分母；分母为 0 时显示 `—`
- **变化量 delta** = 删除后胜率 − 基线胜率（正数 = 删了反而变强）
- **每费变化**：组合 = delta / 删除单位军资金之和；单位汇总 = 该单位所有被采样组合的「组合每费变化」平均值（avg(delta/费用)，n=1 时等价于 avgDelta/该单位军资金）
- 排序维度 × 方向 4 种（analyzer 内部能力）：
  1. 胜率变化量 · 降序（最该删的排最前）
  2. 胜率变化量 · 升序（最能扛的排最前）
  3. 每费变化 · 降序
  4. 每费变化 · 升序
- GUI 不再暴露排序方式下拉：结果表通过 `JTable` + `TableRowSorter` 点击表头自由正/倒序排序，初始默认「胜率变化量 · 降序」

**组合结果**（每个采样组合一行）
- 删除单位（`名称(cost)@(x,y)` 用 ` + ` 连接）、删除军资金、删除后胜率、胜率变化、每费变化

**单位汇总**（每个被采样到的单位一行；仅统计本次采样命中的组合）
- 单位、军资金、平均胜率变化、平均每费变化

### 5. 界面设计

整体 `BorderLayout`：
- NORTH：`BoxLayout.Y_AXIS` 竖排「数据输入」+「参数」两个 `TitledBorder` 面板
- CENTER：「结果」面板
- SOUTH：进度条 + 状态行（阶段文字、已用时/ETA）

**数据输入面板**
- 玩家阵型：单行文本框 + `[从 1P.txt 导入]` 按钮 + 实时解析信息（阵名 / 可删单位数 / 总军资金）
- 评判阵集：大文本框（可粘贴多条，`/` 分隔）+ `[从 1P.txt 导入]` `[从 2P.txt 导入]` `[清空]` 按钮 + 实时「已解析 N 个阵型」
- 文件读取复用 `BattleTab.readFileAutoEncoding`
- 输入变化后 300ms 防抖刷新解析信息与规模预估

**参数面板**（可换行 `WrapLayout`，窄窗口自动增高）
- 探索率 m：`JSpinner` 0~100%（步 5）
- 删除数 n：`JSpinner` 1~N
- 线程数：`JSpinner` 1~256，默认 `Main.MAX_THREADS`
- 规模预估标签：「共 C 个组合，采样 k 个，预计 (k+1)×M 场对战」，规模过大时标红
- `[开始分析]` `[停止]` 按钮；分析中禁用输入与参数控件

**结果面板**
- 顶部信息行：基线胜率 / 基线战绩（胜-负-平-超时）/ 实际采样情况 / 总用时
- `JTabbedPane`：
  - 「组合排行」表：**仅 n ≥ 2 且本次有采样组合时显示**；显示时位于「单位汇总」之前（插到索引 0）
  - 「单位汇总」表：始终显示（n = 1 时两表等价，只显示汇总）
- 选中组合行 → `[复制新阵型代码]` 按钮；双击行直接复制；复制后状态栏提示

### 6. 复制新阵型代码

- 用 `Formation.decode` 解析出的单位列表做过滤（删除对应索引），`Formation.encode()` 重编码为 `name&code#hp`
- HP 后缀随单位删除自动重排（`Formation.encode` 内部按 3 个单位一组重算），保证粘贴回游戏可用
- 单位与核心的对应关系以 `Formation.units` 顺序为准（`units[0]` 核心）

### 7. 深色模式

- `JTable` 由 FlatLaf 自动适配
- 胜率涨/跌红绿配色准备深浅两套，由 `Main.DARK_MODE` 在渲染时决定
- `ContributionTab.updateDarkMode()` 由 `MainGUI` 统一调用（与本项目其他标签页一致）

### 8. 边界与错误处理

| 情况 | 行为 |
|------|------|
| m = 0% | 只跑基线，状态栏提示「未采样任何组合」 |
| n > N | 禁止开始，提示单位数不足 |
| 全部超时/异常 | 胜率显示 `—`，analyzer 内部排序 NaN 恒置底；GUI 默认降序时 NaN 置底，升序时置顶 |
| C 极大且 m = 100% | 规模预估标红警告；组合数超过 100 万时拒绝运行并提示（`ComboSelector.MAX_SAMPLES`，防止内存耗尽） |
| 分析中取消 | 不生成结果，状态栏提示「已取消」 |
| 输入为空/格式错误 | 开始按钮禁用，输入面板显示错误信息 |

## ContributionAnalyzer API 草案

```java
public final class ContributionAnalyzer {

    public enum SortKey { DELTA, PER_COST }       // 排序维度
    public enum SortOrder { DESC, ASC }           // 排序方向

    public record Params(int deleteCount, int explorationPercent,
                         int threads, SortKey sortKey, SortOrder order) {}

    public interface ProgressListener {
        void onProgress(int doneBattles, long totalBattles, String phase);
        // phase: "基线" / "组合"
    }

    public record UnitRef(int index, int type, int x, int y, int cost) {}

    public record ComboResult(int[] unitIndices, long removedCost,
                              int win, int lose, int draw, int timeout,
                              double winRate, double delta, double deltaPerCost) {}

    public record UnitSummary(UnitRef unit, int appearances,
                              double avgDelta, double avgDeltaPerCost) {}

    public record Report(int unitCount, long totalCost,
                         int baselineWin, int baselineLose,
                         int baselineDraw, int baselineTimeout,
                         double baselineWinRate,
                         int sampledCombos, long totalCombos,
                         List<ComboResult> combos, List<UnitSummary> units,
                         long elapsedMs) {}

    public static Report analyze(String playerText, String evalText, Params params,
                                 ProgressListener listener, BooleanSupplier cancelled) throws Exception;
}
```

说明：`Report` 内的列表在返回前已按 `params` 排序；GUI 的 `TableRowSorter` 在此基础上提供临时重排。

## 验证方式

项目无测试框架，采用以下方式验证：

1. **headless 临时测试类**（放系统临时目录编译运行，不入库）：
   - 构造小阵型 vs 4 个评判阵，覆盖 `n=1` 与 `n=2`、`m=100%`
   - 校验：基线胜率与直接调用 `GameTask.run_single` 的结果一致；各组合胜率与「重编码后重新编译」的对照结果一致；聚合数值自洽
2. **GUI 冒烟**：真实阵型跑 `n=1` / `n=2` 小样本，检查：
   - 表头点击正/倒序排序（初始按胜率变化量降序）
   - 窄窗口下参数面板自动换行增高，第二行完整可见
   - 复制新阵型代码并粘贴回工作台解析正常
   - 取消、进度、ETA、规模预估联动
   - 深色/浅色主题切换、n=1 时隐藏组合排行页签
3. **编译校验**：按 `AGENTS.md` 中的 javac 命令编译通过

## 约束

- 模拟引擎相关代码（`GameTask`、`elements/`、`Main.compileFort`、`Main.to_xyr`、`IntShapeMap`）不做任何修改
- 不自动 git commit，由用户手动提交
- 注释与 UI 文案使用中文；GUI 字体使用黑体

## 开放问题

无。
