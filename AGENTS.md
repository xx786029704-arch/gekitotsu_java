# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## 构建与运行

项目版本 v1.9.1，JDK 21，依赖 FlatLaf 3.5.4 + Jackson 2.18.3。Maven 不在 PATH 中，日常开发用 javac 直接编译；打包时需用 Maven 或手动构建 fat JAR（见下文）。

```
# 编译（需指定 FlatLaf + Jackson classpath，注意 GUI/effects/rust 子包）
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" -sourcepath src/main/java src/main/java/org/example/*.java src/main/java/org/example/elements/*.java src/main/java/org/example/elements/**/*.java src/main/java/org/example/GUI/*.java src/main/java/org/example/GUI/effects/*.java src/main/java/org/example/rust/*.java

# 运行 GUI（默认）
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\java" -cp "target/classes;C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" org.example.Main

# 运行 CLI
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\java" -cp "target/classes;C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" org.example.Main --cli
```

程序从工作目录读取 `1P.txt`、`2P.txt`、`config.ini`，输出 `result.txt` 和 `simple_result.txt`。

### Rust 模拟器（gkt-jni）构建与加载

对战模拟默认走 Rust 核心（`gekitotsu_rust` 的 `gkt-core`，JNI 本地库），启动时找不到原生库则回退内置 Java 引擎（BattleTab 右侧会显示当前引擎）。

```
# 构建并分发 gkt_jni.dll（默认 Rust 工程在 D:\program\gekitotsu_ultra_fast）
.\build_native.ps1
# 或指定 Rust 工程位置
.\build_native.ps1 -RustRepo <gekitotsu_rust 路径>
```

`gkt_jni.dll` 加载顺序（`org.example.rust.NativeLoader`）：系统属性 `-Dgekitotsu.native.path` → 环境变量 `GEKITOTSU_NATIVE_PATH` → 工作目录 `native/`、`./`、`rust/` → JAR/classes 同目录及 `native/` → classpath 资源 `/native/gkt_jni.dll`（fat JAR 内，解压到临时目录加载）。

注意：Rust 核心以修正版 oracle（`gekitotsu_java` 工作区：默认种子 1、LCG long 乘法）为语义基准，与旧内置 Java 引擎（种子 0、double 乘法）在 RNG 敏感对局上结果可能不同；批量与贡献分析均使用默认种子路径。

### 手动构建 fat JAR + EXE（Maven 不在 PATH 时）

```
# 1. 编译全部源码（同上）
# 2. 解压依赖 JAR 到临时目录，合并 target/classes + src/main/resources
# 3. jar cmf META-INF/MANIFEST.MF gekitotsu_java-1.9.1.jar .
# 4. jpackage 生成 EXE（复用已打包的 runtime）：
jpackage --type app-image --name "激突Kit" --app-version 1.9.1 \
  --input <jar-dir> --main-jar gekitotsu_java-1.9.1.jar \
  --main-class org.example.Main --icon assets/icon.ico \
  --runtime-image dist/激突Kit/runtime --dest dist
```

输出结构：
- `dist/input/gekitotsu_java-1.9.1.jar` — fat JAR
- `dist/激突Kit/激突Kit.exe` — jpackage 启动器（嵌入图标）
- `dist/激突Kit/app/gekitotsu_java-1.9.1.jar` — 运行时 JAR
- `dist/激突Kit/app/激突Kit.cfg` — jpackage 配置（含版本号）
- `dist/激突Kit/runtime/` — 捆绑的 JRE 21

`assets/icon.ico` 同时复制到 `src/main/resources/icon.ico`，使 GUI 运行时和 EXE 都能使用该图标。

## 项目性质

**激突Kit v1.8** — 激突要塞集成工具箱。在复刻 Flash 游戏「激突要塞」物理引擎的基础上，提供批量对战推演、Swing GUI 可视化管理、轨迹预测、阵型工作台（含效果插件系统）、单位图鉴、友情链接等功能，项目服务于高端硬核玩家群体。界面支持简体中文 / 日语 / 英语三语实时切换。

- GUI 框架：Java Swing + FlatLaf 3.5.4（现代 Look & Feel，支持深色/浅色主题切换、自定义主题色）
- 模拟引擎：零外部依赖，纯 `java.awt` 几何计算

## 核心架构

### GUI 层（开发重点）

