# 贡献分析板块实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增「贡献分析」标签页：对玩家阵型枚举/采样「删除 n 个单位」的组合，批量对战评判阵集，找出对胜率贡献小的单位（组合排行 + 单位汇总 + 可复制新阵型代码）。

**Architecture:** 新增纯逻辑类 `ContributionAnalyzer`（org.example，可访问 CompiledFort 包内字段）+ `ComboSelector`（组合枚举/采样）+ `FortTrimmer`（SoA 裁剪）；GUI 层新增 `ContributionTab`，独立线程池滑动窗口调度，复用 `GameTask.run_single`，不改模拟引擎。`Setting` 抽取 `parseForts` 并剥离 `#HP` 后缀。

**Tech Stack:** Java 21、Swing + FlatLaf 3.5.4、Jackson 2.18.3（仅既有 GUI 用），javac 直接编译（Maven 不在 PATH）。

**设计文档:** `docs/superpowers/specs/2026-09-13-contribution-analysis-design.md`
与 spec 的偏差（spec 已同步）：组合数超过 100 万（`ComboSelector.MAX_SAMPLES`）时拒绝运行以防 OOM；采样统一采用 Floyd 排名抽样（不按 C 大小分流枚举）。

---

## 项目约定与统一命令

- 按 `AGENTS.md` 约定：**不自动 git commit**，本计划所有任务无提交步骤，由用户手动提交。
- 代码注释与 UI 文案使用中文；GUI 字体使用 `黑体`。
- 临时自测目录（不在仓库内）：`C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest`

**统一命令（下称「编译主代码」）：**

```powershell
Get-ChildItem -Recurse -Filter *.java src/main/java | ForEach-Object { $_.FullName } | Set-Content -Encoding ascii target\sources.txt
& "C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d target/classes -cp "C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" "@target/sources.txt"
```

预期：无错误，仅有既有的 deprecation / unchecked 警告。

**统一命令（下称「编译自测」）：**

```powershell
& "C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\javac" -encoding UTF-8 -d "C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\classes" -cp "D:\program\gekitotsu_tool\target\classes" "C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\src\org\example\SelfTest.java"
```

**统一命令（下称「运行自测」）：**

```powershell
& "C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\java" -cp "C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\classes;D:\program\gekitotsu_tool\target\classes" org.example.SelfTest
```

预期：每行 `PASS testXxx`，最后 `TOTAL pass=N fail=0`。

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/main/java/org/example/Setting.java` | 修改 | 抽取 `parseForts(String)`，剥离 `#HP` 后缀，`CompileForts` 委托之 |
| `src/main/java/org/example/ComboSelector.java` | 新建 | C(N,n) 计算、字典序排名/反解、Floyd 采样、全遍历 |
| `src/main/java/org/example/FortTrimmer.java` | 新建 | 按索引过滤 CompiledFort 的 SoA 数组 |
| `src/main/java/org/example/ContributionAnalyzer.java` | 新建 | 解析、基线/组合批量对战、聚合、排序、复制代码生成 |
| `src/main/java/org/example/GUI/ContributionTab.java` | 新建 | 输入/参数/结果三面板、进度、取消、复制 |
| `src/main/java/org/example/GUI/WrapLayout.java` | 新建 | 可换行 FlowLayout，窄窗口自动增高 |
| `src/main/java/org/example/GUI/MainGUI.java` | 修改 | 注册标签页 + 深色模式传播 |
| `AGENTS.md` | 修改 | 补充贡献分析子系统说明 |
| `C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\src\org\example\SelfTest.java` | 新建（仓库外） | headless 自测脚手架，各任务逐步追加测试 |

---

### Task 1: 自测脚手架 + Setting.parseForts 重构

**Files:**
- Create: `C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\src\org\example\SelfTest.java`
- Modify: `src/main/java/org/example/Setting.java:115-152`

- [ ] **Step 1: 创建自测脚手架（含 parseForts 测试，此时应编译失败）**

完整文件内容：

```java
package org.example;

import org.example.GUI.Formation;
import org.example.GUI.Unit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** headless 自测脚手架：反射调用所有 testXxx 方法。 */
public class SelfTest {

    public static void main(String[] args) throws Exception {
        Method[] methods = SelfTest.class.getDeclaredMethods();
        java.util.Arrays.sort(methods, java.util.Comparator.comparing(Method::getName));
        int pass = 0;
        int fail = 0;
        for (Method m : methods) {
            if (!m.getName().startsWith("test") || m.getParameterCount() != 0
                    || !java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            try {
                m.invoke(null);
                System.out.println("PASS " + m.getName());
                pass++;
            } catch (InvocationTargetException e) {
                System.out.println("FAIL " + m.getName() + ": " + e.getCause());
                e.getCause().printStackTrace(System.out);
                fail++;
            }
        }
        System.out.println("TOTAL pass=" + pass + " fail=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void assertTrue(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError(msg);
        }
    }

    static void assertEquals(Object expected, Object actual, String msg) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(msg + " expected=" + expected + " actual=" + actual);
        }
    }

    static void assertEqualsDouble(double expected, double actual, double eps, String msg) {
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            if (Double.isNaN(expected) != Double.isNaN(actual)) {
                throw new AssertionError(msg + " expected=" + expected + " actual=" + actual);
            }
            return;
        }
        if (Math.abs(expected - actual) > eps) {
            throw new AssertionError(msg + " expected=" + expected + " actual=" + actual);
        }
    }

    static int bucketOf(int status) {
        return status == 1 ? 0 : status == 2 ? 1 : status == 0 ? 2 : 3;
    }

    static double expectedWinRate(int[] counts) {
        int decisive = counts[0] + counts[1] + counts[2];
        return decisive > 0 ? (2.0 * counts[0] + counts[2]) * 50.0 / decisive : Double.NaN;
    }

    /** 合成阵型：核心 + 指定单位，返回 name&code#hp 文本。 */
    static String synthFormation(String name, Unit... units) {
        List<Unit> list = new ArrayList<>();
        list.add(new Unit(0, 30, 48, 0));
        for (Unit u : units) {
            list.add(u);
        }
        return new Formation(name, list).encode();
    }

    /** 去掉阵型文本中的 name& 前缀与 #hp 后缀，只留代码。 */
    static String codeOnly(String text) {
        int amp = text.indexOf('&');
        String code = amp >= 0 ? text.substring(amp + 1) : text;
        int hash = code.indexOf('#');
        return hash >= 0 ? code.substring(0, hash) : code;
    }

    static void testParseFortsBasic() {
        String a = synthFormation("A", new Unit(25, 100, 200, 0), new Unit(1, 150, 100, 0));
        String b = synthFormation("B", new Unit(3, 200, 150, 0));
        List<CompiledFort> forts = Setting.parseForts(a + "/" + b);
        assertEquals(2, forts.size(), "parsed count");
        assertEquals(codeOnly(a).length() / 6 - 1, forts.get(0).unitCount, "fort A unitCount");
        assertEquals(codeOnly(b).length() / 6 - 1, forts.get(1).unitCount, "fort B unitCount");
    }

    static void testParseFortsWithHpSuffix() {
        Unit wall = new Unit(25, 100, 200, 0);
        wall.hp = 5;
        String a = synthFormation("A", wall, new Unit(1, 150, 100, 0));
        assertTrue(a.contains("#"), "hp suffix present in test input");
        List<CompiledFort> forts = Setting.parseForts(a);
        assertEquals(1, forts.size(), "hp fort parsed");
        assertEquals(2, forts.get(0).unitCount, "hp fort unitCount");
    }

    static void testParseFortsSkipsMalformed() {
        String a = synthFormation("A", new Unit(25, 100, 200, 0));
        String b = synthFormation("B", new Unit(1, 150, 100, 0));
        List<CompiledFort> forts = Setting.parseForts(a + "/A&/&/" + b);
        assertEquals(2, forts.size(), "malformed entries skipped");
        assertEquals("A", forts.get(0).name, "first kept");
        assertEquals("B", forts.get(1).name, "second kept");
    }

    static void testParseFortsNullAndEmpty() {
        assertTrue(Setting.parseForts(null).isEmpty(), "null -> empty");
        assertTrue(Setting.parseForts("   ").isEmpty(), "blank -> empty");
        assertTrue(Setting.parseForts("///").isEmpty(), "slashes -> empty");
    }
}
```

- [ ] **Step 2: 编译自测，确认失败**

运行「编译自测」。
预期：编译错误，提示 `找不到符号 ... parseForts`。

- [ ] **Step 3: 修改 Setting.java**

把 `CompileForts`（第 115-152 行整个方法）替换为下面两个方法：

```java
    public static List<CompiledFort> CompileForts(String fileName) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            return parseForts(readUtf8(bytes));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("读取 " + fileName + " 失败");
            return new ArrayList<>();
        }
    }

    /** 解析 name&code（多条用 / 分隔）文本；自动剥离 #HP 后缀，畸形条目会被跳过（打印提示），不会抛异常。 */
    public static List<CompiledFort> parseForts(String content) {
        List<CompiledFort> list = new ArrayList<>();
        if (content == null) {
            return list;
        }
        String[] parts = content.trim().split("/");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            int idx = part.lastIndexOf("&");
            String name;
            String code;
            if (idx == -1) {
                name = "";
                code = part;
            } else {
                name = part.substring(0, idx);
                code = part.substring(idx + 1);
            }
            int hash = code.indexOf('#');
            if (hash >= 0) {
                code = code.substring(0, hash);
            }
            code = code.replaceAll("[^a-zA-Z0-9]", "");
            if (idx == -1) {
                if (code.length() < 6) {
                    continue;
                }
                list.add(Main.compileFort(new Fort("", code)));
            } else {
                if (code.length() < 6 || code.length() % 6 != 0) {
                    System.out.println("阵" + name + "代码长度错误，已跳过");
                    continue;
                }
                list.add(Main.compileFort(new Fort(name, code)));
            }
        }
        return list;
    }
```

- [ ] **Step 4: 验证**

依次运行「编译主代码」「编译自测」「运行自测」。
预期：`PASS testParseFortsBasic`、`PASS testParseFortsNullAndEmpty`、`PASS testParseFortsSkipsMalformed`、`PASS testParseFortsWithHpSuffix`、`TOTAL pass=4 fail=0`。

---

### Task 2: ComboSelector（组合枚举与采样）

**Files:**
- Create: `src/main/java/org/example/ComboSelector.java`
- Modify: `C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\src\org\example\SelfTest.java`（追加测试方法）

- [ ] **Step 1: 在 SelfTest.java 末尾（最后一个 `}` 之前）追加测试方法**

