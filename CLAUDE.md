# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建与运行

项目版本 v1.6.0，JDK 21，依赖 FlatLaf 3.5.4 + Jackson 2.18.3。Maven 不在 PATH 中，日常开发用 javac 直接编译；打包时需用 Maven 或手动构建 fat JAR（见下文）。

```
# 编译（需指定 FlatLaf + Jackson classpath，注意 GUI/effects 子包）
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" -sourcepath src/main/java src/main/java/org/example/*.java src/main/java/org/example/elements/*.java src/main/java/org/example/elements/**/*.java src/main/java/org/example/GUI/*.java src/main/java/org/example/GUI/effects/*.java

# 运行 GUI（默认）
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\java" -cp "target/classes;C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" org.example.Main

# 运行 CLI
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\java" -cp "target/classes;C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" org.example.Main --cli
```

程序从工作目录读取 `1P.txt`、`2P.txt`、`config.ini`，输出 `result.txt` 和 `simple_result.txt`。

### 手动构建 fat JAR + EXE（Maven 不在 PATH 时）

```
# 1. 编译全部源码（同上）
# 2. 解压依赖 JAR 到临时目录，合并 target/classes + src/main/resources
# 3. jar cmf META-INF/MANIFEST.MF gekitotsu_java-1.6.0.jar .
# 4. jpackage 生成 EXE（复用已打包的 runtime）：
jpackage --type app-image --name "激突Kit" --app-version 1.6.0 \
  --input <jar-dir> --main-jar gekitotsu_java-1.6.0.jar \
  --main-class org.example.Main --icon assets/icon.ico \
  --runtime-image dist/激突Kit/runtime --dest dist
```

输出结构：
- `dist/input/gekitotsu_java-1.6.0.jar` — fat JAR
- `dist/激突Kit/激突Kit.exe` — jpackage 启动器（嵌入图标）
- `dist/激突Kit/app/gekitotsu_java-1.6.0.jar` — 运行时 JAR
- `dist/激突Kit/app/激突Kit.cfg` — jpackage 配置（含版本号）
- `dist/激突Kit/runtime/` — 捆绑的 JRE 21

`assets/icon.ico` 同时复制到 `src/main/resources/icon.ico`，使 GUI 运行时和 EXE 都能使用该图标。

## 项目性质

**激突Kit v1.6** — 激突要塞集成工具箱。在复刻 Flash 游戏「激突要塞」物理引擎的基础上，提供批量对战推演、Swing GUI 可视化管理、轨迹预测、阵型工作台、单位图鉴等功能，项目服务于高端硬核玩家群体。阵型工作台（CraftTab）已完成核心功能，单位图鉴（UnitDexTab）为最近新增标签页。

- GUI 框架：Java Swing + FlatLaf 3.5.4（现代 Look & Feel，支持深色/浅色主题切换、自定义主题色）
- 模拟引擎：零外部依赖，纯 `java.awt` 几何计算

## 核心架构

### GUI 层（开发重点）

**主框架：**
- **MainGUI.java** — 主窗口。菜单栏（深色主题切换 + 退出）+ 4 个标签页容器 + 主题色快捷按钮 + 生命周期管理。`updateDarkMode()` 统一传播深色模式到各子标签页
- **BattleTab.java** — 对战控制标签页。设置面板 + 阵容编辑器（自动保存）+ 结果查看 + SwingWorker 批量模拟
- **CraftTab.java** — 阵型工作台标签页（~640 行）。三列布局 + 管线协调 + 工作流编排。已完成核心功能
- **UnitDexTab.java** — 单位图鉴标签页。`null` layout 1/5-4/5 比例分割：左侧 63 个单位缩略图列表（32x32）+ 右侧详情面板。深色模式切换时重建列表条目

