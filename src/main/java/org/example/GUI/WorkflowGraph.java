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

    /** 从 INPUT 沿边执行到 OUTPUT，返回最终阵容。
     *  每个节点执行前先校验参数；执行中捕获异常并记录到 node.error。 */
    public Formation execute(Formation input) {
        Formation current = input;
        String nodeId = nextNode(INPUT_ID);
        while (nodeId != null && !nodeId.equals(OUTPUT_ID)) {
            WorkflowNode node = nodes.get(nodeId);
            if (node != null && node.enabled) {
                node.error = null;
                // 执行前校验
                java.util.List<String> errors = node.effect.validate(node.paramValues);
                if (!errors.isEmpty()) {
                    node.error = String.join("; ", errors);
                    break;
                }
                try {
                    current = node.effect.execute(current, node.paramValues);
                } catch (Exception e) {
                    node.error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    break;
                }
            }
            nodeId = nextNode(nodeId);
        }
        return current;
    }
}