```java
    static void testCombinationCount() {
        assertEquals(9880L, ComboSelector.combinationCount(40, 3), "C(40,3)");
        assertEquals(4060L, ComboSelector.combinationCount(30, 3), "C(30,3)");
        assertEquals(1L, ComboSelector.combinationCount(5, 0), "C(5,0)");
        assertEquals(1L, ComboSelector.combinationCount(5, 5), "C(5,5)");
        assertEquals(0L, ComboSelector.combinationCount(3, 5), "C(3,5)");
    }

    static void testRankUnrankRoundTrip() {
        int n = 8;
        int k = 3;
        long total = ComboSelector.combinationCount(n, k);
        for (long r = 0; r < total; r++) {
            int[] comb = ComboSelector.unrank(r, n, k);
            assertEquals(r, ComboSelector.rank(comb, n), "rank/unrank roundtrip r=" + r);
        }
        assertEquals("0,1,2", join(ComboSelector.unrank(0, n, k)), "first combination");
        assertEquals("5,6,7", join(ComboSelector.unrank(total - 1, n, k)), "last combination");
    }

    static void testPlanFullEnumeration() {
        ComboSelector.Selection sel = ComboSelector.plan(10, 1, 100, new java.util.Random(1));
        assertEquals(10L, sel.total(), "total");
        assertEquals(10, sel.count(), "count");
        List<int[]> combos = sel.materialize();
        assertEquals(10, combos.size(), "materialized size");
        assertEquals("0", join(combos.get(0)), "first combo");
        assertEquals("9", join(combos.get(9)), "last combo");
    }

    static void testPlanSampling() {
        ComboSelector.Selection sel = ComboSelector.plan(10, 1, 20, new java.util.Random(1));
        assertEquals(2, sel.count(), "20% of 10 = 2");
        List<int[]> combos = sel.materialize();
        assertEquals(sel.count(), combos.size(), "materialize size matches count");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int[] c : combos) {
            assertTrue(c.length == 1 && c[0] >= 0 && c[0] < 10, "index in range");
            assertTrue(seen.add(java.util.Arrays.toString(c)), "distinct");
        }
    }

    static void testPlanSamplingAtLeastOne() {
        ComboSelector.Selection sel = ComboSelector.plan(10, 2, 1, new java.util.Random(1));
        assertEquals(1, sel.count(), "1% of 45 -> at least 1");
        ComboSelector.Selection zero = ComboSelector.plan(10, 2, 0, new java.util.Random(1));
        assertEquals(0, zero.count(), "0% -> none");
        assertTrue(zero.materialize().isEmpty(), "empty materialize");
    }

    static void testPlanEnumerationOrder() {
        ComboSelector.Selection sel = ComboSelector.plan(6, 3, 100, new java.util.Random(1));
        List<int[]> combos = sel.materialize();
        assertEquals(20, combos.size(), "C(6,3)=20");
        assertEquals(sel.count(), combos.size(), "count matches materialized");
        assertEquals("0,1,2", join(combos.get(0)), "first combo");
        assertEquals("3,4,5", join(combos.get(19)), "last combo");
        for (int i = 1; i < combos.size(); i++) {
            assertTrue(lexLess(combos.get(i - 1), combos.get(i)), "strict lexicographic at " + i);
        }
    }

    static void testPlanInvalidArgs() {
        assertThrows(() -> ComboSelector.plan(0, 1, 100, new java.util.Random(1)), "unitCount=0");
        assertThrows(() -> ComboSelector.plan(5, 0, 100, new java.util.Random(1)), "deleteCount=0");
        assertThrows(() -> ComboSelector.plan(5, 6, 100, new java.util.Random(1)), "deleteCount>unitCount");
        assertThrows(() -> ComboSelector.unrank(ComboSelector.combinationCount(8, 3), 8, 3), "unrank out of range");
        assertThrows(() -> ComboSelector.unrank(-1, 8, 3), "unrank negative");
    }

    static void testPlanSamplingCap() {
        assertThrows(() -> ComboSelector.plan(50, 25, 100, new java.util.Random(1)), "combos over cap");
        assertThrows(() -> ComboSelector.plan(50, 25, 50, new java.util.Random(1)), "sampled combos over cap");
        ComboSelector.Selection allowed = ComboSelector.plan(63, 4, 100, new java.util.Random(1));
        assertEquals(595665L, allowed.total(), "under cap allowed");
        assertThrows(() -> ComboSelector.plan(63, 5, 100, new java.util.Random(1)), "C(63,5) over cap");
    }

    static boolean lexLess(int[] a, int[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return a[i] < b[i];
            }
        }
        return false;
    }

    static void assertThrows(Runnable r, String msg) {
        try {
            r.run();
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError(msg + " expected IllegalArgumentException");
    }

    static String join(int[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int v : arr) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(v);
        }
        return sb.toString();
    }
```

- [ ] **Step 2: 创建 ComboSelector.java**

完整文件内容：

```java
package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** 删除组合的枚举与随机采样。 */
public final class ComboSelector {

    /** 单次分析的组合数硬上限，超过时拒绝（防止内存耗尽）。 */
    public static final long MAX_SAMPLES = 1_000_000L;

    private ComboSelector() {}

    /** C(n, k)，参数非法返回 0；用 double 中间值避免 long 溢出，结果在 2^53 以内精确，更大时为近似值或饱和。 */
    public static long combinationCount(int n, int k) {
        if (n < 0 || k < 0 || k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        k = Math.min(k, n - k);
        double result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - k + i) / i;
            if (result > 9.22e18) {
                return Long.MAX_VALUE;
            }
        }
        return Math.round(result);
    }

    /** 按探索率计算采样数（不含全遍历判定）。0% 返回 0；非 0% 至少 1 个。 */
    public static long plannedCount(long total, int explorationPercent) {
        int m = Math.max(0, Math.min(100, explorationPercent));
        if (m == 0 || total <= 0) {
            return 0;
        }
        if (m == 100 || total == 1) {
            return total;
        }
        return Math.min(total, Math.max(1L, Math.round(total * (m / 100.0))));
    }

    /** 组合的字典序排名；comb 需严格升序。 */
    public static long rank(int[] comb, int n) {
        long rank = 0;
        int start = 0;
        for (int i = 0; i < comb.length; i++) {
            for (int c = start; c < comb[i]; c++) {
                rank += combinationCount(n - c - 1, comb.length - i - 1);
            }
            start = comb[i] + 1;
        }
        return rank;
    }

    /** 字典序排名反解为组合；要求 0 <= rank < C(n,k)。 */
    public static int[] unrank(long rank, int n, int k) {
        if (k < 0 || k > n || rank < 0 || rank >= combinationCount(n, k)) {
            throw new IllegalArgumentException("组合排名越界: " + rank + " (n=" + n + ", k=" + k + ")");
        }
        int[] comb = new int[k];
        int start = 0;
        for (int i = 0; i < k; i++) {
            long count;
            while ((count = combinationCount(n - start - 1, k - i - 1)) <= rank) {
                rank -= count;
                start++;
            }
            comb[i] = start;
            start++;
        }
        return comb;
    }

    /** 采样方案：ranked=true 时 ranks 为按字典序排名的组合；否则按字典序全遍历。 */
    public record Selection(int unitCount, int deleteCount, long total, int count,
                            long[] ranks, boolean ranked) {

        /** 展开组合列表。 */
        public List<int[]> materialize() {
            if (!ranked) {
                if (count > MAX_SAMPLES) {
                    throw new IllegalStateException("组合数过大，无法展开");
                }
                return enumerate(unitCount, deleteCount, count);
            }
            List<int[]> list = new ArrayList<>(ranks.length);
            for (long r : ranks) {
                list.add(unrank(r, unitCount, deleteCount));
            }
            return list;
        }
    }

    /** 生成采样方案。探索率 100% 或采样数达到总数时为全遍历。 */
    public static Selection plan(int unitCount, int deleteCount, int explorationPercent, Random rng) {
        if (unitCount < 1) {
            throw new IllegalArgumentException("没有可删除的单位");
        }
        if (deleteCount < 1 || deleteCount > unitCount) {
            throw new IllegalArgumentException("删除数必须在 1~" + unitCount + " 之间");
        }
        long total = combinationCount(unitCount, deleteCount);
        long k = plannedCount(total, explorationPercent);
        if (k > MAX_SAMPLES) {
            throw new IllegalArgumentException("组合数过大（超过 " + MAX_SAMPLES + " 个），请降低探索率或减少删除数");
        }
        if (k <= 0) {
            return new Selection(unitCount, deleteCount, total, 0, new long[0], true);
        }
        if (k >= total) {
            return new Selection(unitCount, deleteCount, total,
                    (int) Math.min(total, Integer.MAX_VALUE), new long[0], false);
        }
        int kInt = (int) Math.min(k, Integer.MAX_VALUE);
        int capacity = (int) Math.min((long) kInt * 2, 1 << 20);
        Set<Long> chosen = new HashSet<>(capacity);
        for (long j = total - kInt; j < total; j++) {
            long t = Math.floorMod(rng.nextLong(), j + 1);
            if (!chosen.add(t)) {
                chosen.add(j);
            }
        }
        long[] ranks = new long[chosen.size()];
        int idx = 0;
        for (long r : chosen) {
            ranks[idx++] = r;
        }
        Arrays.sort(ranks);
        return new Selection(unitCount, deleteCount, total, ranks.length, ranks, true);
    }

    private static List<int[]> enumerate(int n, int k, int limit) {
        List<int[]> list = new ArrayList<>(Math.min(limit, 1 << 16));
        int[] comb = new int[k];
        for (int i = 0; i < k; i++) {
            comb[i] = i;
        }
        while (list.size() < limit) {
            list.add(comb.clone());
            int i = k - 1;
            while (i >= 0 && comb[i] == n - k + i) {
                i--;
            }
            if (i < 0) {
                break;
            }
            comb[i]++;
            for (int j = i + 1; j < k; j++) {
                comb[j] = comb[j - 1] + 1;
            }
        }
        return list;
    }
}
```

- [ ] **Step 3: 验证**

依次运行「编译主代码」「编译自测」「运行自测」。
预期：12 个测试全部 PASS，`TOTAL pass=12 fail=0`。

---

### Task 3: FortTrimmer（CompiledFort 裁剪）

**Files:**
- Create: `src/main/java/org/example/FortTrimmer.java`
- Modify: `C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\src\org\example\SelfTest.java`（追加测试）

- [ ] **Step 1: 追加失败测试**

