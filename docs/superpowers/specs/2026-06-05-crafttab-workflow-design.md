# CraftTab 工作台与效果库设计

## 概述

为激突Kit的"阵型工作台"标签页实现阵容处理流水线系统。用户输入阵容代码，经过多个效果节点链式处理后输出新阵容。

## 核心数据模型

### Formation — 阵容对象

```java
public class Formation {
    String name;           // 阵型名称
    List<Unit> units;      // 单位列表（第一个固定为核心）
    String encode();       // 编码回阵容代码字符串
}
```

### Effect — 效果接口

```java
public interface Effect {
    String getName();                          // 效果名称
    String getDescription();                   // 描述
    List<EffectParameter> getParameters();     // 可配置参数
    Formation execute(Formation input);        // 执行变换
}
```

### EffectParameter — 参数定义

```java
public record EffectParameter(String key, String label, Type type, Object defaultValue) {
    enum Type { INT, STRING, BOOLEAN, UNIT_SELECTOR }
}
```

### WorkflowGraph — 通用有向图

内部为节点+边结构，不写死为数组链表：
- `Map<String, WorkflowNode> nodes` — id→节点
- `List<Edge> edges` — 有向边（from → to）
- `inputNodeId` / `outputNodeId` — 输入输出端点
- `execute(Formation)` — 拓扑序遍历执行

当前仅线性链，但架构已预留分支/循环/条件扩展。

### WorkflowNode — 图节点

- `id: String`
- `effect: Effect`
- `enabled: boolean`
- `params: Map<String, Object>`
- `getComponent(): JPanel` — 可重写的渲染方法

## 效果注册

- `EffectRegistry` 单例，启动时通过接口发现扫描 `effects/` 子包
- 所有效果实现 `Effect` 接口，新增效果只需写一个类即可被自动发现
- 初期所有效果同包内实现，预留外部插件加载接口

## UI 结构

### 工作台 (craftPanel)

```
JScrollPane
  └─ BoxLayout(Y_AXIS)
       ├─ "输入" 标识
       ├─ NodeComponent (效果1)
       ├─ NodeComponent (效果2)  
       ├─ NodeComponent (效果3)
       └─ "输出" 标识
```

每个 NodeComponent 是 JPanel，支持：
- 显示效果名称（未启用时灰色+删除线）
- 右键菜单（启用/禁用、删除、参数编辑）
- 拖拽排序
- 从效果库拖入新节点

### 效果库 (functionPanel)

```
├─ JTextField (搜索框)
└─ JScrollPane
     └─ EffectLibraryEntry 列表
```

- 搜索过滤
- 双击添加
- 拖拽添加

## 实时执行数据流

```
输入代码变化 / 节点增删改排 / 参数修改 / 启用状态变化
  │
  ▼  (300ms 防抖)
parseAndDisplay()
  │
  ├─ 解析 → Formation
  ├─ workflowGraph.execute(formation)
  ├─ 更新要塞信息 + 单位列表
  └─ 编码输出到 outputTextArea
```

## 文件清单

| 文件 | 职责 |
|------|------|
| `Formation.java` | 阵容数据对象 |
| `Effect.java` | 效果接口 |
| `EffectParameter.java` | 参数定义 record |
| `EffectRegistry.java` | 扫描注册 Effect 实现 |
| `WorkflowGraph.java` | 通用图 + 拓扑执行 |
| `WorkflowNode.java` | 图节点 |
| `NodeComponent.java` | 节点 JPanel 渲染 |
| `EffectLibraryPanel.java` | 效果库面板（替换 functionPanel 占位） |
| `effects/ReplaceUnitEffect.java` | 内置：单位替换 |
| `effects/ShiftFormationEffect.java` | 内置：阵容偏移 |
| `effects/RotateWallsEffect.java` | 内置：墙壁旋转 |

修改文件：
- `CraftTab.java` — 集成工作台和效果库面板，连接实时执行管道

## 非目标

- 外部 JAR 插件加载机制（预留接口，本次不实现）
- 循环/条件/分支节点（架构预留，本次仅线性链）
- 效果参数的可视化编辑对话框（本次用简易参数面板）