**阵型工作台子系统：**
- **CraftTab.java** — 三列 `null` layout 比例布局（左 22% / 中 55% / 右 23%），内列再用 `null` layout 细分。负责阵容解析管线（`runPipeline()`）+ 工作流生命周期 + 跨面板选择协调
- **Formation.java** — 阵容数据对象。`encode()` 序列化为 `name&code` 格式，`encodeHp()`/`decodeHp()` 处理 HP 编解码
- **Unit.java** — 单位数据模型（x/y/r/id/hp）+ `Unit.Info[]` 静态信息表（64 个单位：名称/cost/tech/hp/cd/at/shoot，shoot 为攻击前摇帧数）。`encode()`/`decode()` 使用 61 进制 pskey 字符表。实例方法 `isWall()` / `isWallLike()` / `isCore()` 分类。静态方法 `isWall(ID)` / `isWallLike(ID)` / `isCore(ID)` 供无实例时按 ID 判定。`getDelay()` 返回单位出手延迟。实例方法 `getQuickestXList(wallX, isFirst)` 和静态方法 `getQuickestXList(ID, wallX, isFirst)` 计算该单位在突击中最速出手的 x 坐标集
- **UnitInfoPanel.java** — 单位信息面板。`null` layout 绝对定位：文本标签（名称/HP/CD/AT/cost/坐标）+ 右上角贴图
- **SpritePanel.java** — 单位贴图绘制面板。锚点对齐（核心 60,60 / 兵玉 43,55），`coreSpriteScale`/`nonCoreSpriteScale` 静态缩放变量。自适应深色模式边框
- **UnitListEntryPanel.java** — 单位列表条目。点击回调（`Consumer<Integer>`）+ `setSelected()` 高亮切换
- **FortPreviewPanel.java** — 阵型预览面板。背景图 + 分层精灵渲染（类要塞壁底层 → 非壁上层），含背景色取色器
- **FormulaTable.java** — 要塞运动公式表。从 `formula.json` 加载分段二次方程，二分查找段 → 计算 x(t)
- **ExprEvaluator.java** — 整数表达式求值器。支持变量 + `+`/`-`/`*`/`/` 运算