```java
    static void testTrimMatchesRecompile() {
        String text = synthFormation("A", new Unit(25, 100, 200, 0),
                new Unit(1, 150, 100, 0), new Unit(3, 200, 150, 0));
        CompiledFort src = Setting.parseForts(text).get(0);
        CompiledFort trimmed = FortTrimmer.trim(src, new int[]{1});
        assertEquals(2, trimmed.unitCount, "trimmed unitCount");
        assertEquals(src.baseSeed, trimmed.baseSeed, "baseSeed preserved");

        Formation f = Formation.decode(text);
        List<Unit> kept = new ArrayList<>();
        kept.add(f.units.get(0));
        kept.add(f.units.get(1));
        kept.add(f.units.get(3));
        CompiledFort expect = Setting.parseForts(new Formation(f.name, kept).encode()).get(0);

        for (int i = 0; i < expect.unitCount; i++) {
            assertEquals(expect.type[i], trimmed.type[i], "type[" + i + "]");
            assertEquals(expect.x[i], trimmed.x[i], "x[" + i + "]");
            assertEquals(expect.y[i], trimmed.y[i], "y[" + i + "]");
            assertEquals(expect.r[i], trimmed.r[i], "r[" + i + "]");
            assertEquals(expect.seed[i], trimmed.seed[i], "seed[" + i + "]");
        }
    }

    static void testTrimBoundaries() {
        String text = synthFormation("A", new Unit(25, 100, 200, 0),
                new Unit(1, 150, 100, 0), new Unit(3, 200, 150, 0));
        CompiledFort src = Setting.parseForts(text).get(0);

        CompiledFort copy = FortTrimmer.trim(src, new int[0]);
        assertEquals(src.unitCount, copy.unitCount, "empty removal unitCount");
        assertTrue(copy.type != src.type, "arrays not shared");
        for (int i = 0; i < src.unitCount; i++) {
            assertEquals(src.type[i], copy.type[i], "copy type[" + i + "]");
        }

        CompiledFort noFirst = FortTrimmer.trim(src, new int[]{0});
        assertEquals(src.type[1], noFirst.type[0], "delete first");

        CompiledFort noLast = FortTrimmer.trim(src, new int[]{2});
        assertEquals(src.type[1], noLast.type[1], "delete last keeps middle");

        CompiledFort dedup = FortTrimmer.trim(src, new int[]{1, 1, 0, 0});
        assertEquals(1, dedup.unitCount, "duplicate indices dedup");
        assertEquals(src.type[2], dedup.type[0], "dedup keeps third unit");

        CompiledFort empty = FortTrimmer.trim(src, new int[]{0, 1, 2});
        assertEquals(0, empty.unitCount, "delete all");
        assertEquals(src.baseSeed, empty.baseSeed, "delete all keeps baseSeed");

        assertThrows(() -> FortTrimmer.trim(src, new int[]{-1}), "negative index");
        assertThrows(() -> FortTrimmer.trim(src, new int[]{src.unitCount}), "out of range");
        assertThrows(() -> FortTrimmer.trim(src, new int[]{Integer.MAX_VALUE}), "max index");
    }
```

- [ ] **Step 2: 编译自测，确认失败**

预期：编译错误，提示 `找不到符号 ... FortTrimmer`。

- [ ] **Step 3: 创建 FortTrimmer.java**

完整文件内容：

```java
package org.example;

/** 从 CompiledFort 中裁剪掉指定索引的单位，生成新的 CompiledFort。 */
public final class FortTrimmer {

    private FortTrimmer() {}

    /**
     * 删除指定索引的单位，返回新的 CompiledFort（核心字段、单位顺序与随机种子保持不变，
     * 返回的数组不与 src 共享引用）。
     *
     * @param src           源阵型
     * @param removeIndices 要删除的单位索引（0 基，对应 src.type 数组）；顺序任意，重复自动去重
     * @throws IllegalArgumentException 索引越界时
     */
    public static CompiledFort trim(CompiledFort src, int[] removeIndices) {
        boolean[] remove = new boolean[src.unitCount];
        for (int idx : removeIndices) {
            if (idx < 0 || idx >= src.unitCount) {
                throw new IllegalArgumentException("单位索引越界: " + idx);
            }
            remove[idx] = true;
        }
        return trim(src, remove);
    }

    private static CompiledFort trim(CompiledFort src, boolean[] remove) {
        int kept = 0;
        for (int i = 0; i < src.unitCount; i++) {
            if (!remove[i]) {
                kept++;
            }
        }
        int[] type = new int[kept];
        int[] x = new int[kept];
        int[] y = new int[kept];
        int[] r = new int[kept];
        int[] seed = new int[kept];
        int j = 0;
        for (int i = 0; i < src.unitCount; i++) {
            if (remove[i]) {
                continue;
            }
            type[j] = src.type[i];
            x[j] = src.x[i];
            y[j] = src.y[i];
            r[j] = src.r[i];
            seed[j] = src.seed[i];
            j++;
        }
        return new CompiledFort(src.name, src.coreType, src.coreX, src.coreY,
                src.baseSeed, kept, type, x, y, r, seed);
    }
}
```

- [ ] **Step 4: 验证**

依次运行「编译主代码」「编译自测」「运行自测」。
预期：14 个测试全部 PASS，`TOTAL pass=14 fail=0`。

---

### Task 4: ContributionAnalyzer（核心逻辑）

**Files:**
- Create: `src/main/java/org/example/ContributionAnalyzer.java`
- Modify: `C:\Users\cain\AppData\Local\Temp\opencode\gekitotsu_selftest\src\org\example\SelfTest.java`（追加测试）

- [ ] **Step 1: 追加失败测试**

