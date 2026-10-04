# CraftTab 工作台与效果库 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 CraftTab 中实现阵容处理流水线系统 —— 工作流图引擎 + 效果注册表 + 内置效果 + JPanel 节点 UI + 效果库面板 + 实时执行管道。

**Architecture:** 通用有向图 WorkflowGraph（非数组链表）作为执行引擎，Effect 接口 + static 自注册实现效果发现，JPanel 组件渲染节点，EffectLibraryPanel 提供搜索/拖拽/双击添加效果，所有变更通过 300ms 防抖触发自动重新执行。

**Tech Stack:** Java 21, Swing, FlatLaf 3.5.4, 纯 JDK（无外部依赖用于工作流引擎）

---

## File Structure

| 文件 | 职责 | 类型 |
|------|------|------|
| `Formation.java` | 阵容数据对象（name + units + encode） | 新建 |
| `Effect.java` | 效果接口（name, description, parameters, execute） | 新建 |
| `EffectParameter.java` | 参数定义 record（key, label, type, default） | 新建 |
| `EffectRegistry.java` | static 注册表，效果自注册 + 搜索 | 新建 |
| `WorkflowGraph.java` | 通用有向图 + 拓扑执行 | 新建 |
| `WorkflowNode.java` | 图节点（id, effect, enabled, params） | 新建 |
| `NodeComponent.java` | 节点 JPanel 渲染（名称 + 状态 + 右键菜单 + 拖拽） | 新建 |
| `EffectLibraryPanel.java` | 效果库面板（搜索 + 列表 + 双击/拖拽添加） | 新建 |
| `effects/ReplaceUnitEffect.java` | 内置：单位整体替换 | 新建 |
| `effects/ShiftFormationEffect.java` | 内置：阵容平移 | 新建 |
| `effects/RotateWallsEffect.java` | 内置：墙壁随机旋转 | 新建 |
| `CraftTab.java` | 集成所有组件，连接实时执行管道 | 修改 |

所有新建文件位于 `src/main/java/org/example/GUI/`，效果类位于 `src/main/java/org/example/GUI/effects/`。

---

### Task 1: Formation — 阵容数据模型

**Files:**
- Create: `src/main/java/org/example/GUI/Formation.java`

- [ ] **Step 1: 创建 Formation.java**

```java
package org.example.GUI;

import java.util.ArrayList;
import java.util.List;

/** 阵容数据对象，包含阵名和单位列表（第一个单位固定为核心）。 */
public class Formation {
    public String name;
    public final List<Unit> units;

    public Formation(String name, List<Unit> units) {
        this.name = name;
        this.units = new ArrayList<>(units);
    }

    /** 将阵容重新编码为 name&code 格式的字符串。 */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (Unit u : units) {
            String e = u.encode();
            if (!e.isEmpty()) {
                sb.append(e);
            }
        }
        String code = sb.toString();
        if (name == null || name.isEmpty()) {
            return code;
        }
        return name + "&" + code;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/Formation.java
```

---

### Task 2: Effect + EffectParameter — 效果接口

**Files:**
- Create: `src/main/java/org/example/GUI/EffectParameter.java`
- Create: `src/main/java/org/example/GUI/Effect.java`

- [ ] **Step 1: 创建 EffectParameter.java**

```java
package org.example.GUI;

/** 效果参数定义。 */
public record EffectParameter(
        String key,
        String label,
        Type type,
        Object defaultValue
) {
    public enum Type { INT, STRING, BOOLEAN }
}
```

- [ ] **Step 2: 创建 Effect.java**

```java
package org.example.GUI;

import java.util.Collections;
import java.util.List;

/** 效果接口 —— 阵容处理流水线的最小处理单元。 */
public interface Effect {
    String getName();
    String getDescription();

    default List<EffectParameter> getParameters() {
        return Collections.emptyList();
    }

    /** 对输入阵容执行变换，返回新阵容。不得修改输入对象。 */
    Formation execute(Formation input);
}
```

- [ ] **Step 3: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/EffectParameter.java src/main/java/org/example/GUI/Effect.java src/main/java/org/example/GUI/Formation.java
```

---

### Task 3: EffectRegistry — 效果注册表

**Files:**
- Create: `src/main/java/org/example/GUI/EffectRegistry.java`

- [ ] **Step 1: 创建 EffectRegistry.java**

```java
package org.example.GUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 效果注册表单例。效果类通过 static 块自注册，无需手动列举。 */
public class EffectRegistry {
    private static final List<Effect> effects = new ArrayList<>();