**主框架：**
- **SplashWindow.java** — 启动开屏窗口。无边框半透明 `JWindow`：圆角卡片 + 软件图标 + 状态文字 + 循环进度条，在公式表与主界面加载期间立即显示以提供反馈；`Main.main()` 在 EDT 创建，`MainGUI` 的 `windowOpened` 时淡出关闭
- **MainGUI.java** — 主窗口。菜单栏（深色主题切换 + 语言切换 + 检查更新 + 退出）+ 6 个标签页容器 + 主题色快捷按钮 + 生命周期管理。`updateDarkMode()` 统一传播深色模式到各子标签页；「选项 → 语言」菜单切换后保存配置并重建主窗口。构造末尾调用 `UpdateChecker.checkOnStartup(this, this::shutdown)` 异步检查更新
- **BattleTab.java** — 对战控制标签页。设置面板 + 阵容编辑器（自动保存）+ 结果查看 + SwingWorker 批量模拟
- **BehaviorAnalysisTab.java** — 行为分析标签页（原「贡献分析」页）。上方为共享输入区，下方为分析项选择区（`JTabbedPane`，目前含「贡献分析」「对阵画像」「战场空间」）。详见下文「行为分析子系统」
- **CraftTab.java** — 阵型工作台标签页。三列布局 + 管线协调 + 工作流编排 + 效果插件系统 + Delete 键快捷删除节点
- **UnitDexTab.java** — 单位图鉴标签页。`null` layout 1/5-4/5 比例分割：左侧 63 个单位缩略图列表（32x32）+ 右侧详情面板。深色模式切换时重建列表条目
- **LinkTab.java** — 友情链接标签页。分类展示原作官网/国内社区/日本社区资源链接，悬停高亮 + 点击打开浏览器

**阵型工作台子系统：**
- **CraftTab.java** — 三列 `null` layout 比例布局（左 22% / 中 55% / 右 23%），内列再用 `null` layout 细分。负责阵容解析管线（`runPipeline()`）+ 工作流生命周期 + 跨面板选择协调
- **Formation.java** — 阵容数据对象。`encode()` 序列化为 `name&code` 格式，`encodeHp()`/`decodeHp()` 处理 HP 编解码；`getAccelLevel()`/`accelLevelOf(types)` 计算加速度等级（红加速器 +1、蓝加速器 +2）
- **Unit.java** — 单位数据模型（x/y/r/id/hp）+ `Unit.Info[]` 静态信息表（64 个单位：名称/cost/tech/hp/cd/at/shoot，shoot 为攻击前摇帧数）。`encode()`/`decode()` 使用 61 进制 pskey 字符表。实例方法 `isWall()` / `isWallLike()` / `isCore()` 分类。静态方法 `isWall(ID)` / `isWallLike(ID)` / `isCore(ID)` 供无实例时按 ID 判定。`getDelay()` 返回单位出手延迟。实例方法 `getQuickestXList(wallX, isFirst)` 和静态方法 `getQuickestXList(ID, wallX, isFirst)` 计算该单位在突击中最速出手的 x 坐标集
- **UnitInfoPanel.java** — 单位信息面板。`null` layout 绝对定位：文本标签（名称/HP/CD/AT/cost/坐标）+ 右上角贴图
- **SpritePanel.java** — 单位贴图绘制面板。锚点对齐（核心 60,60 / 兵玉 43,55），`coreSpriteScale`/`nonCoreSpriteScale` 静态缩放变量。自适应深色模式边框
- **UnitListEntryPanel.java** — 单位列表条目。点击回调（`Consumer<Integer>`）+ `setSelected()` 高亮切换
- **FortPreviewPanel.java** — 阵型预览面板。背景图 + 分层精灵渲染（类要塞壁底层 → 非壁上层），含背景色取色器
- **FormulaTable.java** — 要塞运动公式表。从 `formula.json` 加载分段二次方程，二分查找段 → 计算 x(t)
- **ExprEvaluator.java** — 整数表达式求值器。支持变量 + `+`/`-`/`*`/`/` 运算