```java
    static void testRemoveUnitsFromCode() {
        String text = synthFormation("P", new Unit(25, 100, 200, 0), new Unit(1, 150, 100, 0));
        Formation before = Formation.decode(text);
        String removed = ContributionAnalyzer.removeUnitsFromCode(text, new int[]{0});
        Formation after = Formation.decode(removed);
        assertEquals(before.units.size() - 1, after.units.size(), "one unit removed");
        assertEquals(before.name, after.name, "name kept");
        assertEquals(before.units.get(2).id, after.units.get(1).id, "remaining unit kept");
        assertThrows(() -> ContributionAnalyzer.removeUnitsFromCode(text, new int[]{-1}), "negative index");
    }

    static void testAnalyzeBaselineAndCombos() throws Exception {
        Main.MAX_FRAME_LIMIT = 65536;
        String playerText = decisivePlayerText();
        String evalText = decisiveEvalText();
        ContributionAnalyzer.Params params = new ContributionAnalyzer.Params(
                2, 100, 2, ContributionAnalyzer.SortKey.DELTA, ContributionAnalyzer.SortOrder.DESC);
        ContributionAnalyzer.Report report =
                ContributionAnalyzer.analyze(playerText, evalText, params, null, () -> false);

        assertEquals(3, report.unitCount(), "unitCount");
        assertEquals(3, report.sampledCombos(), "sampled combos");
        assertEquals(3L, report.totalCombos(), "total combos");
        assertTrue(!Double.isNaN(report.baseline().winRate()), "baseline winRate not NaN");
        assertEquals(3, report.units().size(), "unit summary size");
        assertTrue(report.combos().stream().anyMatch(cr -> cr.delta() < -1e-9), "at least one negative delta");

        CompiledFort player = Setting.parseForts(playerText).get(0);
        List<CompiledFort> evals = Setting.parseForts(evalText);
        int[] base = new int[4];
        for (CompiledFort e : evals) {
            base[bucketOf(new GameTask().run_single(player, e).status)]++;
        }
        assertEquals(base[0], report.baseline().win(), "baseline win");
        assertEquals(base[1], report.baseline().lose(), "baseline lose");
        assertEquals(base[2], report.baseline().draw(), "baseline draw");
        assertEquals(base[3], report.baseline().timeout(), "baseline timeout");
        assertEqualsDouble(expectedWinRate(base), report.baseline().winRate(), 1e-9, "baseline winRate");

        for (ContributionAnalyzer.ComboResult cr : report.combos()) {
            String removedText = ContributionAnalyzer.removeUnitsFromCode(playerText, cr.unitIndices());
            CompiledFort trimmed = Setting.parseForts(removedText).get(0);
            int[] counts = new int[4];
            for (CompiledFort e : evals) {
                counts[bucketOf(new GameTask().run_single(trimmed, e).status)]++;
            }
            String msg = "combo " + java.util.Arrays.toString(cr.unitIndices());
            assertEquals(counts[0], cr.win(), msg + " win");
            assertEquals(counts[1], cr.lose(), msg + " lose");
            assertEquals(counts[2], cr.draw(), msg + " draw");
            assertEquals(counts[3], cr.timeout(), msg + " timeout");
            assertEqualsDouble(cr.winRate() - report.baseline().winRate(), cr.delta(), 1e-9, msg + " delta");
            long cost = 0;
            for (int idx : cr.unitIndices()) {
                cost += report.refs().get(idx).cost();
            }
            assertEquals(cost, cr.removedCost(), msg + " removedCost");
            assertEqualsDouble(cr.delta() / cr.removedCost(), cr.deltaPerCost(), 1e-9, msg + " deltaPerCost");
        }

        for (ContributionAnalyzer.UnitSummary us : report.units()) {
            int count = 0;
            double sum = 0;
            double perCostSum = 0;
            for (ContributionAnalyzer.ComboResult cr : report.combos()) {
                for (int idx : cr.unitIndices()) {
                    if (idx == us.unit().index()) {
                        count++;
                        sum += cr.delta();
                        perCostSum += cr.deltaPerCost();
                    }
                }
            }
            assertEquals(count, us.appearances(), "appearances " + us.unit().index());
            assertEqualsDouble(sum / count, us.avgDelta(), 1e-9, "avgDelta " + us.unit().index());
            assertEqualsDouble(perCostSum / count, us.avgDeltaPerCost(), 1e-9,
                    "avgDeltaPerCost " + us.unit().index());
        }
    }

    static void testUnitPerCostSemantics() throws Exception {
        Main.MAX_FRAME_LIMIT = 65536;
        ContributionAnalyzer.Report n1 = ContributionAnalyzer.analyze(decisivePlayerText(), decisiveEvalText(),
                new ContributionAnalyzer.Params(1, 100, 2,
                        ContributionAnalyzer.SortKey.DELTA, ContributionAnalyzer.SortOrder.DESC),
                null, () -> false);
        for (ContributionAnalyzer.UnitSummary us : n1.units()) {
            assertEqualsDouble(us.avgDelta() / us.unit().cost(), us.avgDeltaPerCost(), 1e-9,
                    "n=1 perCost equals avgDelta/cost " + us.unit().index());
        }
        ContributionAnalyzer.Report n2 = ContributionAnalyzer.analyze(decisivePlayerText(), decisiveEvalText(),
                new ContributionAnalyzer.Params(2, 100, 2,
                        ContributionAnalyzer.SortKey.DELTA, ContributionAnalyzer.SortOrder.DESC),
                null, () -> false);
        assertEquals(3, n2.units().size(), "n=2 unit summary size");
        for (ContributionAnalyzer.UnitSummary us : n2.units()) {
            double perCostSum = 0;
            int count = 0;
            for (ContributionAnalyzer.ComboResult cr : n2.combos()) {
                for (int idx : cr.unitIndices()) {
                    if (idx == us.unit().index()) {
                        perCostSum += cr.deltaPerCost();
                        count++;
                    }
                }
            }
            assertEqualsDouble(perCostSum / count, us.avgDeltaPerCost(), 1e-9,
                    "n=2 perCost is avg of combo perCost " + us.unit().index());
        }
    }

    static void testAnalyzeSorting() throws Exception {
        Main.MAX_FRAME_LIMIT = 65536;
        String playerText = decisivePlayerText();
        String evalText = decisiveEvalText();

        ContributionAnalyzer.Report desc = ContributionAnalyzer.analyze(playerText, evalText,
                new ContributionAnalyzer.Params(1, 100, 2,
                        ContributionAnalyzer.SortKey.DELTA, ContributionAnalyzer.SortOrder.DESC),
                null, () -> false);
        int descStrict = 0;
        for (int i = 1; i < desc.combos().size(); i++) {
            double prev = desc.combos().get(i - 1).delta();
            double cur = desc.combos().get(i).delta();
            assertTrue(prev >= cur, "delta descending at " + i);
            if (prev > cur) {
                descStrict++;
            }
        }
        assertTrue(descStrict > 0, "delta descending has strict pair");

        ContributionAnalyzer.Report asc = ContributionAnalyzer.analyze(playerText, evalText,
                new ContributionAnalyzer.Params(1, 100, 2,
                        ContributionAnalyzer.SortKey.PER_COST, ContributionAnalyzer.SortOrder.ASC),
                null, () -> false);
        int ascStrict = 0;
        for (int i = 1; i < asc.combos().size(); i++) {
            double prev = asc.combos().get(i - 1).deltaPerCost();
            double cur = asc.combos().get(i).deltaPerCost();
            assertTrue(prev <= cur, "perCost ascending at " + i);
            if (prev < cur) {
                ascStrict++;
            }
        }
        assertTrue(ascStrict > 0, "perCost ascending has strict pair");

        int unitStrict = 0;
        for (int i = 1; i < asc.units().size(); i++) {
            double prev = asc.units().get(i - 1).avgDeltaPerCost();
            double cur = asc.units().get(i).avgDeltaPerCost();
            assertTrue(prev <= cur, "unit perCost ascending at " + i);
            if (prev < cur) {
                unitStrict++;
            }
        }
        assertTrue(unitStrict > 0, "unit perCost ascending has strict pair");
        assertEquals(3, desc.units().size(), "desc unit count");
        assertEquals(0, desc.units().get(2).unit().index(), "most harmful unit last in DESC");
    }

    static void testAnalyzeCancel() throws Exception {
        Main.MAX_FRAME_LIMIT = 65536;
        ContributionAnalyzer.Report report = ContributionAnalyzer.analyze(decisivePlayerText(), decisiveEvalText(),
                new ContributionAnalyzer.Params(1, 100, 2,
                        ContributionAnalyzer.SortKey.DELTA, ContributionAnalyzer.SortOrder.DESC),
                null, () -> true);
        assertTrue(report == null, "cancelled -> null");
    }

    static void testAnalyzeCallbacks() throws Exception {
        Main.MAX_FRAME_LIMIT = 65536;
        int[] baselineCalls = {0};
        int[] progressCalls = {0};
        long[] lastTotal = {0};
        int[] lastDone = {-1};
        boolean[] validPhase = {true};
        ContributionAnalyzer.Report report = ContributionAnalyzer.analyze(decisivePlayerText(), decisiveEvalText(),
                new ContributionAnalyzer.Params(1, 100, 2,
                        ContributionAnalyzer.SortKey.DELTA, ContributionAnalyzer.SortOrder.DESC),
                new ContributionAnalyzer.ProgressListener() {
                    @Override
                    public void onProgress(int done, long total, String phase) {
                        progressCalls[0]++;
                        lastTotal[0] = total;
                        lastDone[0] = done;
                        if (!"基线".equals(phase) && !"组合".equals(phase)) {
                            validPhase[0] = false;
                        }
                    }

                    @Override
                    public void onBaseline(ContributionAnalyzer.Baseline baseline) {
                        baselineCalls[0]++;
                    }
                }, () -> false);
        assertEquals(1, baselineCalls[0], "onBaseline exactly once");
        assertTrue(progressCalls[0] >= 1, "progress called");
        assertEquals((long) (report.sampledCombos() + 1) * 4, lastTotal[0], "last total");
        assertTrue(lastDone[0] == lastTotal[0], "last done equals total");
        assertTrue(validPhase[0], "phase values valid");
    }

    static void testRemoveUnitsFromCodeHp() {
        Unit u1 = new Unit(25, 100, 200, 0);
        u1.hp = 5;
        Unit u2 = new Unit(1, 150, 100, 0);
        u2.hp = 7;
        Unit u3 = new Unit(3, 200, 150, 0);
        u3.hp = 9;
        String text = synthFormation("H", u1, u2, u3);
        assertTrue(text.contains("#"), "hp suffix present");
        String removed = ContributionAnalyzer.removeUnitsFromCode(text, new int[]{0});
        Formation after = Formation.decode(removed);
        assertEquals(3, after.units.size(), "core + 2 units");
        assertEquals(7, after.units.get(1).hp, "u2 hp kept");
        assertEquals(9, after.units.get(2).hp, "u3 hp kept");
    }

    static void testRemoveUnitsFromCodeUnnamedHp() {
        List<Unit> us = new ArrayList<>();
        us.add(new Unit(0, 30, 48, 0));
        Unit u1 = new Unit(25, 100, 200, 0);
        u1.hp = 5;
        Unit u2 = new Unit(1, 150, 100, 0);
        u2.hp = 7;
        Unit u3 = new Unit(3, 200, 150, 0);
        u3.hp = 9;
        us.add(u1);
        us.add(u2);
        us.add(u3);
        String text = new Formation("", us).encode() + new Formation("", us).encodeHp();
        assertTrue(text.contains("#"), "unnamed hp input");
        String removed = ContributionAnalyzer.removeUnitsFromCode(text, new int[]{0});
        Formation after = Formation.decode(removed);
        assertEquals(3, after.units.size(), "core + 2 units");
        assertEquals(7, after.units.get(1).hp, "u2 hp kept");
        assertEquals(9, after.units.get(2).hp, "u3 hp kept");
    }

    static String decisivePlayerText() {
        return synthFormation("P", new Unit(41, 100, 150, 0),
                new Unit(1, 150, 100, 0), new Unit(25, 200, 200, 0));
    }

    static String decisiveEvalText() {
        return synthFormation("E0")
                + "/" + synthFormation("E1", new Unit(1, 150, 100, 0))
                + "/" + synthFormation("E2", new Unit(41, 100, 150, 0))
                + "/" + synthFormation("E3", new Unit(1, 80, 150, 0), new Unit(1, 130, 150, 0),
                        new Unit(1, 180, 150, 0), new Unit(1, 230, 150, 0));
    }
```

- [ ] **Step 2: 编译自测，确认失败**

预期：编译错误，提示 `找不到符号 ... ContributionAnalyzer`。

- [ ] **Step 3: 创建 ContributionAnalyzer.java**

完整文件内容：