    public static void register(Effect effect) {
        effects.add(effect);
    }

    public static List<Effect> getAll() {
        return Collections.unmodifiableList(effects);
    }

    /** 按名称模糊搜索。 */
    public static List<Effect> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAll();
        }
        String lower = query.toLowerCase().trim();
        return effects.stream()
                .filter(e -> e.getName().toLowerCase().contains(lower)
                        || e.getDescription().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/EffectRegistry.java src/main/java/org/example/GUI/Effect.java
```

---

### Task 4: WorkflowNode — 图节点

**Files:**
- Create: `src/main/java/org/example/GUI/WorkflowNode.java`

- [ ] **Step 1: 创建 WorkflowNode.java**

```java
package org.example.GUI;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 工作流图中的一个节点，持有 Effect 实例及其运行时参数和状态。 */
public class WorkflowNode {
    public final String id;
    public final Effect effect;
    public boolean enabled = true;
    public final Map<String, Object> paramValues = new HashMap<>();

    public WorkflowNode(Effect effect) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.effect = effect;
        // 初始化参数默认值
        for (EffectParameter p : effect.getParameters()) {
            paramValues.put(p.key(), p.defaultValue());
        }
    }

    /** 创建该节点的默认渲染组件。子类可重写以自定义外观。 */
    public JPanel createComponent(Runnable onChanged, Runnable onDelete) {
        return new NodeComponent(this, onChanged, onDelete);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/WorkflowNode.java
```

NodeComponent 尚未创建，此编译会失败——暂时跳过，在 Task 8 后一起编译。

---

### Task 5: WorkflowGraph — 通用有向图引擎

**Files:**
- Create: `src/main/java/org/example/GUI/WorkflowGraph.java`

- [ ] **Step 1: 创建 WorkflowGraph.java**

```java
package org.example.GUI;

import java.util.*;

/** 通用有向图结构，用于编排 Effect 执行顺序。
 *  当前仅支持线性链式执行，但节点+边的图结构已为分支/循环/条件预留扩展能力。 */
public class WorkflowGraph {
    public static final String INPUT_ID  = "__INPUT__";
    public static final String OUTPUT_ID = "__OUTPUT__";

    private final Map<String, WorkflowNode> nodes = new LinkedHashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    public record Edge(String from, String to) {}

    // ---- 节点操作 ----

    public void addNode(WorkflowNode node) {
        nodes.put(node.id, node);
    }

    public void removeNode(String id) {
        nodes.remove(id);
        edges.removeIf(e -> e.from.equals(id) || e.to.equals(id));
    }

    public WorkflowNode getNode(String id) {
        return nodes.get(id);
    }

    public List<WorkflowNode> getEffectNodes() {
        List<WorkflowNode> list = new ArrayList<>(nodes.values());
        list.removeIf(n -> n.id.equals(INPUT_ID) || n.id.equals(OUTPUT_ID));
        return list;
    }

    // ---- 边操作 ----

    public void connect(String from, String to) {
        // 移除已存在的同方向边
        edges.removeIf(e -> e.from.equals(from) && e.to.equals(to));
        edges.add(new Edge(from, to));
    }

    public String nextNode(String nodeId) {
        for (Edge e : edges) {
            if (e.from.equals(nodeId)) return e.to;
        }
        return null;
    }

    public String prevNode(String nodeId) {
        for (Edge e : edges) {
            if (e.to.equals(nodeId)) return e.from;
        }
        return null;
    }

    // ---- 拓扑顺序 ----

    /** 返回从 INPUT 到 OUTPUT 的节点 ID 列表（仅效果节点，不含端点）。 */
    public List<String> getExecutionOrder() {
        List<String> order = new ArrayList<>();
        String current = nextNode(INPUT_ID);
        while (current != null && !current.equals(OUTPUT_ID)) {
            order.add(current);
            current = nextNode(current);
        }
        return order;
    }

    // ---- 重建线性链 ----

    /** 清空所有边，按给定顺序重建 INPUT → IDs[0] → ... → OUTPUT 的线性链。 */
    public void rebuildLinearChain(List<String> orderedIds) {
        edges.clear();
        String prev = INPUT_ID;
        for (String id : orderedIds) {
            if (nodes.containsKey(id)) {
                edges.add(new Edge(prev, id));
                prev = id;
            }
        }
        edges.add(new Edge(prev, OUTPUT_ID));
    }

    // ---- 执行 ----

    /** 从 INPUT 沿边执行到 OUTPUT，返回最终阵容。 */
    public Formation execute(Formation input) {
        Formation current = input;
        String nodeId = nextNode(INPUT_ID);
        while (nodeId != null && !nodeId.equals(OUTPUT_ID)) {
            WorkflowNode node = nodes.get(nodeId);
            if (node != null && node.enabled) {
                current = node.effect.execute(current);
            }
            nodeId = nextNode(nodeId);
        }
        return current;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/WorkflowGraph.java src/main/java/org/example/GUI/WorkflowNode.java src/main/java/org/example/GUI/Effect.java src/main/java/org/example/GUI/Formation.java
```

---

### Task 6: 内置效果 — ReplaceUnitEffect

**Files:**
- Create: `src/main/java/org/example/GUI/effects/ReplaceUnitEffect.java`

- [ ] **Step 1: 创建 ReplaceUnitEffect.java**

```java
package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.ArrayList;
import java.util.List;

/** 单位整体替换：将阵容中所有指定类型的单位替换为另一类型，保留坐标和旋转角。 */
public class ReplaceUnitEffect implements Effect {
    static {
        EffectRegistry.register(new ReplaceUnitEffect());
    }

    @Override
    public String getName() { return "单位替换"; }

    @Override
    public String getDescription() { return "将一种单位整体替换为另一种单位，保留原有坐标和旋转角。"; }

    @Override
    public List<EffectParameter> getParameters() {
        return List.of(
                new EffectParameter("sourceId", "源单位ID", EffectParameter.Type.INT, 1),
                new EffectParameter("targetId", "目标单位ID", EffectParameter.Type.INT, 1)
        );
    }

    @Override
    public Formation execute(Formation input) {
        int srcId = ((Number) paramValue("sourceId")).intValue();
        int tgtId = ((Number) paramValue("targetId")).intValue();

        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : input.units) {
            Unit nu;
            if (u.id == srcId && !u.isCore) {
                nu = new Unit(tgtId, u.x, u.y, u.r);
            } else {
                nu = new Unit(u.id, u.x, u.y, u.r);
                nu.isCore = u.isCore;
            }
            newUnits.add(nu);
        }
        return new Formation(input.name, newUnits);
    }

    private Object paramValue(String key) {
        // 临时方案：使用 EffectParameter 中的默认值
        for (EffectParameter p : getParameters()) {
            if (p.key().equals(key)) return p.defaultValue();
        }
        return 0;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/effects/ReplaceUnitEffect.java src/main/java/org/example/GUI/EffectRegistry.java src/main/java/org/example/GUI/Effect.java src/main/java/org/example/GUI/EffectParameter.java src/main/java/org/example/GUI/Formation.java
```

---

### Task 7: 内置效果 — ShiftFormationEffect + RotateWallsEffect

**Files:**
- Create: `src/main/java/org/example/GUI/effects/ShiftFormationEffect.java`
- Create: `src/main/java/org/example/GUI/effects/RotateWallsEffect.java`

- [ ] **Step 1: 创建 ShiftFormationEffect.java**

```java
package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.ArrayList;
import java.util.List;

/** 阵容整体平移：将所有单位的坐标偏移 (dx, dy)。核心也一并偏移。 */
public class ShiftFormationEffect implements Effect {
    static {
        EffectRegistry.register(new ShiftFormationEffect());
    }

    @Override
    public String getName() { return "阵容平移"; }

    @Override
    public String getDescription() { return "将阵容中所有单位（含核心）沿 X/Y 轴平移指定距离。"; }

    @Override
    public List<EffectParameter> getParameters() {
        return List.of(
                new EffectParameter("dx", "X偏移", EffectParameter.Type.INT, 0),
                new EffectParameter("dy", "Y偏移", EffectParameter.Type.INT, 0)
        );
    }

    @Override
    public Formation execute(Formation input) {
        int dx = ((Number) getParam("dx")).intValue();
        int dy = ((Number) getParam("dy")).intValue();

        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : input.units) {
            Unit nu = new Unit(u.id, u.x + dx, u.y + dy, u.r);
            nu.isCore = u.isCore;
            newUnits.add(nu);
        }
        return new Formation(input.name, newUnits);
    }

    private Object getParam(String key) {
        for (EffectParameter p : getParameters()) {
            if (p.key().equals(key)) return p.defaultValue();
        }
        return 0;
    }
}
```

- [ ] **Step 2: 创建 RotateWallsEffect.java**

```java
package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.*;

/** 墙壁随机旋转：将阵容中所有墙壁类单位的旋转角设为随机值。 */
public class RotateWallsEffect implements Effect {
    // 墙壁类单位 ID
    private static final Set<Integer> WALL_IDS = Set.of(25, 26, 27, 28, 43, 44, 45, 46, 47, 48);

    static {
        EffectRegistry.register(new RotateWallsEffect());
    }

    @Override
    public String getName() { return "墙壁随机旋转"; }

    @Override
    public String getDescription() { return "将所有要塞壁和突击壁的旋转角随机化。"; }

    @Override
    public Formation execute(Formation input) {
        Random rng = new Random();
        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : input.units) {
            Unit nu;
            if (WALL_IDS.contains(u.id)) {
                nu = new Unit(u.id, u.x, u.y, rng.nextInt(360));
            } else {
                nu = new Unit(u.id, u.x, u.y, u.r);
                nu.isCore = u.isCore;
            }
            newUnits.add(nu);
        }
        return new Formation(input.name, newUnits);
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/effects/ShiftFormationEffect.java src/main/java/org/example/GUI/effects/RotateWallsEffect.java
```

---

### Task 8: NodeComponent — 节点 JPanel 渲染

**Files:**
- Create: `src/main/java/org/example/GUI/NodeComponent.java`

- [ ] **Step 1: 创建 NodeComponent.java**

```java
package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 工作流节点的默认 JPanel 渲染组件。显示效果名称，未启用时灰色+删除线。 */
public class NodeComponent extends JPanel {
    private static final Color BG_ENABLED_DARK  = new Color(60, 63, 65);
    private static final Color BG_ENABLED_LIGHT = new Color(245, 245, 245);
    private static final Color BG_DISABLED_DARK  = new Color(50, 50, 50);
    private static final Color BG_DISABLED_LIGHT = new Color(230, 230, 230);

    protected final WorkflowNode node;
    protected final Runnable onChanged;
    private final JLabel nameLabel;

    public NodeComponent(WorkflowNode node, Runnable onChanged, Runnable onDelete) {
        super(new BorderLayout(10, 0));
        this.node = node;
        this.onChanged = onChanged;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(90, 90, 90) : new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // 拖拽手柄
        JLabel dragHandle = new JLabel("☰");
        dragHandle.setFont(new Font("黑体", Font.PLAIN, 14));
        dragHandle.setForeground(new Color(150, 150, 150));
        add(dragHandle, BorderLayout.WEST);

        // 名称标签
        nameLabel = new JLabel(node.effect.getName());
        nameLabel.setFont(new Font("黑体", Font.PLAIN, 13));
        add(nameLabel, BorderLayout.CENTER);

        // 右键菜单
        JPopupMenu menu = new JPopupMenu();
        JMenuItem toggleItem = new JMenuItem(node.enabled ? "禁用" : "启用");
        toggleItem.addActionListener(e -> {
            node.enabled = !node.enabled;
            applyStyle();
            onChanged.run();
        });
        menu.add(toggleItem);
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> onDelete.run());
        menu.add(deleteItem);
        setComponentPopupMenu(menu);

        applyStyle();
    }

    protected void applyStyle() {
        if (node.enabled) {
            setBackground(Main.DARK_MODE ? BG_ENABLED_DARK : BG_ENABLED_LIGHT);
            nameLabel.setText(node.effect.getName());
        } else {
            setBackground(Main.DARK_MODE ? BG_DISABLED_DARK : BG_DISABLED_LIGHT);
            nameLabel.setText("<html><span style='color:gray;text-decoration:line-through'>"
                    + node.effect.getName() + "</span></html>");
        }
        setOpaque(true);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/NodeComponent.java src/main/java/org/example/GUI/WorkflowNode.java src/main/java/org/example/GUI/Effect.java
```

---

### Task 9: EffectLibraryPanel — 效果库面板

**Files:**
- Create: `src/main/java/org/example/GUI/EffectLibraryPanel.java`

- [ ] **Step 1: 创建 EffectLibraryPanel.java**

```java
package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/** 效果库面板：搜索框 + 可双击/拖拽添加的效果条目列表。 */
public class EffectLibraryPanel extends JPanel {
    private final JTextField searchField;
    private final JPanel listPanel;
    private final Consumer<Effect> onAddEffect;

    public EffectLibraryPanel(Consumer<Effect> onAddEffect) {
        super(new BorderLayout(0, 4));
        this.onAddEffect = onAddEffect;

        searchField = new JTextField();
        searchField.setFont(new Font("黑体", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "搜索效果...");

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(searchField, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });

        refresh();
    }

    private void refresh() {
        listPanel.removeAll();
        List<Effect> results = EffectRegistry.search(searchField.getText());
        for (Effect effect : results) {
            listPanel.add(createEntry(effect));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createEntry(Effect effect) {
        JPanel entry = new JPanel(new BorderLayout(6, 0));
        entry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        Main.DARK_MODE ? new Color(70, 70, 70) : new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        entry.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel(effect.getName());
        nameLabel.setFont(new Font("黑体", Font.PLAIN, 13));
        entry.add(nameLabel, BorderLayout.CENTER);

        JLabel descLabel = new JLabel(effect.getDescription());
        descLabel.setFont(new Font("黑体", Font.PLAIN, 10));
        descLabel.setForeground(Main.DARK_MODE ? new Color(160, 160, 160) : new Color(140, 140, 140));
        entry.add(descLabel, BorderLayout.SOUTH);

        // 双击添加
        entry.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onAddEffect.accept(effect);
                }
            }
        });

        // 悬停高亮
        entry.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                entry.setBackground(Main.DARK_MODE ? new Color(70, 70, 75) : new Color(235, 235, 240));
                entry.setOpaque(true);
            }
            @Override public void mouseExited(MouseEvent e) {
                entry.setOpaque(false);
                entry.repaint();
            }
        });

        return entry;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "...flatlaf...;...jackson..." -sourcepath src/main/java src/main/java/org/example/GUI/EffectLibraryPanel.java src/main/java/org/example/GUI/EffectRegistry.java src/main/java/org/example/GUI/Effect.java
```

---

### Task 10: CraftTab 集成 — 连接所有组件

**Files:**
- Modify: `src/main/java/org/example/GUI/CraftTab.java` (完整重写)

- [ ] **Step 1: 重写 CraftTab.java 集成工作台和效果库**

关键变更：
1. `craftPanel` → 工作台面板（JScrollPane + BoxLayout 中放 "输入" 标签 + NodeComponent 列表 + "输出" 标签）
2. `functionPanel` → EffectLibraryPanel 实例
3. 新增 `WorkflowGraph` 实例 + `rebuildWorkflowUI()` + `runPipeline()` 方法
4. `parseAndDisplay()` 增加工作流执行步骤
5. 所有变更触发 `runPipeline()`

```java
package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

public class CraftTab extends JPanel {
    private final JFrame parentFrame;

    private final JScrollPane inputPanel;
    private final JScrollPane outputPanel;
    private final JTabbedPane infoTabPane;
    private final JTextArea fortInfoTextArea;
    private final JPanel unitListContent;
    private final JPanel workflowContent;
    private final EffectLibraryPanel effectLibraryPanel;
    private final JPanel unitInfoPanel;
    private final JPanel fortInfoPanel;
    private final JPanel analysisPanel;

    private final JTextArea inputTextArea;
    private final JTextArea outputTextArea;

    private final Timer parseTimer;
    private final WorkflowGraph workflowGraph = new WorkflowGraph();
    private List<Unit> currentUnits = new ArrayList<>();
    private String currentFortName = "";

    // 标签用于工作台中的 "输入" 和 "输出"
    private final JLabel workflowInputLabel;
    private final JLabel workflowOutputLabel;

    public CraftTab(JFrame parentFrame) {
        super(new BorderLayout(8, 8));
        this.parentFrame = parentFrame;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        inputTextArea = createTextArea(true);
        outputTextArea = createTextArea(false);
        inputTextArea.setLineWrap(true);
        outputTextArea.setLineWrap(true);

        // 创建面板
        inputPanel = new JScrollPane(inputTextArea);
        outputPanel = new JScrollPane(outputTextArea);
        unitInfoPanel = new JPanel();
        fortInfoPanel = new JPanel();
        analysisPanel = new JPanel();

        inputPanel.setBorder(BorderFactory.createTitledBorder("输入"));
        outputPanel.setBorder(BorderFactory.createTitledBorder("输出"));
        unitInfoPanel.setBorder(BorderFactory.createTitledBorder("单位信息"));
        fortInfoPanel.setBorder(BorderFactory.createTitledBorder("阵型预览"));
        analysisPanel.setBorder(BorderFactory.createTitledBorder("分析"));

        // ---- 要塞信息标签页 ----
        fortInfoTextArea = new JTextArea();
        fortInfoTextArea.setEditable(false);
        fortInfoTextArea.setFont(new Font("黑体", Font.PLAIN, 14));
        fortInfoTextArea.setLineWrap(true);
        fortInfoTextArea.setWrapStyleWord(true);

        JScrollPane fortInfoScroll = new JScrollPane(fortInfoTextArea);
        fortInfoScroll.setBorder(null);
        fortInfoScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        fortInfoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // ---- 单位列表标签页 ----
        unitListContent = new JPanel();
        unitListContent.setLayout(new BoxLayout(unitListContent, BoxLayout.Y_AXIS));
        unitListContent.setOpaque(true);

        JScrollPane unitListScroll = new JScrollPane(unitListContent);
        unitListScroll.setBorder(null);
        unitListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        unitListScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        unitListScroll.getVerticalScrollBar().setUnitIncrement(16);

        infoTabPane = new JTabbedPane();
        infoTabPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(70, 70, 70) : new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        infoTabPane.addTab("要塞信息", fortInfoScroll);
        infoTabPane.addTab("单位列表", unitListScroll);

        // ---- 工作台面板 ----
        workflowInputLabel = createWorkflowLabel("▼ 输入");
        workflowOutputLabel = createWorkflowLabel("▲ 输出");

        workflowContent = new JPanel();
        workflowContent.setLayout(new BoxLayout(workflowContent, BoxLayout.Y_AXIS));
        workflowContent.setOpaque(true);

        JScrollPane workflowScroll = new JScrollPane(workflowContent);
        workflowScroll.setBorder(null);
        workflowScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        workflowScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        workflowScroll.getVerticalScrollBar().setUnitIncrement(16);

        // ---- 效果库面板 ----
        effectLibraryPanel = new EffectLibraryPanel(this::addEffectToWorkflow);

        // 左侧列
        JPanel leftPanel = new JPanel(new GridBagLayout());
        addToColumn(leftPanel, inputPanel,  0, 0.25);
        addToColumn(leftPanel, outputPanel, 1, 0.25);
        addToColumn(leftPanel, infoTabPane, 2, 0.50);

        // 右侧列
        JPanel rightPanel = new JPanel(new GridBagLayout());
        addToColumn(rightPanel, unitInfoPanel,  0, 0.25);
        addToColumn(rightPanel, fortInfoPanel,  1, 0.45);
        addToColumn(rightPanel, analysisPanel,  2, 0.30);

        // 中间列：工作台 + 效果库
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.fill = GridBagConstraints.BOTH;
        gbcCenter.weighty = 1.0;
        gbcCenter.gridy = 0;
        gbcCenter.gridx = 0;
        gbcCenter.weightx = 33.0;
        centerPanel.add(workflowScroll, gbcCenter);
        gbcCenter.gridx = 1;
        gbcCenter.weightx = 22.0;
        centerPanel.add(effectLibraryPanel, gbcCenter);

        // 三列，固定宽度比
        JPanel columns = new JPanel(null);
        columns.add(leftPanel);
        columns.add(centerPanel);
        columns.add(rightPanel);
        columns.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = columns.getWidth();
                int h = columns.getHeight();
                if (w <= 0 || h <= 0) return;
                int leftW = w * 22 / 100;
                int centerW = w * 55 / 100;
                int rightW = w - leftW - centerW;
                leftPanel.setBounds(0, 0, leftW, h);
                centerPanel.setBounds(leftW, 0, centerW, h);
                rightPanel.setBounds(leftW + centerW, 0, rightW, h);
            }
        });

        add(columns, BorderLayout.CENTER);

        // 自动解析：300ms 防抖
        parseTimer = new Timer(300, e -> runPipeline());
        parseTimer.setRepeats(false);
        inputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { parseTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { parseTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { parseTimer.restart(); }
        });
    }

    // ========== 工作流操作 ==========

    private void addEffectToWorkflow(Effect effect) {
        WorkflowNode node = new WorkflowNode(effect);
        workflowGraph.addNode(node);

        List<String> order = workflowGraph.getExecutionOrder();
        if (!order.contains(node.id)) {
            order.add(node.id);
        }
        workflowGraph.rebuildLinearChain(order);
        rebuildWorkflowUI();
        runPipeline();
    }

    private void removeNodeFromWorkflow(String nodeId) {
        workflowGraph.removeNode(nodeId);
        workflowGraph.rebuildLinearChain(workflowGraph.getExecutionOrder());
        rebuildWorkflowUI();
        runPipeline();
    }

    private void rebuildWorkflowUI() {
        workflowContent.removeAll();
        workflowContent.add(workflowInputLabel);
        workflowContent.add(Box.createVerticalStrut(4));

        List<String> order = workflowGraph.getExecutionOrder();
        for (String id : order) {
            WorkflowNode node = workflowGraph.getNode(id);
            if (node != null) {
                JPanel comp = node.createComponent(
                        () -> { rebuildWorkflowUI(); runPipeline(); },
                        () -> removeNodeFromWorkflow(id));
                workflowContent.add(comp);
                workflowContent.add(Box.createVerticalStrut(4));
            }
        }
        workflowContent.add(workflowOutputLabel);
        workflowContent.add(Box.createVerticalGlue());
        workflowContent.revalidate();
        workflowContent.repaint();
    }

    // ========== 管线执行 ==========

    private void runPipeline() {
        String text = inputTextArea.getText().trim();
        if (text.isEmpty()) {
            clearResults();
            return;
        }

        try {
            // 1. 解析输入
            String name, code;
            int ampIdx = text.indexOf('&');
            if (ampIdx >= 0) {
                name = text.substring(0, ampIdx).trim();
                code = text.substring(ampIdx + 1).trim();
            } else {
                name = "";
                code = text;
            }

            if (code.length() < 6) {
                throw new IllegalArgumentException("代码长度不足（至少需要6个字符）");
            }

            Unit core = Unit.decodeAsCore(code.substring(0, 6));
            core.isCore = true;

            List<Unit> units = new ArrayList<>();
            units.add(core);
            int unitCount = code.length() / 6 - 1;
            for (int i = 0; i < unitCount; i++) {
                int start = 6 + i * 6;
                Unit unit = Unit.decode(code.substring(start, start + 6));
                if (unit.id >= 0 && unit.id < Unit.infos.length) {
                    units.add(unit);
                }
            }

            Formation input = new Formation(name, units);

            // 2. 执行工作流
            Formation result = workflowGraph.execute(input);

            // 3. 显示结果
            currentUnits = result.units;
            currentFortName = result.name;
            updateFortInfo();
            updateUnitList();
            outputTextArea.setText(result.encode());
        } catch (Exception e) {
            clearResults();
            fortInfoTextArea.setText("解析失败: " + e.getMessage());
            outputTextArea.setText("");
        }
    }

    private void clearResults() {
        currentUnits.clear();
        currentFortName = "";
        fortInfoTextArea.setText("");
        outputTextArea.setText("");
        unitListContent.removeAll();
        unitListContent.revalidate();
        unitListContent.repaint();
    }

    // ========== 要塞信息 ==========

    private void updateFortInfo() {
        int redCount = 0;
        int blueCount = 0;
        int totalCost = 0;
        for (Unit u : currentUnits) {
            if (u.id == 29) redCount++;
            else if (u.id == 30) blueCount++;
            if (u.id >= 0 && u.id < Unit.infos.length) {
                totalCost += Unit.infos[u.id].cost();
            }
        }
        int accelLevel = redCount * 1 + blueCount * 2;

        StringBuilder sb = new StringBuilder();
        sb.append("阵型名称: ").append(currentFortName.isEmpty() ? "(无)" : currentFortName).append("\n\n");
        sb.append("加速等级: ").append(accelLevel).append("\n\n");
        sb.append("总军资金: ").append(totalCost);
        fortInfoTextArea.setText(sb.toString());
    }

    // ========== 单位列表 ==========

    private void updateUnitList() {
        unitListContent.removeAll();

        for (int i = 0; i < currentUnits.size(); i++) {
            Unit unit = currentUnits.get(i);
            JPanel entry = createUnitEntry(unit);
            unitListContent.add(entry);
            if (i < currentUnits.size() - 1) {
                unitListContent.add(Box.createVerticalStrut(2));
            }
        }
        unitListContent.add(Box.createVerticalGlue());
        unitListContent.revalidate();
        unitListContent.repaint();
    }

    private JPanel createUnitEntry(Unit unit) {
        JPanel entry = new JPanel(new BorderLayout(12, 0));
        entry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(80, 80, 80) : new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        String unitName;
        if (unit.isCore) {
            unitName = "要塞核心";
        } else {
            unitName = Unit.infos[unit.id].name();
        }
        JLabel nameLabel = new JLabel(unitName);
        nameLabel.setFont(new Font("黑体", Font.PLAIN, 13));

        JLabel coordLabel = new JLabel("(" + unit.x + ", " + unit.y + ")");
        coordLabel.setFont(new Font("黑体", Font.PLAIN, 12));
        coordLabel.setForeground(Main.DARK_MODE ? new Color(180, 180, 180) : new Color(120, 120, 120));

        JPanel leftP = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftP.setOpaque(false);
        leftP.add(nameLabel);
        leftP.add(coordLabel);

        entry.add(leftP, BorderLayout.CENTER);

        if (unit.isCore) {
            JLabel coreTag = new JLabel("核心");
            coreTag.setFont(new Font("黑体", Font.BOLD, 11));
            coreTag.setForeground(new Color(200, 120, 0));
            entry.add(coreTag, BorderLayout.EAST);
            if (Main.DARK_MODE) {
                entry.setBackground(new Color(60, 50, 30));
            } else {
                entry.setBackground(new Color(255, 248, 225));
            }
            entry.setOpaque(true);
        }

        return entry;
    }

    // ========== 布局辅助 ==========

    private JLabel createWorkflowLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("黑体", Font.BOLD, 12));
        label.setForeground(Main.DARK_MODE ? new Color(160, 160, 160) : new Color(100, 100, 100));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        label.setAlignmentX(CENTER_ALIGNMENT);
        return label;
    }

    private void addToColumn(JPanel column, JComponent comp, int gridy, double weighty) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = gridy;
        gbc.weightx = 1.0;
        gbc.weighty = weighty;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);
        column.add(comp, gbc);
    }

    private static JTextArea createTextArea(boolean editable) {
        JTextArea area = new JTextArea(1, 1);
        area.setEditable(editable);
        area.setFont(new Font("黑体", Font.PLAIN, 13));
        area.setTabSize(4);
        area.setLineWrap(Main.WORD_WRAP);
        area.setWrapStyleWord(Main.WORD_WRAP);
        return area;
    }
}
```

注意：`overrideDeleteAction` 方法中替换右键菜单删除行为的逻辑在每次 `rebuildWorkflowUI` 时重新绑定。后续可重构为 NodeComponent 接受删除回调参数。

- [ ] **Step 2: 编译所有文件**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" -sourcepath src/main/java src/main/java/org/example/GUI/*.java src/main/java/org/example/GUI/effects/*.java
```

---

### Task 11: 验证运行

- [ ] **Step 1: 启动 GUI 并测试完整流程**

```bash
"C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\java" -cp "target/classes;C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" org.example.Main
```

测试步骤：
1. 在"阵型工作台"输入 1P.txt 中的阵容代码，确认解析正常
2. 双击效果库中的效果，确认添加到工作台
3. 右键节点 → 禁用/启用，确认节点变灰/恢复
4. 右键节点 → 删除，确认节点消失
5. 修改输入代码，确认自动重新执行
6. 确认输出窗口显示处理后的阵容代码
7. 确认要塞信息 + 单位列表反映处理结果