**工作流子系统：**
- **WorkflowGraph.java** — 有向图（当前线性链式，预留分支/循环结构）。Effect 节点编排 + 执行顺序管理。`execute()` 先调用 `validate()` 校验参数，再 try/catch 执行，出错记录到 `node.error`
- **WorkflowNode.java** — 工作流节点。持有 Effect 实例 + 运行时参数（`Map<String, Object>`）+ 启用/禁用状态 + `error` 字段
- **NodeComponent.java** — 节点 UI 组件。悬停/选中/禁用/错误背景色 + 错误时红色边框+⚠图标+tooltip + 双击编辑参数 + 单击效果开关（●/× 切换）+ 长按拖拽排序 + 可获取键盘焦点（支持 Delete 键删除）
- **WorkflowDropPanel.java** — 工作流面板。`BoxLayout.Y_AXIS` + 落点指示线绘制
- **EffectRegistry.java** — Effect 注册表。内置效果硬编码加载 + `PluginLoader` 扫描 `effects/*.jar` 通过 `ServiceLoader` 自注册
- **EffectLibraryPanel.java** — 效果库面板。搜索框 + 双击添加 + 拖拽添加，通过 `Consumer<Effect>` 回调通信
- **Effect.java** — 效果接口。`getName()`/`getDescription()`/`getAuthor()`/`getVersion()`/`getParameters()`/`validate()`/`execute()`。`execute()` 可直接修改传入的 Formation 对象
- **EffectParameter.java** — 效果参数定义。支持 INT/STRING/BOOLEAN/UNIT_ID 四种类型。BOOLEAN 在 UI 中渲染为 JCheckBox
- **effects/** 目录 — 内置效果 + 插件 JAR 存放目录。`build_plugin.bat` 编译打包工具（纯 ASCII，拖放 .java 文件即可编译为插件 JAR）；文件名以 `_` 开头的 JAR 默认不加载（示例插件 `_CopyAndRenameEffect.jar` 由此默认隐藏，去掉前缀即启用）
- **PluginLoader.java** — 插件扫描器。扫描 `effects/*.jar`（跳过 `_` 前缀），通过 `ServiceLoader` 发现 Effect 实现并注册到 EffectRegistry
- **effects/samples/** — 示例插件：`CopyAndRenameEffect.java`（重影幻视），演示全部四种参数类型 + validate + ServiceLoader 注册；`README.md` 为自定义 Effect 开发指南（含项目 GitHub 链接）

**轨迹预测子系统：**
- **TraceTab.java** — 轨迹预测标签页。轨迹/要塞壁/变量列表管理 + 编辑对话框 + 色板 + 拖拽排序 + 多选 + 键盘快捷键（Delete/Ctrl+CVXAD）+ 剪贴板序列化
- **TraceCanvas.java** — 轨迹画布。滚轮缩放（0.1x–5.0x，以鼠标为锚点）+ 拖拽平移 + 网格/要塞车/轨迹渲染
- **TraceItemPanel.java** — 轨迹列表条目。色块 + 名称 + 悬停/按压/选中状态，回调模式通信
- **Trace.java** / **TraceWall.java** / **Variable.java** — 数据模型。`ListItem` 接口 + 表达式字符串字段（通过 ExprEvaluator 求值）+ 人类可读写剪贴板序列化

**阵容分析子系统：**
- **AnalysisPanel.java** — 分析面板。工作流模式：`setUnits()` → `refresh()` 依次调用各检测方法，当前含 `checkOverlap()`（同种兵玉重叠/过近检测）和 `checkAssaultQuickest()`（突击最速行动检测）。每项检测为独立方法，新增检测只需写方法然后注册到 `refresh()`
- **FormationStabilizer.java** — 阵容稳定化器。模拟游戏第一帧 `Ball.land()` 碰撞，将因卡墙而被挤压的兵玉坐标还原到真实位置。内部复刻了 Wall/Core/Base 三种 `hitTestPoint` 形状，以 `CompositeShape` 模式做传感器上移检测。`stabilize(units)` → 返回调整后的单位列表。仅供分析使用，不影响输出编码
- **AssaultDetector.java** — 突击检测器。`detect(units)` 找到所有突击壁（Far=56/Near=55）并检测其 HitsJump 形状内的兵玉，返回 `AssaultGroup` 列表。冲突解决：Far 优先于 Near ；Near 冲突取代码靠前；Far 冲突取 `wall.x + (unitIdx>wallIndex?1:0)` 较大者。`checkQuickestX(groups)` 检测每组中每种兵玉是否采用最速 x 坐标，未采用则生成建议。`isInJumpRange(unit, wall)` 复刻 HitsJump 的 Wall 形判定（中心 `(wall.x, wall.y-35)`，x 偏移 +0.3）
- **AssaultGroup.java** — 突击组数据类。`isFar`（true=远突击）、`wallX/wallY/wallIndex`、`unitsBefore/unitsAfter`（代码顺序在壁之前/之后的兵玉）。`mergeNear(other)` 合并两个 Near 组

**行为分析子系统（`BehaviorAnalysisTab` 页）：**
- **BehaviorInputPanel.java** — 共享输入区。上方 `JSplitPane` 左右分栏（默认 1:1，可拖动）：左侧「原始阵型」（单个阵型，换行自动去除）、右侧「测试阵集」（多阵型，`/` 分隔）；各含导入/清空按钮与信息行。文本变化经 300ms 防抖后刷新信息行并通知分析项；需要即时内容时用 `snapshot()` 同步读取；`setLocked(boolean)` 在分析期间锁定输入
- **BehaviorInput.java** — 输入快照 record（`playerText`/`evalText`，构造时 trim，原始阵型去换行）
- **BehaviorAnalysisPanel.java** — 分析项基类（继承 JPanel）。子类实现 `titleKey()`（标签词条键）与 `onInputChanged(BehaviorInput)`；`currentInput()` 同步读取最新输入（启动分析时用）；`setInputBusy(boolean)` 锁定共享输入；只持有 `Supplier`/`Consumer` 回调，不持有父组件引用
- **新增分析项步骤**：继承 `BehaviorAnalysisPanel`（构造中 `setLayout`），实现上述两个抽象方法，然后在 `BehaviorAnalysisTab` 构造函数中调用 `addItem(new XxxAnalysisPanel(inputPanel::snapshot, inputPanel::setLocked))`；标题词条须在 lang.json 三语中同步添加
- **SplitRatio.java** — 包内工具：JSplitPane 首次显示时按比例设置分隔线（组件未显示时宽高为 0，按比例定位无效，故延后到首次显示生效一次）

**贡献分析子系统：**
- **ContributionAnalyzer.java** — 核心逻辑。解析输入、组合枚举/采样、独立线程池滑动窗口批量对战、聚合与排序、复制代码生成。不修改模拟引擎
- **ComboSelector.java** — 组合枚举与采样。C(N,n) 计算、字典序排名/反解、Floyd 算法随机采样、全遍历；组合数上限 `MAX_SAMPLES`（100 万）
- **FortTrimmer.java** — 从 CompiledFort 过滤掉指定索引的单位生成新阵型（单位顺序、随机种子保持不变）
- **ContributionAnalysisPanel.java** — 贡献分析分析项（「行为分析」页的子功能，继承 `BehaviorAnalysisPanel`）。`JSplitPane` 左侧配置列（约 1/4 宽，`resizeWeight=0.25`，最小宽 180；竖向行布局 + 信息文本）、右侧结果列（3/4 宽，组合排行 / 单位汇总），中间分隔线可拖动；探索率 m（0~100%）、删除数 n、线程数；结果表点击表头排序（初始按胜率变化量降序）；基线先显示、滑动窗口进度、ETA、可取消；n=1 时只显示单位汇总页签；组合数超上限时禁止开始；启动分析时用 `currentInput()` 读取最新输入

**对阵画像子系统（「行为分析」页的子功能，仅结果级数据）：**
- **MatchupAnalyzer.java** — 核心逻辑。解析输入、批量跑「原始阵型 × 测试阵集」、聚合逐局结果、单位敏感度与阵集元数据（核心位置、构成）。优先 Rust 批量（按 256 局分片以便取消），不可用时回退 Java 线程池；不采集遥测。核心坐标换算为阵型工作台标准（原始码坐标 − 52/−58，0..276）；单位敏感度对低样本做收缩：两组胜率先各自向 50% 收缩（伪计数 4 局）再相减，得到 `adjustedDelta` 供排名；加速度相性按对手阵型的加速度等级分组（口径同 `Formation.getAccelLevel`：红加速器 +1、蓝加速器 +2）。Keener 修正（可选，`estimateStrength`）：对测试阵集内部跑循环赛（每对双方向各一局，即两局；全量超过对战预算 `STRENGTH_MAX_BATTLES` 160000 时用固定种子（`Random(1)`）按规则圆环图确定性采样——取 K 个随机偏移且固定含偏移 1 保证连通，每个节点度数恰为 2K，避免旧贪心采样度数不均（7..49）导致评分被 +1 基线系统抬高（度数与评分相关系数 0.94））。阵集 Keener 矩阵取每对双局聚合得分 `sIJ + 1 − sJI`（胜两局 2 / 各胜一局 1 / 全负 0，与用户的 Excel 特征向量实验口径一致）再加 1 基线（`A_ij = 聚合得分 + 1`，Keener 的 +1：否则全量对局的胜者矩阵可约——小阵集实测出现零分量；+1 对排名扰动很小，对 200 全量 Spearman 0.9975）、未对局配对取 `KEENER_EPS`（1e-6），`perronVector()` 幂迭代求 Perron–Frobenius 主特征向量作为对手强度权重 w。修正胜率 = 按对手权重加权平均每局得分率 `ρ_i = Σ_{k≠i} w_k·M_ik / (2·Σ_{k≠i} w_k)`（0.5 = 阵集平均，加权均值恰为 0.5；排名与按 w 排序一致）。我方复用阶段一全部对局、按同一权重加权平均（`ρ_t = Σ w·得分 / Σ w`），不新增对战、也不作为矩阵节点（采样路径下阵集节点平均只打 d 局而我方打满 m 局，基线会随对局数把我方系统性推到榜首）。输出含我方在内的名次、实际胜率与修正胜率（`correctedRate`；`delta = 修正 − 实际` 即对手强度构成的修正量），逐对手给出其修正胜率，均只暴露胜率口径、不展示强度分。循环赛在 Java 线程池上用 `RustBattle.runSingle`（回退 `GameTask`）并行执行，避免逐行批量的长尾串行；阵集权重与修正胜率的持久缓存 `StrengthCache`：键为「格式版本 + 引擎 + 帧上限 + 阵集代码」的 SHA-256 十六进制，值写入工作目录 `strength_cache.bin`（魔数+版本+两个数组），进程重启不失效；内存与文件均按最近最少使用（LRU）上限 32 份自动淘汰最久未用条目（与原始阵型无关，可跨分析复用），同阵集换原始阵型时第二轮秒回（命中缓存时跳过循环赛，进度总量只含阶段一，也不发「Keener 修正计算中」阶段提示），文件损坏或版本不符时按空缓存处理；阵集 < 6 或未启用时 `strength` 为 null。由于单局结果另有对局级缓存（见 `BattleCache`），阵集增删一个阵型时只需补跑缺失对局（实测 200→199 阵集从约 100 秒降到 0.3 秒）
- **MatchupProfilePanel.java** — GUI。配置列（线程数、平滑半径、「启用 Keener 修正」勾选，默认开）+ 结果列（约 1/4 / 3/4 分隔）；结果含「核心位置」「单位敏感度」「节奏与赢面」「加速度相性」「Keener 修正」五页签；信息行显示含循环赛的总场次；Keener 修正页为两行说明与指导意义 + 三项指标（阵集排名、实际胜率（含局数）、修正胜率（按 Keener 权重加权平均的每局得分率，50% = 阵集平均）后附变化量：修正 − 实际，增加绿字/降低红字）+ `StrengthChart`（不展示强度分，统一用胜率口径），未启用或阵集过小时给出提示；敏感度表默认按修正差值升序、出现次数 < 3 的行淡化显示；分析期间锁定共享输入；`BehaviorAnalysisPanel` 子类，接入方式同贡献分析
- **CoreHeatmapPanel.java** — 对手核心位置胜率热力图（平滑版）。坐标 0..276（工作台标准），每个样本在核心位置贡献高斯核形成连续胜率场，红-黄-绿渐变、透明度随样本密度增加；真实样本点用小圆点标记（绿=胜、红=负、灰=平/超时）；横轴刻度在图像上方（与纵轴共用左上原点），最大值 276 额外标注；悬停显示坐标、估计胜率与附近样本
- **MatchupCharts.java** — 自绘图表：「节奏与赢面」页左侧上下两幅——时长分布与胜率走势（左轴局数堆叠柱 + 右轴胜率折线，无端点圆点）、赢面分布（获胜方剩余 HP）；右侧整栏为汇总统计。「加速度相性」页为按对手加速度等级的胜率柱状图（柱高=胜率、柱顶标注百分数、柱下标注 胜/负/平 局数、50% 虚线基准、红→黄→绿配色，悬停显示对局/胜负平明细）。「Keener 修正」页为对手修正胜率散点图：横轴为对手修正胜率（按 Keener 权重加权平均的每局得分率，0.5 = 阵集平均），纵轴为对位结果（自下而上 负/平/胜 三条网格线，红/灰/绿圆点），虚线标出我方修正胜率、菱形标出我方实际胜率；悬停优先显示离鼠标最近的圆点，显示对手名称/修正胜率/排名/结果。超时与异常不计入图表，悬停显示分箱统计；配色随深色模式

**战场空间子系统（「行为分析」页的子功能，遥测级数据，需要 Rust 模拟器）：**
- **BattlefieldAnalyzer.java** — 两阶段分析：先跑「原始阵型 × 测试阵集」结果并按筛选（全部/仅胜/仅负/仅平/仅超时）取子集，再对子集调用 Rust 遥测聚合。槽位坐标 = 工作台标准（兵玉 raw−16/−20、核心 raw−52/−58）；兵玉与要塞壁均计入槽位受创/阵亡；遥测无 Java 回退
- **BattlefieldPanel.java** — GUI。配置列（线程数、时间线采样帧、筛选）+ 结果三页签：「阵型受创」（气泡/平滑两种呈现、受创/阵亡两种指标）、「推进曲线」（平均/胜/负/其他系列可勾选，默认开启平均/胜/负）、「开局压迫」；分析期间锁定共享输入
- **SlotHeatmapPanel.java** — 阵型受创视图（左右两幅，均带工作台坐标系：原点为 (0,0) 兵玉的绘制位置，即画布左上角偏置 (36,36)；视图范围按数据范围对称外扩 x ∈ [−36, 384]、y ∈ [−36, 385]，画布（`Bg/bg.png` 420×470）按此范围绘制、底部车板下缘被裁掉；横轴在上方与纵轴共用左上原点、每 50 网格、标注 0/50/…/300 与上限 348/349）。左幅仅阵型图（bg.png + 单位贴图；图层顺序与阵型工作台一致——核心与要塞壁在下、兵玉在上，车板盖住网格）；右幅仅热力图（气泡或平滑场；平滑场用加窗高斯核双通道——颜色 = 核加权平均指标（按单槽上限归一，密集低伤不会因叠加偏红）、透明度 = 叠加密度（sqrt 映射，核在边缘连续归零），右侧绘图区外的竖向色带显示 0..最大单槽值）；核心不计入槽位统计且不参与悬停提示（数值远大于兵玉、会淹没差异）；悬停分别显示槽位名/坐标（左）与受创/阵亡两项指标（右）
- **BattlefieldCharts.java** — 自绘图表：推进曲线（我方/对方 HP、存活、弹幕随时间按胜/负/其他分组平均，横轴按最大对局时长动态收缩）、开局压迫分布（首次接触帧、双方核心首次受击帧，含"未发生"计数）
- **gkt-jni telemetry.rs** — 遥测聚合（Rust 侧，core 零改动）：逐局观察玩家兵玉与要塞壁，按出生坐标精确匹配阵型槽位（兵玉传感器不含墙类单位、顺序也与代码不一致，必须按坐标匹配；wall 传感器混有车板 Base，按 `Element::Wall` 过滤）；阵亡单位会从传感器移除，须遍历自身记录表；聚合受创/阵亡（核心不计入槽位；兵玉与要塞壁均计入，时间线「存活」保持兵玉口径；阵亡仅计核心被毁前的战斗死亡，清场死亡不计入以免全队统一偏移；无贽玉的撞击帧 `dokkan_flg` 内每帧 1 点擦伤整帧不计入受创，有贽玉时引擎本就跳过撞击伤害）、时间线（双方 HP/存活/弹幕，每箱采样一次）与逐局标量（首次接触、首次核心受击、伤害/阵亡统计；负载：header + 标量×10 + 时间线 + 槽位×2）

**单位图鉴子系统：**
- **UnitDexTab.java** — 单位图鉴主标签页。`null` layout + `doLayout()` 比例布局（左 1/5 + 右 4/5）。左侧为 `BoxLayout.Y_AXIS` 单位列表 + `JScrollPane`，右侧为 `UnitDexDetailPanel`。JSON 加载用 Jackson 按当前语言从 `unit_details_zh/ja/en.json` 读取。全局 AWT 点击监听取消选择
- **UnitDexEntryPanel.java** — 单位列表条目组件。32x32 `ThumbnailPanel` 缩略图 + 单位名称，`CompoundBorder` 选中高亮（主题色 2px）。贴图加载根据 `Unit.isCore(id)` 使用 `SpritePanel.coreSpriteScale`/`nonCoreSpriteScale` 计算有效视觉尺寸后等比缩放，核心单位缩略图按 0.5x 有效比例补偿
- **UnitDexDetailPanel.java** — 单位详情面板。`null` layout + `doLayout()`：左侧 `SpritePanel`（100x100）+ 右侧双列（名称/编码/流式属性标签 + 最速行动计算面板）+ 底部 JSON 扩展区。`TitledBorder` 颜色在 `updateColors()` 中随深色模式切换
- **unit_details_zh.json / unit_details_ja.json / unit_details_en.json** — 单位详情三语 JSON 数据文件（`src/main/resources/`），各含 63 个条目（id 0-62），每项含 `id`/`description`/`tactics`/`notes` 字段。运行时由 `UnitDexTab` 按当前语言加载并传入 `UnitDexDetailPanel`，供后续补全单位介绍文案

**最速行动计算（UnitDexDetailPanel 子功能）：**
- 显示条件：`cd >= 0 && shoot >= 0 && !Unit.isWallLike(id)`，不符合条件则隐藏整个面板
- 输入：`wallXField`（突击壁 x 坐标，`DocumentListener` 自动触发）+ `isFirstCheck`（"兵玉代码在前"，`ActionListener` 自动触发）
- 计算：调用 `Unit.getQuickestXList(ID, wallX, isFirst)`，结果显示在 `quickestResultArea`（只读 JTextArea）
- 布局：`BoxLayout.Y_AXIS`，内部子行使用 `FlowLayout.LEFT` 确保左对齐

**更新检查子系统：**
- **UpdateChecker.java**（`org.example`）— 基于 GitHub Releases 的更新检查。请求 `api.github.com/repos/xx786029704-arch/gekitotsu_java/releases/latest`（`java.net.http.HttpClient`，连接 5s / 请求 8s 超时），解析 `tag_name`（自动去 v 前缀，数字段比较）与 assets（zip 附件优先）；后台线程执行、失败静默；启动时每次进程只检查一次（语言切换重建主窗口不重复），发现新版本且未被跳过时延迟 1.5s 弹窗；「选项 → 检查更新」手动检查（无论是否跳过都显示结果，检查期间菜单项禁用并显示「正在检查更新…」）；弹窗含「立即更新」（app-image 环境可用）或「前往下载」（回退，优先打开 zip 附件下载地址否则打开 Release 页面）/「稍后再说」+「不再提示此版本」勾选（版本号存 config.ini 的 `SKIP_UPDATE_VERSION`，更高版本仍会提示）
- **UpdateInstaller.java**（`org.example`）— 全自动更新安装器。定位安装目录（`jpackage.app-path` 属性，回退 fat JAR 位于 `<app-image>/app/` 反推；要求目录含 `app/`、可写且不在系统临时目录，否则不可用、按钮回退「前往下载」）；模态进度对话框下载 zip（可取消，10 分钟超时）→ 解压校验（兼容一层顶层目录、防 Zip Slip、必须含 `app/`）→ 生成 UTF-8 BOM 的 PowerShell 延迟替换脚本（等待主进程 PID 退出 → robocopy 合并覆盖安装目录（不删用户文件、失败重试 30 次）→ 重启新版 exe → 清理临时文件与脚本）→ 启动脚本后由传入的 exitAction 退出程序；下载/解压失败时提示并回退打开浏览器

**通用组件：**
- **ColorPicker.java** — HSB/RGB 取色器对话框。`ColorPicker.showDialog(parent, initial, darkMode, showReset)` 静态方法
- **WrapLayout.java** — 可换行 FlowLayout。按容器宽度计算多行高度并自动增高容器，解决窄窗口下参数行被裁切的问题
- **FixedJTextArea.java** / **FixedTextAreaUI.java** / **FixedWrappedPlainView.java** — 修复 JDK `WrappedPlainView.viewToModel` 中 `round=false` 的光标向下取整问题

**国际化子系统：**
- **I18n.java**（`org.example`）— 三语支持核心。`t(key[, args])` 取词条并 String.format（缺失回退中文→key）；`pick(map, key[, def])` 剪贴板字段名跨语言解析；`unitName(id)` 单位名；`font(style, size)` / `fontFamily()` 按语言返回字体；`detectSystemLang()` 按系统 Locale 推断语言
- **`src/main/resources/lang.json`** — 全部界面文案三语词条，结构 `{ "zh": {key: text}, "ja": {...}, "en": {...} }`，键名语义化点分命名（如 `battle.start`、`unit.42.name`）
- 语言存于 `Main.LANGUAGE`（config.ini 的 `LANGUAGE` 键，取值 zh/ja/en），`Setting.loadConfig()` 中设置；未配置时按系统语言推断
- 剪贴板人类可读序列化（Trace/TraceWall/Variable）字段名随语言翻译，`fromHumanReadable` 用 `I18n.pick` 兼容三种语言的字段名
- 内置效果（`GUI/effects/*`）的名称/描述/参数名一律走 `I18n.t`
- 新增界面文案时必须同步在 lang.json 的三个语言块中添加词条

GUI 字体统一通过 `I18n.font(...)` 创建（中文 黑体 / 日文 Meiryo / 英文 Segoe UI），样式与字号语义不变；禁止硬编码 `new Font("黑体", ...)`。UI 文案与注释使用中文，但 UI 文案一律经 `I18n.t` 输出，不得硬编码。
应用图标位于 `assets/icon.ico`，运行时从 classpath `/icon.ico` 加载。

### GUI 架构模式

- **面板提取**：可复用子面板从主标签页中拆分为独立类，通过 `Consumer<T>` / `Runnable` 回调与父组件通信，不储存父组件引用。深色模式通过 `Main.DARK_MODE` 静态字段在构造/绘制时读取。示例：`EffectLibraryPanel(Consumer<Effect>)`、`UnitListEntryPanel(Unit, int, Consumer<Integer>)`
- **比例布局**：使用 `null` layout + `ComponentListener.componentResized()` 中按百分比 `setBounds()`，确保布局稳定不受子组件 preferred size 影响。应用于 CraftTab 的三列及各内列面板。对于需要随内容变化重新布局的面板（如标签文本改变后高度变化），使用覆写 `doLayout()` 替代 `ComponentListener`，配合 `revalidate()` 触发布局更新
- **深色模式**：`Main.DARK_MODE` 全局静态字段，默认深色（`Setting.loadConfig()` 缺省 `DARK_MODE=true`）。各面板的 `updateDarkMode()` 或 `refresh()` 方法在 `MainGUI` 中统一调用

### 对战引擎（稳定，勿改）

默认由 Rust 核心（`org.example.rust.RustBattle`，见上文 gkt-jni）执行；原生库不可用时回退到内置 Java 引擎。

- **GameTask.java** — 游戏循环：`base_move()` → `judge()` → `update()`，直到一方 HP 归零或达到帧数上限
- **Main.runAllBattles()** — 1P×2P 全对阵批量模拟（Rust `runBatch`，回退时 Java 线程池并行），输出 `FortStats` 统计；原始阵型由 `Main.p1Raws/p2Raws` 提供代码给 Rust
- **org.example.rust.RustBattle / NativeLoader** — JNI 包装与 `gkt_jni.dll` 定位加载；`available()` 探测 + `unavailableReason()` 供 GUI 展示。`runSingle` 与 `runBatch` 均接入对局级持久缓存 `BattleCache`（`runBatch` 全命中直接返回、全未命中走原生批量、部分命中只补跑缺失对局），引擎版本变化自动失效；Java 回退路径（`GameTask`）经 `BattleCache.cached("java", ...)` 同样缓存
- **BattleCache.java** — 对局级持久缓存（`org.example`）。键 = SHA-256(引擎 + 帧上限 + 1P 代码 + 2P 代码) 截断 128 位，值 = 单局结果（状态 / 剩余 HP / 帧数 / 耗时，状态 -2 异常不缓存）；工作目录 `battle_cache.bin` 为带 CRC 的 36 字节追加日志（半写记录与校验失败记录自动跳过），内存 LRU 上限 20 万条，文件超过 16 MB 时压缩重写为当前条目；各分析共用，阵集增删只需补跑缺失对局
- **ContributionAnalyzer** — Rust 路径用 `FortTrimmer.trimCode` 做代码级裁剪并逐局调用 `RustBattle.runSingle`；回退路径仍用 `FortTrimmer.trim` + Java `GameTask`
- 元素层级：`Shape` → `Ball`（60+ 兵玉）/ `Wall` / `Bullet` / `HitSystem`
- 数据分离：敌我双方元素归入 `unit[0..1]`、`atk[0..1]`、`wall[0..1]` 等容器，命中检测直查对方容器
- **IntShapeMap** — 自定义 int→Shape 哈希表，开放寻址 + 斐波那契哈希 + 墓碑删除，缓存友好，不可替换为 HashMap
- **CompiledFort** — SoA（结构数组）布局的预编译阵容数据（`int[] type, x, y, r, seed`）

### 阵容编码

61 进制（`pskey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"` 字符表），`name&code` 格式。`Main.compileFort()` 解析，`to_xyr()` 解码坐标。HP 编解码见 `Formation.encodeHp()`/`Formation.decodeHp()`。

### 文件编码

`1P.txt`/`2P.txt` 可能是 UTF-8 或 GBK。`Setting.readUtf8()` 自动检测 BOM 并读取。输出统一 UTF-8。GUI 文本读取共享同一检测逻辑。`config.ini` 保存主题、线程数、帧数上限与 `LANGUAGE`（zh/ja/en）等设置。

## 不要修改的部分

- **GameTask.java** 及 `elements/` 下所有模拟逻辑（units、atk、hit、wall）—— 原封不动复刻 Flash 引擎，修改会导致模拟结果偏差
- **Main.compileFort()** 和 **Main.to_xyr()** — 阵容解码算法
- **IntShapeMap** — 自定义数据结构，内聚于模拟引擎性能
- **FormulaTable.java** — 公式表数据加载与计算逻辑

## 碰撞判定体系（知识注解，供分析器参考）

游戏第一帧的核心碰撞逻辑在 `Ball.land()`：
1. `ys += 1; drop_y = y + ys`（重力）
2. 若 `drop_y >= 566` → 地面状态，跳过墙壁碰撞
3. `while (wall[side].hitTestPoint(x, drop_y + 15)) { drop_y -= 1; }` — 底部传感器逐像素上移直到脱离所有墙壁
4. `y = drop_y; ySync()` — 量子化到 0.05 精度

`wall[side]` 为 `CompositeShape`，内含三类碰撞形状：
| 形状 | 来源 | 关键参数 |
|------|------|---------|
| 标准墙壁 | `Wall.hitTestPoint` | AABB [-16.85, 17.5]×[-17.5, 17.5]，四角圆角 r²=16 |
| 核心 | `Core.hitTestPoint` | 复合八边形+圆形 |
| 车板 | `Base.hitTestPoint` | AABB [-191.5, 191.5]×[-15.5, 51.5] |

坐标变换（side 0）：兵玉/墙壁 `gameXY = (unit.x+66, unit.y+152)`，核心 `gameXY = (unit.x+102, unit.y+190)`。

突击（jump）机制：
- `jump_flg` 状态：0=正常，1=突击中，2=地面
- Near（近突击壁 id=55）→ `jump_u`，Far（远突击壁 id=56）→ `jump_f`，Near 优先（if/else if）
- `HitsJump`：创建于 `(wall.x, wall.y-35)`，持续 1 帧，判定形状与 Wall 完全一致（x 偏移 +0.3）
- 兵玉中心命中 HitsJump → `jump_flg=1`，获得速度矢量，翻转 `on_side`

## 编码偏好

- 注释使用中文
- UI 文案不得硬编码，一律通过 `I18n.t(key)` 输出，并在 `lang.json` 中同步维护三语词条
- GUI 字体通过 `I18n.font(...)` 创建，不硬编码 `黑体`
- 修改代码后不自动 git commit，由用户手动提交
- 可复用的 UI 组件优先抽取为独立类（如 ColorPicker、SpritePanel、UnitInfoPanel）
- 固定比例的面板布局使用 `null` layout + `ComponentListener` 模式，不使用 `GridBagLayout`（避免 preferred size 引起的布局跳动）

## 翻译术语表（三语统一）

| 中文 | 日语 | 英语 |
|------|------|------|
| 兵玉 | 玉 | Ball |
| 突击 / 突击壁 | 乗り込み / 乗り込み壁 | assault / assault wall |
| 要塞壁 | 要塞壁 | wall |
| 核心 | コア | core |
| 阵型 / 阵容 | 要塞 | fortress |
| 对战 | 対戦 | battle |
| 最速行动 | 最速行動 | quickest action |
| 军资金 | 軍資金 | cost |
| 工作台 | ワークベンチ | workbench |
| 轨迹预测 | 軌道予測 | trajectory prediction |
| 单位图鉴 | ユニット図鑑 | unit dex |
| 行为分析 | 行動分析 | behavior analysis |
| 对阵画像 | 対戦プロファイル | matchup profile |
| 战场空间 | 戦場空間 | battlefield space |
| 贡献分析 | 貢献分析 | contribution analysis |