```java
package org.example;

import org.example.GUI.Formation;
import org.example.GUI.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

/** 贡献分析：批量对战计算“删除 n 个单位”前后的胜率变化。 */
public final class ContributionAnalyzer {

    public enum SortKey { DELTA, PER_COST }

    public enum SortOrder { DESC, ASC }

    public record Params(int deleteCount, int explorationPercent, int threads,
                         SortKey sortKey, SortOrder order) {}

    public record UnitRef(int index, int type, int x, int y, int cost, String name) {}

    public record ComboResult(int[] unitIndices, long removedCost,
                              int win, int lose, int draw, int timeout,
                              double winRate, double delta, double deltaPerCost) {}

    public record UnitSummary(UnitRef unit, int appearances,
                              double avgDelta, double avgDeltaPerCost) {}

    public record Baseline(int win, int lose, int draw, int timeout, double winRate) {}

    public record Report(int unitCount, long totalCost, List<UnitRef> refs, Baseline baseline,
                         int sampledCombos, long totalCombos,
                         List<ComboResult> combos, List<UnitSummary> units,
                         int battleErrors, long elapsedMs) {}

    /** 进度回调。注意：回调在 analyze 的调用线程（后台分析线程）触发，不是 EDT，GUI 侧需自行切换线程。 */
    public interface ProgressListener {
        void onProgress(int doneBattles, long totalBattles, String phase);

        default void onBaseline(Baseline baseline) {
        }
    }

    private ContributionAnalyzer() {}

    /**
     * 执行贡献分析。
     *
     * @return 分析结果；取消（cancelled 为 true 或线程中断）时返回 null
     * @throws Exception 输入解析/参数错误，或对战任务抛出异常时
     */
    public static Report analyze(String playerText, String evalText, Params params,
                                 ProgressListener listener, BooleanSupplier cancelled) throws Exception {
        long startNanos = System.nanoTime();

        List<CompiledFort> playerForts = Setting.parseForts(playerText);
        if (playerForts.size() != 1) {
            throw new IllegalArgumentException("玩家阵型必须且只能包含一个阵型");
        }
        CompiledFort player = playerForts.get(0);
        Formation formation = Formation.decode(playerText.trim());
        int unitCount = player.unitCount;
        if (formation.units.size() - 1 != unitCount) {
            throw new IllegalArgumentException("阵型文本与代码解析结果不一致");
        }

        List<CompiledFort> evalForts = Setting.parseForts(evalText);
        if (evalForts.isEmpty()) {
            throw new IllegalArgumentException("评判阵集为空或格式错误");
        }

        int n = params.deleteCount();
        if (n < 1 || n > unitCount) {
            throw new IllegalArgumentException("删除数必须在 1~" + unitCount + " 之间");
        }

        List<UnitRef> refs = new ArrayList<>();
        long totalCost = 0;
        for (int i = 0; i < unitCount; i++) {
            int type = player.type[i];
            Unit.Info info = (type >= 0 && type < Unit.infos.length)
                    ? Unit.infos[type] : Unit.infos[Unit.infos.length - 1];
            int cost = info.cost() > 0 ? info.cost() : 1;
            refs.add(new UnitRef(i, type, player.x[i], player.y[i], cost, info.name()));
            totalCost += cost;
        }

        ComboSelector.Selection selection =
                ComboSelector.plan(unitCount, n, params.explorationPercent(), new Random());
        List<int[]> combos = selection.materialize();
        int k = combos.size();
        int m = evalForts.size();
        int threads = Math.max(1, Math.min(256, params.threads()));
        long totalBattles = (long) (k + 1) * m;
        int window = Math.max(64, threads * 4);

        int[] baselineCounts = new int[4];
        int[] comboCounts = new int[k * 4];
        Map<Integer, CompiledFort> activeForts = new HashMap<>();
        int[] pendingBattles = new int[k];
        int errors = 0;
        int battlesDone = 0;
        int baselineDone = 0;
        boolean baselineReported = false;
        long lastNotify = 0;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CompletionService<int[]> completion = new ExecutorCompletionService<>(pool);
            int next = 0;
            int inFlight = 0;
            while ((next < totalBattles || inFlight > 0) && !cancelled.getAsBoolean()) {
                while (inFlight < window && next < totalBattles) {
                    int job = next++;
                    int comboIdx = job < m ? -1 : (job - m) / m;
                    int oppIdx = job < m ? job : (job - m) % m;
                    // 惰性裁剪：只保留窗口内仍有在途对战的组合，避免一次性持有全部裁剪结果
                    final CompiledFort p1;
                    if (comboIdx < 0) {
                        p1 = player;
                    } else {
                        CompiledFort active = activeForts.get(comboIdx);
                        if (active == null) {
                            active = FortTrimmer.trim(player, combos.get(comboIdx));
                            activeForts.put(comboIdx, active);
                        }
                        p1 = active;
                        pendingBattles[comboIdx]++;
                    }
                    CompiledFort p2 = evalForts.get(oppIdx);
                    completion.submit(() -> {
                        try {
                            Result r = new GameTask().run_single(p1, p2);
                            return new int[]{comboIdx, r.status};
                        } catch (Exception t) {
                            return new int[]{comboIdx, -2};
                        }
                    });
                    inFlight++;
                }
                if (inFlight == 0) {
                    break;
                }
                Future<int[]> future = completion.take();
                int[] res = future.get();
                inFlight--;
                battlesDone++;
                int bucket = bucketOf(res[1]);
                if (res[1] == -2) {
                    errors++;
                }
                if (res[0] < 0) {
                    baselineCounts[bucket]++;
                    baselineDone++;
                    if (!baselineReported && baselineDone == m) {
                        baselineReported = true;
                        if (listener != null) {
                            listener.onBaseline(toBaseline(baselineCounts));
                        }
                    }
                } else {
                    comboCounts[res[0] * 4 + bucket]++;
                    if (--pendingBattles[res[0]] == 0) {
                        activeForts.remove(res[0]);
                    }
                }
                long now = System.nanoTime();
                if (listener != null && (now - lastNotify > 100_000_000L || battlesDone == totalBattles)) {
                    lastNotify = now;
                    listener.onProgress(battlesDone, totalBattles, res[0] < 0 ? "基线" : "组合");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            // 已提交的对战跑完即止（GameTask 不响应中断）
            pool.shutdownNow();
        }

        if (cancelled.getAsBoolean() || baselineDone < m) {
            return null;
        }

        Baseline baseline = toBaseline(baselineCounts);
        double baseRate = baseline.winRate();

        List<ComboResult> comboResults = new ArrayList<>(k);
        for (int ci = 0; ci < k; ci++) {
            int base = ci * 4;
            int win = comboCounts[base];
            int lose = comboCounts[base + 1];
            int draw = comboCounts[base + 2];
            int timeout = comboCounts[base + 3];
            long removedCost = 0;
            for (int idx : combos.get(ci)) {
                removedCost += refs.get(idx).cost();
            }
            double comboRate = winRate(win, lose, draw);
            double delta = comboRate - baseRate;
            comboResults.add(new ComboResult(combos.get(ci), removedCost,
                    win, lose, draw, timeout, comboRate, delta, delta / removedCost));
        }
        sortCombos(comboResults, params.sortKey(), params.order());

        double[] sumDelta = new double[unitCount];
        double[] sumDeltaPerCost = new double[unitCount];
        int[] appearances = new int[unitCount];
        for (ComboResult cr : comboResults) {
            for (int idx : cr.unitIndices()) {
                sumDelta[idx] += cr.delta();
                sumDeltaPerCost[idx] += cr.deltaPerCost();
                appearances[idx]++;
            }
        }
        List<UnitSummary> unitSummaries = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            if (appearances[i] == 0) {
                continue;
            }
            double avgDelta = sumDelta[i] / appearances[i];
            // 单位每费变化 = 该单位所有被采样组合的「组合每费变化」平均值（而非 avgDelta/自身cost）
            unitSummaries.add(new UnitSummary(refs.get(i), appearances[i],
                    avgDelta, sumDeltaPerCost[i] / appearances[i]));
        }
        sortUnits(unitSummaries, params.sortKey(), params.order());

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        return new Report(unitCount, totalCost, refs, baseline, k, selection.total(),
                comboResults, unitSummaries, errors, elapsedMs);
    }

    /** 生成删除指定单位后的阵型代码（name&code#hp），索引 0 对应第一个非核心单位。 */
    public static String removeUnitsFromCode(String playerText, int[] unitIndices) {
        Formation formation = Formation.decode(playerText.trim());
        boolean[] remove = new boolean[formation.units.size()];
        for (int idx : unitIndices) {
            if (idx < 0 || idx >= formation.units.size() - 1) {
                throw new IllegalArgumentException("单位索引越界: " + idx);
            }
            remove[idx + 1] = true;
        }
        List<Unit> kept = new ArrayList<>();
        for (int i = 0; i < formation.units.size(); i++) {
            if (!remove[i]) {
                kept.add(formation.units.get(i));
            }
        }
        Formation result = new Formation(formation.name, kept);
        String encoded = result.encode();
        if (formation.name == null || formation.name.isEmpty()) {
            return encoded + result.encodeHp();
        }
        return encoded;
    }

    private static int bucketOf(int status) {
        return status == 1 ? 0 : status == 2 ? 1 : status == 0 ? 2 : 3;
    }

    private static double winRate(int win, int lose, int draw) {
        int decisive = win + lose + draw;
        return decisive > 0 ? (2.0 * win + draw) * 50.0 / decisive : Double.NaN;
    }

    private static Baseline toBaseline(int[] counts) {
        return new Baseline(counts[0], counts[1], counts[2], counts[3],
                winRate(counts[0], counts[1], counts[2]));
    }

    private static void sortCombos(List<ComboResult> list, SortKey key, SortOrder order) {
        Comparator<ComboResult> cmp = (a, b) -> compareValues(
                key == SortKey.DELTA ? a.delta() : a.deltaPerCost(),
                key == SortKey.DELTA ? b.delta() : b.deltaPerCost(),
                order);
        list.sort(cmp.thenComparing(ComboResult::unitIndices, Arrays::compare));
    }

    private static void sortUnits(List<UnitSummary> list, SortKey key, SortOrder order) {
        Comparator<UnitSummary> cmp = (a, b) -> compareValues(
                key == SortKey.DELTA ? a.avgDelta() : a.avgDeltaPerCost(),
                key == SortKey.DELTA ? b.avgDelta() : b.avgDeltaPerCost(),
                order);
        list.sort(cmp.thenComparingInt(s -> s.unit().index()));
    }

    /** NaN 永远排在最后；其他值按指定方向比较。 */
    private static int compareValues(double a, double b, SortOrder order) {
        boolean na = Double.isNaN(a);
        boolean nb = Double.isNaN(b);
        if (na || nb) {
            return na == nb ? 0 : (na ? 1 : -1);
        }
        int c = Double.compare(b, a);
        return order == SortOrder.DESC ? c : -c;
    }
}
```

- [ ] **Step 4: 验证**

依次运行「编译主代码」「编译自测」「运行自测」。
预期：22 个测试全部 PASS，`TOTAL pass=22 fail=0`（含分析器与真实 GameTask 的逐组合对照）。

---

### Task 5: ContributionTab（GUI）

**Files:**
- Create: `src/main/java/org/example/GUI/WrapLayout.java`
- Create: `src/main/java/org/example/GUI/ContributionTab.java`

- [ ] **Step 1: 创建 WrapLayout.java 与 ContributionTab.java**

先创建 `WrapLayout.java`，完整文件内容：

```java
package org.example.GUI;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * 可换行的 FlowLayout：按容器当前宽度计算多行高度，
 * 实际高度不足时自动增高容器，避免窄窗口下第二行内容被裁掉。
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= getHgap() + 1;
        if (minimum.width < 0) {
            minimum.width = 0;
        }
        return minimum;
    }

    @Override
    public void layoutContainer(Container target) {
        super.layoutContainer(target);
        Dimension preferred = preferredLayoutSize(target);
        if (target.getHeight() != preferred.height) {
            target.setSize(target.getWidth(), preferred.height);
            Container parent = target.getParent();
            if (parent != null) {
                parent.revalidate();
            }
        }
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }
            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + hgap * 2;
            int maxWidth = targetWidth - horizontalInsetsAndGap;
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            int members = target.getComponentCount();
            for (int i = 0; i < members; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowWidth + d.width > maxWidth) {
                    addRow(dim, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) {
                    rowWidth += hgap;
                }
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(dim, rowWidth, rowHeight);
            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;
            Container scrollPane = javax.swing.SwingUtilities.getAncestorOfClass(
                    javax.swing.JScrollPane.class, target);
            if (scrollPane != null && target.isValid()) {
                dim.width -= getHgap() + 1;
            }
            return dim;
        }
    }

    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) {
            dim.height += getVgap();
        }
        dim.height += rowHeight;
    }
}
```

再创建 `ContributionTab.java`，完整文件内容：

