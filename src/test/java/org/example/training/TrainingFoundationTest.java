package org.example.training;

import org.example.Fort;
import org.example.Main;
import org.example.Utils;

/**
 * 不依赖第三方测试框架，便于在当前仅配置 javac 的工程中直接运行。
 */
public final class TrainingFoundationTest {
    private TrainingFoundationTest() {
    }

    public static void main(String[] args) {
        testExactLcgSequence();
        testDefaultSeedCanBeMixed();
        testSeedSchedule();
        testDeterministicBattleAndTelemetry();
        testDifferentSeedsReachRandomUnits();
        testSummonedUnitsAreTracked();
        System.out.println("TrainingFoundationTest: ALL PASSED");
    }

    private static void testExactLcgSequence() {
        Utils random = new Utils(0);
        int[] expectedStates = {
                12_345,
                1_406_932_606,
                654_583_775,
                1_449_466_924,
                229_283_573
        };
        for (int expectedState : expectedStates) {
            float value = random.random();
            check(random.getSeed() == expectedState, "LCG 状态不符合 31 位整数公式");
            check(value >= 0 && value < 1, "随机数必须位于 [0, 1)");
        }
    }

    private static void testDefaultSeedCanBeMixed() {
        check(new Utils().getSeed() == Utils.DEFAULT_SEED, "默认种子必须为非零值");
        check(Utils.DEFAULT_SEED != 0, "默认种子不能被阵型因子乘法吞掉");
    }

    private static void testSeedSchedule() {
        int[] first = SeedSchedule.create(20260724L, 8);
        int[] second = SeedSchedule.create(20260724L, 8);
        check(java.util.Arrays.equals(first, second), "同一主种子的种子表必须稳定");
        check(first[0] != first[1], "相邻 episode 不应得到相同种子");
    }

    private static void testDeterministicBattleAndTelemetry() {
        String emptyFortCode = coreCode();
        Fort p1 = new Fort("empty-p1", emptyFortCode);
        Fort p2 = new Fort("empty-p2", emptyFortCode);
        BattleOptions options = new BattleOptions(
                2_000,
                123_456,
                TelemetryMode.EVENTS_AND_SNAPSHOTS,
                30
        );

        TrainingBattleResult first = TrainingBattleRunner.run(p1, p2, options);
        TrainingBattleResult second = TrainingBattleRunner.run(p1, p2, options);

        check(first.status() == second.status(), "同种子胜负必须一致");
        check(first.winnerHp() == second.winnerHp(), "同种子胜者血量必须一致");
        check(first.frames() == second.frames(), "同种子帧数必须一致");
        check(first.p1Hp() == second.p1Hp() && first.p2Hp() == second.p2Hp(),
                "同种子双方最终 HP 必须一致");
        check(first.finalRandomState() == second.finalRandomState(), "同种子最终随机状态必须一致");
        check(first.telemetry().events().equals(second.telemetry().events()),
                "同种子事件流必须逐项一致");
        check(first.telemetry().snapshots().equals(second.telemetry().snapshots()),
                "同种子快照必须逐项一致");

        BattleSummary summary = first.telemetry().summary();
        check(summary.collisionCount() > 0, "空阵对撞应记录至少一次要塞碰撞");
        check(summary.firstCollisionFrame() > 0, "应记录首次碰撞帧");
        check(first.telemetry().snapshots().getFirst().frame() == 0, "首个快照必须是初始帧");
        check(first.telemetry().snapshots().getLast().frame() == first.frames(),
                "最后快照必须对应终局帧");
    }

    private static void testDifferentSeedsReachRandomUnits() {
        Fort p1 = new Fort("shotgun-p1", oneUnitCode(13, 250, 350, 0));
        Fort p2 = new Fort("shotgun-p2", oneUnitCode(13, 250, 350, 0));
        TrainingBattleResult seedOne = TrainingBattleRunner.run(
                p1,
                p2,
                new BattleOptions(500, 1, TelemetryMode.EVENTS, 0)
        );
        TrainingBattleResult seedTwo = TrainingBattleRunner.run(
                p1,
                p2,
                new BattleOptions(500, 2, TelemetryMode.EVENTS, 0)
        );
        check(seedOne.finalRandomState() != seedTwo.finalRandomState(),
                "显式种子必须传递到会使用随机数的单位");

        TrainingBattleResult withoutTelemetry = TrainingBattleRunner.run(
                p1,
                p2,
                new BattleOptions(500, 1, TelemetryMode.OFF, 0)
        );
        check(seedOne.status() == withoutTelemetry.status(), "遥测不得改变胜负");
        check(seedOne.frames() == withoutTelemetry.frames(), "遥测不得改变终局帧");
        check(seedOne.p1Hp() == withoutTelemetry.p1Hp()
                        && seedOne.p2Hp() == withoutTelemetry.p2Hp(),
                "遥测不得改变最终 HP");
        check(seedOne.finalRandomState() == withoutTelemetry.finalRandomState(),
                "遥测不得消耗随机数");
        check(withoutTelemetry.telemetry() == null, "OFF 模式不应创建遥测对象");
    }

    private static void testSummonedUnitsAreTracked() {
        Fort p1 = new Fort("dark-p1", oneUnitCode(45, 250, 350, 0));
        Fort p2 = new Fort("empty-p2", coreCode());
        TrainingBattleResult result = TrainingBattleRunner.run(
                p1,
                p2,
                new BattleOptions(300, 7, TelemetryMode.EVENTS, 0)
        );

        check(result.telemetry().summary().p1UnitSpawns() > 0,
                "暗玉召唤出的单位应计入 1P spawn 统计");
        check(result.telemetry().events().stream()
                        .anyMatch(event -> event.type() == BattleEventType.UNIT_SPAWN
                                && event.side() == 0),
                "暗玉召唤出的单位应产生 UNIT_SPAWN 事件");
    }

    private static String coreCode() {
        return "0" + encode(0, 190, 400);
    }

    private static String oneUnitCode(int type, int x, int y, int rotation) {
        return coreCode() + Main.pskey.charAt(type) + encode(rotation, x, y);
    }

    private static String encode(int rotation, int x, int y) {
        return Utils.to_rad61(rotation * 1_000_000 + x * 1_000 + y, 5);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