**工作流子系统：**
- **WorkflowGraph.java** — 有向图（当前线性链式，预留分支/循环结构）。Effect 节点编排 + 执行顺序管理
- **WorkflowNode.java** — 工作流节点。持有 Effect 实例 + 运行时参数（`Map<String, Object>`）+ 启用/禁用状态
- **NodeComponent.java** — 节点 UI 组件。悬停/选中/禁用背景色 + 双击编辑参数 + 单击效果开关（●/× 切换）+ 长按拖拽排序
- **WorkflowDropPanel.java** — 工作流面板。`BoxLayout.Y_AXIS` + 落点指示线绘制
- **Effect.java** / **EffectParameter.java** — Effect 接口（name/description/parameters/execute）+ 参数记录（key/label/type/defaultValue）
- **EffectRegistry.java** — Effect 注册表单例。`Class.forName()` 自注册 + 内置效果硬编码加载
- **EffectLibraryPanel.java** — 效果库面板。搜索框 + 双击添加 + 拖拽添加，通过 `Consumer<Effect>` 回调通信
- **effects/** — 内置效果：`ReplaceUnitEffect`（替换单位类型）、`ShiftFormationEffect`（平移阵容）、`RotateWallsEffect`（随机旋转墙壁）

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

**单位图鉴子系统：**
- **UnitDexTab.java** — 单位图鉴主标签页。`null` layout + `doLayout()` 比例布局（左 1/5 + 右 4/5）。左侧为 `BoxLayout.Y_AXIS` 单位列表 + `JScrollPane`，右侧为 `UnitDexDetailPanel`。JSON 加载用 Jackson 从 `unit_details.json` 读取。全局 AWT 点击监听取消选择
- **UnitDexEntryPanel.java** — 单位列表条目组件。32x32 `ThumbnailPanel` 缩略图 + 单位名称，`CompoundBorder` 选中高亮（主题色 2px）。贴图加载根据 `Unit.isCore(id)` 使用 `SpritePanel.coreSpriteScale`/`nonCoreSpriteScale` 计算有效视觉尺寸后等比缩放，核心单位缩略图按 0.5x 有效比例补偿
- **UnitDexDetailPanel.java** — 单位详情面板。`null` layout + `doLayout()`：左侧 `SpritePanel`（100x100）+ 右侧双列（名称/编码/流式属性标签 + 最速行动计算面板）+ 底部 JSON 扩展区。`TitledBorder` 颜色在 `updateColors()` 中随深色模式切换
- **unit_details.json** — 单位详情 JSON 数据文件（`src/main/resources/`），含 63 个条目（id 0-62），每项含 `id`/`description`/`tactics`/`notes` 字段。运行时由 `UnitDexTab` 加载并传入 `UnitDexDetailPanel`，供后续补全单位介绍文案

**最速行动计算（UnitDexDetailPanel 子功能）：**
- 显示条件：`cd >= 0 && shoot >= 0 && !Unit.isWallLike(id)`，不符合条件则隐藏整个面板
- 输入：`wallXField`（突击壁 x 坐标，`DocumentListener` 自动触发）+ `isFirstCheck`（"兵玉代码在前"，`ActionListener` 自动触发）
- 计算：调用 `Unit.getQuickestXList(ID, wallX, isFirst)`，结果显示在 `quickestResultArea`（只读 JTextArea）
- 布局：`BoxLayout.Y_AXIS`，内部子行使用 `FlowLayout.LEFT` 确保左对齐

**通用组件：**
- **ColorPicker.java** — HSB/RGB 取色器对话框。`ColorPicker.showDialog(parent, initial, darkMode, showReset)` 静态方法
- **FixedJTextArea.java** / **FixedTextAreaUI.java** / **FixedWrappedPlainView.java** — 修复 JDK `WrappedPlainView.viewToModel` 中 `round=false` 的光标向下取整问题

GUI 全局字体为 `黑体`，UI 文案和注释使用中文。
应用图标位于 `assets/icon.ico`，运行时从 classpath `/icon.ico` 加载。

### GUI 架构模式

- **面板提取**：可复用子面板从主标签页中拆分为独立类，通过 `Consumer<T>` / `Runnable` 回调与父组件通信，不储存父组件引用。深色模式通过 `Main.DARK_MODE` 静态字段在构造/绘制时读取。示例：`EffectLibraryPanel(Consumer<Effect>)`、`UnitListEntryPanel(Unit, int, Consumer<Integer>)`
- **比例布局**：使用 `null` layout + `ComponentListener.componentResized()` 中按百分比 `setBounds()`，确保布局稳定不受子组件 preferred size 影响。应用于 CraftTab 的三列及各内列面板。对于需要随内容变化重新布局的面板（如标签文本改变后高度变化），使用覆写 `doLayout()` 替代 `ComponentListener`，配合 `revalidate()` 触发布局更新
- **深色模式**：`Main.DARK_MODE` 全局静态字段。各面板的 `updateDarkMode()` 或 `refresh()` 方法在 `MainGUI` 中统一调用

### 对战引擎（稳定，勿改）

- **GameTask.java** — 游戏循环：`base_move()` → `judge()` → `update()`，直到一方 HP 归零或达到帧数上限
- **Main.runAllBattles()** — 线程池并行跑 1P×2P 全对阵，输出 `FortStats` 统计
- 元素层级：`Shape` → `Ball`（60+ 兵玉）/ `Wall` / `Bullet` / `HitSystem`
- 数据分离：敌我双方元素归入 `unit[0..1]`、`atk[0..1]`、`wall[0..1]` 等容器，命中检测直查对方容器
- **IntShapeMap** — 自定义 int→Shape 哈希表，开放寻址 + 斐波那契哈希 + 墓碑删除，缓存友好，不可替换为 HashMap
- **CompiledFort** — SoA（结构数组）布局的预编译阵容数据（`int[] type, x, y, r, seed`）

### 阵容编码

61 进制（`pskey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"` 字符表），`name&code` 格式。`Main.compileFort()` 解析，`to_xyr()` 解码坐标。HP 编解码见 `Formation.encodeHp()`/`Formation.decodeHp()`。

### 文件编码

`1P.txt`/`2P.txt` 可能是 UTF-8 或 GBK。`Setting.readUtf8()` 自动检测 BOM 并读取。输出统一 UTF-8。GUI 文本读取共享同一检测逻辑。

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

- 注释和 UI 文案使用中文
- GUI 字体使用 `黑体`
- 修改代码后不自动 git commit，由用户手动提交
- 可复用的 UI 组件优先抽取为独立类（如 ColorPicker、SpritePanel、UnitInfoPanel）
- 固定比例的面板布局使用 `null` layout + `ComponentListener` 模式，不使用 `GridBagLayout`（避免 preferred size 引起的布局跳动）