```java
package org.example.GUI;

import org.example.CompiledFort;
import org.example.ComboSelector;
import org.example.ContributionAnalyzer;
import org.example.Main;
import org.example.Setting;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

/** 贡献分析标签页：批量对战找出对胜率贡献小的单位。 */
public class ContributionTab extends JPanel {

    // —— 输入 ——
    private final JTextField playerField = new JTextField();
    private final FixedJTextArea evalArea = new FixedJTextArea();
    private final JLabel playerInfoLabel = new JLabel(" ");
    private final JLabel evalInfoLabel = new JLabel(" ");

    // —— 参数 ——
    private final JSpinner exploreSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 100, 5));
    private final JSpinner deleteSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JSpinner threadSpinner = new JSpinner(
            new SpinnerNumberModel(Math.min(256, Math.max(1, Main.MAX_THREADS)), 1, 256, 1));
    private final JLabel scaleLabel = new JLabel(" ");
    private final JButton startButton = new JButton("开始分析");
    private final JButton stopButton = new JButton("停止");

    // —— 进度 ——
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel("就绪");
    private final JLabel timeLabel = new JLabel(" ");

    // —— 结果 ——
    private final JLabel baselineLabel = new JLabel(" ");
    private final JTabbedPane resultTabs = new JTabbedPane();
    private final ComboTableModel comboModel = new ComboTableModel();
    private final UnitTableModel unitModel = new UnitTableModel();
    private final JTable comboTable = new JTable(comboModel);
    private final JTable unitTable = new JTable(unitModel);
    private final TableRowSorter<ComboTableModel> comboSorter = new TableRowSorter<>(comboModel);
    private final TableRowSorter<UnitTableModel> unitSorter = new TableRowSorter<>(unitModel);
    private final JScrollPane comboScroll = new JScrollPane(comboTable);
    private final JScrollPane unitScroll = new JScrollPane(unitTable);
    private final JButton copyButton = new JButton("复制新阵型代码");

    // —— 状态 ——
    private final Timer refreshTimer = new Timer(300, e -> refreshInputState());
    private boolean analyzing = false;
    private List<CompiledFort> parsedEval = List.of();
    private int deletableCount = 0;
    private ContributionAnalyzer.Report lastReport;
    private SwingWorker<ContributionAnalyzer.Report, Void> worker;
    private long startTime;
    private String analyzedPlayerText;
    private boolean overCap = false;
    private JButton importPlayerButton;
    private JButton import2pButton;
    private JButton import1pButton;
    private JButton clearEvalButton;

    public ContributionTab() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        refreshTimer.setRepeats(false);
        exploreSpinner.setToolTipText("按比例随机采样删除组合，重跑可换样本");

        add(buildNorth(), BorderLayout.NORTH);
        add(buildResultPanel(), BorderLayout.CENTER);
        add(buildProgressPanel(), BorderLayout.SOUTH);

        wireEvents();
        installRenderers();
        refreshInputState();
    }

    /** 深色模式切换时由 MainGUI 调用。 */
    public void updateDarkMode() {
        repaint();
        comboTable.repaint();
        unitTable.repaint();
        updateScaleLabel();
    }

    private JPanel buildNorth() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(buildInputPanel());
        panel.add(buildParamPanel());
        return panel;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createTitledBorder("数据输入"));

        JPanel playerBox = new JPanel();
        playerBox.setLayout(new BoxLayout(playerBox, BoxLayout.Y_AXIS));
        JPanel playerRow = new JPanel(new BorderLayout(8, 0));
        playerRow.add(new JLabel("玩家阵型:"), BorderLayout.WEST);
        playerField.setFont(new Font("黑体", Font.PLAIN, 13));
        playerRow.add(playerField, BorderLayout.CENTER);
        importPlayerButton = new JButton("从 1P.txt 导入");
        importPlayerButton.addActionListener(e -> {
            String first = firstFormationText(BattleTab.readFileAutoEncoding("1P.txt"));
            if (first == null) {
                JOptionPane.showMessageDialog(this, "1P.txt 中没有可用阵型", "提示", JOptionPane.WARNING_MESSAGE);
            } else {
                playerField.setText(first);
            }
        });
        playerRow.add(importPlayerButton, BorderLayout.EAST);
        playerBox.add(playerRow);
        playerInfoLabel.setFont(new Font("黑体", Font.PLAIN, 12));
        playerBox.add(playerInfoLabel);
        panel.add(playerBox, BorderLayout.NORTH);

        JPanel evalBox = new JPanel(new BorderLayout(0, 2));
        evalBox.add(new JLabel("评判阵集:"), BorderLayout.NORTH);
        evalArea.setFont(new Font("黑体", Font.PLAIN, 13));
        evalArea.setRows(4);
        evalBox.add(new JScrollPane(evalArea), BorderLayout.CENTER);
        panel.add(evalBox, BorderLayout.CENTER);

        JPanel evalButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        import2pButton = new JButton("从 2P.txt 导入");
        import2pButton.addActionListener(e -> evalArea.setText(BattleTab.readFileAutoEncoding("2P.txt")));
        import1pButton = new JButton("从 1P.txt 导入");
        import1pButton.addActionListener(e -> evalArea.setText(BattleTab.readFileAutoEncoding("1P.txt")));
        clearEvalButton = new JButton("清空");
        clearEvalButton.addActionListener(e -> evalArea.setText(""));
        evalButtons.add(import2pButton);
        evalButtons.add(import1pButton);
        evalButtons.add(clearEvalButton);
        evalInfoLabel.setFont(new Font("黑体", Font.PLAIN, 12));
        evalButtons.add(evalInfoLabel);
        panel.add(evalButtons, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(0, 190));
        return panel;
    }

    private JPanel buildParamPanel() {
        JPanel panel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        panel.setBorder(BorderFactory.createTitledBorder("参数调节"));
        panel.add(new JLabel("探索率 m:"));
        panel.add(exploreSpinner);
        panel.add(new JLabel("%"));
        panel.add(new JLabel("删除数 n:"));
        panel.add(deleteSpinner);
        panel.add(new JLabel("线程数:"));
        panel.add(threadSpinner);
        panel.add(startButton);
        panel.add(stopButton);
        stopButton.setEnabled(false);
        scaleLabel.setFont(new Font("黑体", Font.PLAIN, 12));
        panel.add(scaleLabel);
        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createTitledBorder("结果"));
        baselineLabel.setFont(new Font("黑体", Font.PLAIN, 13));
        panel.add(baselineLabel, BorderLayout.NORTH);

        resultTabs.addTab("单位汇总", unitScroll);
        panel.add(resultTabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        copyButton.setEnabled(false);
        bottom.add(copyButton);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildProgressPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        progressBar.setStringPainted(true);
        panel.add(progressBar, BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout());
        row.add(statusLabel, BorderLayout.WEST);
        row.add(timeLabel, BorderLayout.EAST);
        panel.add(row, BorderLayout.SOUTH);
        return panel;
    }

    private void wireEvents() {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                scheduleRefresh();
            }

            public void removeUpdate(DocumentEvent e) {
                scheduleRefresh();
            }

            public void changedUpdate(DocumentEvent e) {
                scheduleRefresh();
            }
        };
        playerField.getDocument().addDocumentListener(listener);
        evalArea.getDocument().addDocumentListener(listener);
        exploreSpinner.addChangeListener(e -> updateScaleLabel());
        deleteSpinner.addChangeListener(e -> updateScaleLabel());
        startButton.addActionListener(e -> startAnalysis());
        stopButton.addActionListener(e -> stopAnalysis());
        copyButton.addActionListener(e -> copySelectedCombo());
        comboTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copyFrom(comboTable);
                }
            }
        });
        unitTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copyFrom(unitTable);
                }
            }
        });
        comboTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCopyButton();
            }
        });
        unitTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCopyButton();
            }
        });
    }

    private void scheduleRefresh() {
        refreshTimer.restart();
    }

    private void refreshInputState() {
        if (analyzing) {
            return;
        }
        parsedEval = List.of();
        deletableCount = 0;

        String evalText = evalArea.getText().trim();
        String evalError = null;
        if (!evalText.isEmpty()) {
            try {
                parsedEval = Setting.parseForts(evalText);
            } catch (Exception ex) {
                evalError = "阵集解析失败: " + ex.getMessage();
            }
        }
        if (evalError != null) {
            evalInfoLabel.setText(evalError);
        } else if (parsedEval.isEmpty()) {
            evalInfoLabel.setText(evalText.isEmpty() ? "未输入评判阵集" : "未解析到有效阵型");
        } else {
            evalInfoLabel.setText("已解析 " + parsedEval.size() + " 个阵型");
        }

        String playerText = playerField.getText().trim();
        if (playerText.isEmpty()) {
            playerInfoLabel.setText("未输入玩家阵型");
        } else {
            try {
                List<CompiledFort> forts = Setting.parseForts(playerText);
                if (forts.size() != 1) {
                    playerInfoLabel.setText("只能输入一个阵型");
                } else {
                    Formation formation = Formation.decode(playerText);
                    deletableCount = formation.units.size() - 1;
                    int cost = 0;
                    for (int i = 1; i <= deletableCount; i++) {
                        int c = Unit.infos[formation.units.get(i).id].cost();
                        cost += Math.max(c, 1);
                    }
                    playerInfoLabel.setText("阵名: " + (formation.name.isEmpty() ? "(无)" : formation.name)
                            + " | 可删单位数: " + deletableCount + " | 总军资金: " + cost);
                }
            } catch (Exception ex) {
                playerInfoLabel.setText("阵型解析失败: " + ex.getMessage());
            }
        }

        SpinnerNumberModel model = (SpinnerNumberModel) deleteSpinner.getModel();
        int max = Math.max(1, deletableCount);
        model.setMaximum(max);
        if ((int) deleteSpinner.getValue() > max) {
            deleteSpinner.setValue(max);
        }
        updateScaleLabel();
    }

    private void updateScaleLabel() {
        int n = (int) deleteSpinner.getValue();
        int m = (int) exploreSpinner.getValue();
        overCap = false;
        boolean ok = deletableCount > 0 && !parsedEval.isEmpty() && n >= 1 && n <= deletableCount;
        if (!ok) {
            scaleLabel.setText(" ");
            scaleLabel.setForeground(null);
        } else if (m == 0) {
            scaleLabel.setText("探索率 0%：仅计算基线胜率");
            scaleLabel.setForeground(null);
        } else {
            long total = ComboSelector.combinationCount(deletableCount, n);
            long k = ComboSelector.plannedCount(total, m);
            long battles = (k + 1) * parsedEval.size();
            overCap = k > ComboSelector.MAX_SAMPLES;
            if (overCap) {
                scaleLabel.setText(String.format("采样 %,d 个组合，超过上限 %,d，请降低探索率或删除数",
                        k, ComboSelector.MAX_SAMPLES));
            } else {
                scaleLabel.setText(String.format("共 %,d 个组合, 采样 %,d 个, 预计 %,d 场对战", total, k, battles));
            }
            scaleLabel.setForeground(overCap || battles > 2_000_000 ? warnColor() : null);
        }
        startButton.setEnabled(!analyzing && ok && !overCap);
    }

    private void startAnalysis() {
        if (analyzing) {
            return;
        }
        refreshInputState();
        if (parsedEval.isEmpty() || deletableCount < 1) {
            JOptionPane.showMessageDialog(this, "请先输入有效的玩家阵型和评判阵集", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (overCap) {
            JOptionPane.showMessageDialog(this,
                    "采样组合数超过上限 " + ComboSelector.MAX_SAMPLES + "，请降低探索率或减少删除数",
                    "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String playerText = playerField.getText().trim();
        analyzedPlayerText = playerText;
        String evalText = evalArea.getText().trim();
        ContributionAnalyzer.Params params = new ContributionAnalyzer.Params(
                (int) deleteSpinner.getValue(),
                (int) exploreSpinner.getValue(),
                (int) threadSpinner.getValue(),
                ContributionAnalyzer.SortKey.DELTA,
                ContributionAnalyzer.SortOrder.DESC);

        analyzing = true;
        setControlsEnabled(false);
        lastReport = null;
        comboTable.clearSelection();
        unitTable.clearSelection();
        copyButton.setEnabled(false);
        comboModel.setRows(List.of());
        unitModel.setRows(List.of());
        baselineLabel.setText("基线计算中...");
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setString("0");
        statusLabel.setText("正在分析...");
        timeLabel.setText(" ");
        startTime = System.nanoTime();

        worker = new SwingWorker<>() {
            @Override
            protected ContributionAnalyzer.Report doInBackground() throws Exception {
                return ContributionAnalyzer.analyze(playerText, evalText, params,
                        new ContributionAnalyzer.ProgressListener() {
                            @Override
                            public void onProgress(int done, long total, String phase) {
                                SwingUtilities.invokeLater(() -> {
                                    if (!analyzing) {
                                        return;
                                    }
                                    progressBar.setMaximum((int) Math.min(total, Integer.MAX_VALUE));
                                    progressBar.setValue(done);
                                    progressBar.setString(done + " / " + total);
                                    statusLabel.setText("正在分析[" + phase + "]...");
                                    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
                                    String eta = (done > 0 && total > done)
                                            ? String.format("剩余约 %.1fs",
                                                    (total - done) * (elapsedMs / (double) done) / 1000.0)
                                            : (total > done ? "估算中" : "即将完成");
                                    timeLabel.setText(String.format("已用时: %.1fs | %s", elapsedMs / 1000.0, eta));
                                });
                            }

                            @Override
                            public void onBaseline(ContributionAnalyzer.Baseline baseline) {
                                SwingUtilities.invokeLater(() -> baselineLabel.setText(baselineText(baseline) + " | 组合计算中..."));
                            }
                        }, this::isCancelled);
            }

            @Override
            protected void done() {
                analyzing = false;
                setControlsEnabled(true);
                try {
                    if (isCancelled()) {
                        statusLabel.setText("已取消");
                        progressBar.setString("已取消");
                        return;
                    }
                    ContributionAnalyzer.Report report = get();
                    if (report == null) {
                        statusLabel.setText("已取消");
                        progressBar.setString("已取消");
                        return;
                    }
                    showReport(report, params);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText("分析失败: " + cause.getMessage());
                    JOptionPane.showMessageDialog(ContributionTab.this,
                            "分析失败:\n" + cause.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void stopAnalysis() {
        if (worker != null) {
            worker.cancel(true);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        playerField.setEnabled(enabled);
        evalArea.setEnabled(enabled);
        importPlayerButton.setEnabled(enabled);
        import2pButton.setEnabled(enabled);
        import1pButton.setEnabled(enabled);
        clearEvalButton.setEnabled(enabled);
        exploreSpinner.setEnabled(enabled);
        deleteSpinner.setEnabled(enabled);
        threadSpinner.setEnabled(enabled);
        stopButton.setEnabled(!enabled);
        if (enabled) {
            updateScaleLabel();
        } else {
            startButton.setEnabled(false);
        }
    }

    private void showReport(ContributionAnalyzer.Report report, ContributionAnalyzer.Params params) {
        lastReport = report;
        baselineLabel.setText(baselineText(report.baseline())
                + String.format(" | 采样 %d/%d 组合 | 用时 %.1fs",
                report.sampledCombos(), report.totalCombos(), report.elapsedMs() / 1000.0));
        comboModel.setRefs(report.refs());
        comboModel.setRows(report.combos());
        unitModel.setRows(report.units());
        setComboTabVisible(params.deleteCount() >= 2 && report.sampledCombos() > 0);
        applyDefaultSort(params);
        copyButton.setEnabled(false);
        progressBar.setString("完成");
        statusLabel.setText("分析完成");
        long battles = (long) (report.sampledCombos() + 1) * parsedEval.size();
        if (report.sampledCombos() == 0) {
            statusLabel.setText("分析完成（未采样任何组合，仅基线）");
        } else if (battles > 0 && report.battleErrors() * 10L > battles) {
            statusLabel.setText("分析完成（异常场次偏多: " + report.battleErrors() + "）");
        }
    }

    private void setComboTabVisible(boolean visible) {
        int idx = resultTabs.indexOfComponent(comboScroll);
        if (visible && idx < 0) {
            resultTabs.insertTab("组合排行", null, comboScroll, null, 0);
        } else if (!visible && idx >= 0) {
            resultTabs.remove(idx);
        }
    }

    private void applyDefaultSort(ContributionAnalyzer.Params params) {
        boolean delta = params.sortKey() == ContributionAnalyzer.SortKey.DELTA;
        javax.swing.SortOrder order = params.order() == ContributionAnalyzer.SortOrder.DESC
                ? javax.swing.SortOrder.DESCENDING : javax.swing.SortOrder.ASCENDING;
        comboSorter.setSortKeys(List.of(new RowSorter.SortKey(
                delta ? COMBO_DELTA_COLUMN : COMBO_PER_COST_COLUMN, order)));
        unitSorter.setSortKeys(List.of(new RowSorter.SortKey(
                delta ? UNIT_DELTA_COLUMN : UNIT_PER_COST_COLUMN, order)));
    }

    private void copySelectedCombo() {
        if (comboTable.getSelectedRow() >= 0) {
            copyFrom(comboTable);
        } else {
            copyFrom(unitTable);
        }
    }

    private void copyFrom(JTable table) {
        if (lastReport == null || analyzedPlayerText == null) {
            return;
        }
        int[] unitIndices;
        int viewRow;
        if (table == comboTable) {
            viewRow = comboTable.getSelectedRow();
            if (viewRow < 0) {
                return;
            }
            unitIndices = comboModel.get(comboTable.convertRowIndexToModel(viewRow)).unitIndices();
        } else {
            viewRow = unitTable.getSelectedRow();
            if (viewRow < 0) {
                return;
            }
            unitIndices = new int[]{unitModel.get(unitTable.convertRowIndexToModel(viewRow)).unit().index()};
        }
        String code = ContributionAnalyzer.removeUnitsFromCode(analyzedPlayerText, unitIndices);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        statusLabel.setText("已复制新阵型代码到剪贴板");
    }

    private void updateCopyButton() {
        boolean hasSelection = comboTable.getSelectedRow() >= 0 || unitTable.getSelectedRow() >= 0;
        copyButton.setEnabled(!analyzing && hasSelection && lastReport != null);
    }

    private static String baselineText(ContributionAnalyzer.Baseline b) {
        return String.format("基线胜率: %s | 战绩: 胜%d 负%d 平%d 超时%d",
                fmtPercent(b.winRate()), b.win(), b.lose(), b.draw(), b.timeout());
    }

    private static String fmtPercent(double v) {
        return Double.isNaN(v) ? "—" : String.format("%.2f%%", v);
    }

    private static String firstFormationText(String content) {
        for (String part : content.split("/")) {
            part = part.trim();
            if (!part.isEmpty()) {
                return part;
            }
        }
        return null;
    }

    private static Color upColor() {
        return Main.DARK_MODE ? new Color(0x81C784) : new Color(0x2E7D32);
    }

    private static Color downColor() {
        return Main.DARK_MODE ? new Color(0xE57373) : new Color(0xC62828);
    }

    private static Color warnColor() {
        return Main.DARK_MODE ? new Color(0xFF8A80) : new Color(0xD32F2F);
    }

    private static final int COMBO_PLAIN_RATE_COLUMN = 2;
    private static final int COMBO_DELTA_COLUMN = 3;
    private static final int COMBO_PER_COST_COLUMN = 4;
    private static final int UNIT_DELTA_COLUMN = 2;
    private static final int UNIT_PER_COST_COLUMN = 3;

    /** 比较器视 NaN 为最小；TableRowSorter 对 DESCENDING 取反后 NaN 置底，ASCENDING 时置顶。 */
    private static final Comparator<Double> NAN_LAST_DESC = (a, b) -> {
        boolean na = a.isNaN();
        boolean nb = b.isNaN();
        if (na || nb) {
            return na == nb ? 0 : (na ? -1 : 1);
        }
        return Double.compare(a, b);
    };

    private void installRenderers() {
        comboTable.setRowSorter(comboSorter);
        unitTable.setRowSorter(unitSorter);

        SignedRenderer signed = new SignedRenderer();
        comboSorter.setComparator(COMBO_PLAIN_RATE_COLUMN, NAN_LAST_DESC);
        comboSorter.setComparator(COMBO_DELTA_COLUMN, NAN_LAST_DESC);
        comboSorter.setComparator(COMBO_PER_COST_COLUMN, NAN_LAST_DESC);
        unitSorter.setComparator(UNIT_DELTA_COLUMN, NAN_LAST_DESC);
        unitSorter.setComparator(UNIT_PER_COST_COLUMN, NAN_LAST_DESC);

        comboTable.getColumnModel().getColumn(COMBO_PLAIN_RATE_COLUMN).setCellRenderer(new PlainPercentRenderer());
        comboTable.getColumnModel().getColumn(COMBO_DELTA_COLUMN).setCellRenderer(signed);
        comboTable.getColumnModel().getColumn(COMBO_PER_COST_COLUMN).setCellRenderer(new PerCostRenderer());
        unitTable.getColumnModel().getColumn(UNIT_DELTA_COLUMN).setCellRenderer(signed);
        unitTable.getColumnModel().getColumn(UNIT_PER_COST_COLUMN).setCellRenderer(new PerCostRenderer());
    }

    /** 带正负号与红绿配色的百分比渲染器。 */
    private static final class SignedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Double d) {
                if (d.isNaN()) {
                    setText("—");
                    if (!isSelected) {
                        setForeground(table.getForeground());
                    }
                } else {
                    setText(String.format("%+.2f%%", d));
                    if (!isSelected) {
                        setForeground(d > 0 ? upColor() : (d < 0 ? downColor() : table.getForeground()));
                    }
                }
            }
            return this;
        }
    }

    /** 删除后胜率：无正号。 */
    private static final class PlainPercentRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Double d) {
                setText(d.isNaN() ? "—" : String.format("%.2f%%", d));
            }
            return this;
        }
    }

    /** 每费变化：%+/费，带红绿配色。 */
    private static final class PerCostRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Double d) {
                if (d.isNaN()) {
                    setText("—");
                    if (!isSelected) {
                        setForeground(table.getForeground());
                    }
                } else {
                    setText(String.format("%+.3f%%/费", d));
                    if (!isSelected) {
                        setForeground(d > 0 ? upColor() : (d < 0 ? downColor() : table.getForeground()));
                    }
                }
            }
            return this;
        }
    }

    /** 组合排行表模型。 */
    private static final class ComboTableModel extends AbstractTableModel {
        private static final String[] COLUMNS =
                {"删除单位", "删除军资金", "删除后胜率", "胜率变化", "每费变化"};
        private List<ContributionAnalyzer.ComboResult> rows = List.of();
        private List<ContributionAnalyzer.UnitRef> refs = List.of();

        void setRows(List<ContributionAnalyzer.ComboResult> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void setRefs(List<ContributionAnalyzer.UnitRef> refs) {
            this.refs = refs;
        }

        ContributionAnalyzer.ComboResult get(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0 -> String.class;
                case 1 -> Integer.class;
                default -> Double.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            ContributionAnalyzer.ComboResult r = rows.get(row);
            return switch (column) {
                case 0 -> describe(r);
                case 1 -> (int) r.removedCost();
                case 2 -> r.winRate();
                case 3 -> r.delta();
                default -> r.deltaPerCost();
            };
        }

        private String describe(ContributionAnalyzer.ComboResult r) {
            StringBuilder sb = new StringBuilder();
            for (int idx : r.unitIndices()) {
                if (sb.length() > 0) {
                    sb.append(" + ");
                }
                ContributionAnalyzer.UnitRef ref = refs.get(idx);
                sb.append(ref.name()).append("(").append(ref.cost()).append(")@(")
                        .append(ref.x()).append(",").append(ref.y()).append(")");
            }
            return sb.toString();
        }
    }

    /** 单位汇总表模型。 */
    private static final class UnitTableModel extends AbstractTableModel {
        private static final String[] COLUMNS =
                {"单位", "军资金", "平均胜率变化", "平均每费变化"};
        private List<ContributionAnalyzer.UnitSummary> rows = List.of();

        void setRows(List<ContributionAnalyzer.UnitSummary> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        ContributionAnalyzer.UnitSummary get(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0 -> String.class;
                case 1 -> Integer.class;
                default -> Double.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            ContributionAnalyzer.UnitSummary u = rows.get(row);
            return switch (column) {
                case 0 -> u.unit().name() + "@(" + u.unit().x() + "," + u.unit().y() + ")";
                case 1 -> u.unit().cost();
                case 2 -> u.avgDelta();
                default -> u.avgDeltaPerCost();
            };
        }
    }
}
```

- [ ] **Step 2: 编译主代码**

预期：无错误（仅有既有警告）。如报错，按错误信息修正本文件后重试。

- [ ] **Step 3: 临时自测不受影响**

依次运行「编译自测」「运行自测」。
预期：`TOTAL pass=22 fail=0`（GUI 不参与自测）。

---

### Task 6: MainGUI 注册标签页

**Files:**
- Modify: `src/main/java/org/example/GUI/MainGUI.java`

- [ ] **Step 1: 添加字段**

在第 13-16 行附近（`unitDexTab` / `linkTab` 字段区域）追加：

```java
    private final ContributionTab contributionTab;
```

- [ ] **Step 2: 注册标签页**

把第 68 行：

```java
        mainTabs.addTab("对战模拟", new BattleTab());
```

替换为：

```java
        mainTabs.addTab("对战模拟", new BattleTab());
        contributionTab = new ContributionTab();
        mainTabs.addTab("贡献分析", contributionTab);
```

- [ ] **Step 3: 深色模式传播**

在 `updateDarkMode()` 方法开头（`if (traceTab != null)` 之前）插入：

```java
        if (contributionTab != null) {
            contributionTab.updateDarkMode();
        }
```

- [ ] **Step 4: 验证**

运行「编译主代码」。
预期：无错误。再依次运行「编译自测」「运行自测」，预期 `TOTAL pass=22 fail=0`。

---

### Task 7: 文档更新与最终验证

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: 更新 AGENTS.md**

在「**主框架：**」列表中（`**BattleTab.java**` 行之后）插入：

```markdown
- **ContributionTab.java** — 贡献分析标签页。输入玩家阵型 + 评判阵集，枚举/采样删除 n 个单位的组合批量对战，按胜率变化或每费变化排序展示组合排行与单位汇总，可复制删除后的阵型代码
```

在「**阵容分析子系统：**」之后新增一节：

```markdown
**贡献分析子系统：**
- **ContributionAnalyzer.java** — 核心逻辑。解析输入、组合枚举/采样、独立线程池滑动窗口批量对战、聚合与排序、复制代码生成。不修改模拟引擎
- **ComboSelector.java** — 组合枚举与采样。C(N,n) 计算、字典序排名/反解、Floyd 算法随机采样、全遍历；组合数上限 `MAX_SAMPLES`（100 万）
- **FortTrimmer.java** — 从 CompiledFort 过滤掉指定索引的单位生成新阵型（单位顺序、随机种子保持不变）
- **ContributionTab.java** — GUI。数据输入/参数调节/结果显示三面板；探索率 m（0~100%）、删除数 n、线程数；结果表点击表头排序（初始按胜率变化量降序）；基线先显示、滑动窗口进度、ETA、可取消；n=1 时只显示单位汇总页签；组合数超上限时禁止开始
```

在「**通用组件：**」列表中（`**FixedJTextArea.java**` 行之后）插入：

```markdown
- **WrapLayout.java** — 可换行 FlowLayout。按容器宽度计算多行高度并自动增高容器，解决窄窗口下参数行被裁切的问题
```

- [ ] **Step 2: 全量验证**

1. 运行「编译主代码」→ 无错误
2. 运行「编译自测」→ 无错误
3. 运行「运行自测」→ `TOTAL pass=22 fail=0`
4. 启动 GUI 人工冒烟：

```powershell
& "C:\Program Files\jdk-21_windows-x64_bin\jdk-21.0.9\bin\java" -cp "target/classes;C:\Users\cain\.m2\repository\com\formdev\flatlaf\3.5.4\flatlaf-3.5.4.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.18.3\jackson-databind-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.18.3\jackson-core-2.18.3.jar;C:\Users\cain\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.18.3\jackson-annotations-2.18.3.jar" org.example.Main
```

人工检查清单：
- [ ] 切到「贡献分析」标签页（位于「对战模拟」之后），三个面板显示正常
- [ ] `[从 1P.txt 导入]` 玩家阵型、`[从 2P.txt 导入]` 评判阵集，解析信息与规模预估实时变化
- [ ] 规模预估：`n=1, m=5%` 显示合理数字；`n=3, m=100%` 明显变大时标红
- [ ] 开始分析：进度条推进，基线完成后立即显示基线与战绩，继续组合计算
- [ ] 完成后：`n=1` 只有「单位汇总」页签；把 n 改为 2 再跑一次，「组合排行」页签出现
- [ ] n≥2 时「组合排行」页签显示在「单位汇总」之前
- [ ] 单位汇总表无「出现组合数」列
- [ ] n≥2 时单位汇总的每费变化为各组合每费变化的平均
- [ ] 结果表初始按胜率变化量降序；点击各数值列表头可正/倒序排序；表中无「排名」列；默认降序时全部超时/异常的行显示为「—」且排在表尾
- [ ] 缩小窗口宽度 → 参数面板自动换行增高，第二行内容完整可见
- [ ] 选中组合行 → 复制按钮可用；复制后粘贴到「阵型工作台」能正常解析（单位数少 1）
- [ ] 分析中「停止」→ 状态栏显示「已取消」，控件恢复可用
- [ ] 深色/浅色主题切换后表格与红绿配色正常
- [ ] 使用「对战模拟」时的线程池不受本标签页线程数影响
- [ ] `n=1` 时选中/双击「单位汇总」行 → 复制按钮可用，复制的代码单位数少 1
- [ ] 分析完成后修改玩家阵型文本，再双击旧结果行 → 复制出的仍是发起分析时阵型的删除结果
- [ ] 分析进行中「从 1P/2P 导入」「清空」按钮不可用
- [ ] 探索率 0% → 规模标签提示「仅计算基线胜率」，运行后状态栏提示「未采样任何组合」
- [ ] 基线阶段点「停止」→ 显示「已取消」，无结果残留
- [ ] 超上限（如 63 单位删 5、m=100%）→ 标签标红、开始按钮禁用

- [ ] **Step 3: 不提交**

按项目约定，改动由用户手动 git commit，无需执行任何提交命令。

---

## Self-Review 记录

- **Spec 覆盖**：输入解析（Task 1/4/5）、组合枚举与采样（Task 2）、对战执行与调度（Task 4）、指标与排序（Task 4/5）、界面三面板（Task 5）、复制代码（Task 4/5）、深色模式（Task 5/6）、边界情况（Task 2/4/5）、验证方式（Task 4/7）、文档（Task 7）——均有对应任务。
- **占位符扫描**：无 TBD/TODO；所有代码步骤均有完整代码。
- **类型一致性**：`ComboSelector.Selection#count/total/materialize`、`ContributionAnalyzer.Params/Report/Baseline/UnitRef/ComboResult/UnitSummary`、`FortTrimmer.trim`、`ContributionTab.updateDarkMode` 在测试与 GUI 中签名一致。
- **已知取舍**：GUI 降序时 NaN 置底（自定义比较器），升序时 NaN 置顶。
