package org.example;

<<<<<<< Updated upstream
import org.example.elements.*;
import org.example.elements.hit.KekkaiField;
import org.example.elements.units.*;
import org.example.elements.wall.*;
=======
import org.example.server.BattleServer;

>>>>>>> Stashed changes
import java.util.*;

public class Main {
<<<<<<< Updated upstream
    public static final boolean ENABLE_VISUALIZATION = true;    //是否开启可视化
    public static final boolean SHOW_UNIT_HP = true;    // 是否显示单位生命值（用于debug）
    public static final int LOGIC_TPS = 60;      //帧率限制，0代表无限制
=======
    public static int MAX_FRAME_LIMIT = 65536;    //最大运行帧数
    public static boolean SHOW_REMAIN_HP = false;
    public static boolean AUTO_PLAY = false;
    public static int MAX_THREADS = Runtime.getRuntime().availableProcessors();
    public static int PORT = 8080;
    public static String MODE = "interactive";   // interactive | batch | server
    public static final String CONFIG_FILE = "config.ini";
>>>>>>> Stashed changes

    private static boolean end = false;
    public static boolean norikomi_flg = false;    //怒土の神秘小变量，撞击时会变成true
    private static int j;   //怒土遍历用的变量
    private static int i;   //怒土遍历用的变量
    public static String pskey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";      //密码表
    private static GameWindow window;
    public static int ID = 0;   //待分配的ID，只会一直增长
    private static float wrk;   //怒土の神秘小变量
    static int max_run_time = 65536;    //最大运行帧数
    private static final Scanner scanner = new Scanner(System.in);
    public static int[] hp = {100, 100};    //要塞血量
    public static int[] hp0_flg = {0, 0};   //要塞爆炸标记
    public static boolean[] dokkan_flg = {false, false};    //受到撞击标记，有贽玉不会变成true
    public static int[] saihai_cnt = {0, 0};        //采玉效果剩余时间
    public static int[] saihai_rot = {0, 0};        //采玉效果角度
    public static int[] dead_last = {0, 0};         //上一个死亡的单位种类
    public static float[] core_x = new float[]{0, 0};
    public static float[] core_y = new float[]{0, 0};
    public static int time = 0;     //计时器
    public static Utils[] seeder={new Utils(),new Utils()};
    public static Base[] bases;     //双方车板
    public static Core[] cores = new Core[]{null, null};    //双方老家
    //TODO：Gemini 说这里可以自己写可以以int为键的哈希表类，不用Integer包装类，至于提升大不大我不知道。
    public static LinkedHashMap<Integer, Shape> elements = new LinkedHashMap<>();   //所有需要参与循环的部件
    public static CompositeShape[] wall = {new CompositeShape(0,0), new CompositeShape(0,0)};   //墙体
    public static CompositeShape[] unit = {new CompositeShape(0,0), new CompositeShape(0,0)};   //单位
    public static CompositeShape[] shield = {new CompositeShape(0,0), new CompositeShape(0,0)}; //屏障
    public static CompositeShape[] atk = {new CompositeShape(0,0), new CompositeShape(0,0)};    //攻击
    public static CompositeShape[] fort = {new CompositeShape(0,0), new CompositeShape(0,0)};   //要塞主体
    public static CompositeShape[] team = {new CompositeShape(0,0), new CompositeShape(0,0)};   //队伍
    public static CompositeShape[] heal = {new CompositeShape(0,0), new CompositeShape(0,0)};   //治疗
    public static CompositeShape[] repair = {new CompositeShape(0,0), new CompositeShape(0,0)}; //修复
    public static CompositeShape[] jump_u = {new CompositeShape(0,0), new CompositeShape(0,0)}; //近突
    public static CompositeShape[] jump_f = {new CompositeShape(0,0), new CompositeShape(0,0)}; //远突
    public static CompositeShape[] snipe = {new CompositeShape(0,0), new CompositeShape(0,0)}; //狙击
    public static CompositeShape[] turn_ccw = {new CompositeShape(0,0), new CompositeShape(0,0)}; //顺时针
    public static CompositeShape[] turn_cw = {new CompositeShape(0,0), new CompositeShape(0,0)}; //逆时针
    public static LinkedList<Integer>[] kekkaiIds = new LinkedList[]{new LinkedList<Integer>(), new LinkedList<Integer>()};
    public static KekkaiField[] kekkaiFields = new KekkaiField[]{null, null};
    static String default_code = "000P6RAnPDL9 vs 000P6R";    //默认对战代码，为空时在运行时手动输入

    public static void main(String[] args) {
<<<<<<< Updated upstream
        for (int i = 0; i <= 1; i++){
            fort[i].addShape(wall[i]);
            fort[i].addShape(unit[i]);
        }
        for (int i = 0; i <= 1; i++){
            team[i].addShape(wall[i]);
            team[i].addShape(unit[i]);
            team[i].addShape(shield[i]);
            team[i].addShape(atk[i]);
        }

        bases = new Base[]{new Base(240, 532, 0), new Base(1680, 532, 1)};

        if (default_code.isEmpty()) {   //处理代码导入
            System.out.println("请输入对战代码：(输入格式: 1P代码 vs 2P代码)");
            String[] input = scanner.nextLine().split(" vs ");
            main_setup(input);
        } else {
            main_setup(default_code.split(" vs "));
        }
        if (ENABLE_VISUALIZATION) {
            java.awt.EventQueue.invokeLater(() -> {
                window = new GameWindow(1920, 960, 60).setList(new ArrayList<>(elements.values()));
                window.setVisible(true);
            });
        }
        long timePerTick = LOGIC_TPS > 0 ? 1000000000L / LOGIC_TPS : 0;
        long lastTime = System.nanoTime();

        while (time < max_run_time && !end){    /*主循环*/
            long now = System.nanoTime();
            if (timePerTick == 0 || now - lastTime >= timePerTick) {
                lastTime = now;
                time++;
                base_move();    //要塞车移动
                judge();    //判断对局是否应该结束
                update();   //调用所有元素step()方法
                if (ENABLE_VISUALIZATION && window != null) {
                    window.setList(new ArrayList<>(elements.values()));
                    window.requestRender();
                }
            }
        }
    }

    private static void main_setup(String[] code){      //初始布局
        i = 0;
        while (i <= 1) {
            code[i] = code[i].replaceAll("[^a-zA-Z0-9]", "");
            if (code[i].length() % 6 != 0) {
                return;
            }
            int[] xyr = to_xyr(code[i].substring(1, 6));
            Utils.universalSeed = Utils.universalSeed * (xyr[0] % 168 + 48) * (xyr[1] % 168 + 48);
            int baseSeed = (xyr[0] % 168 + 48) * (xyr[1] % 168 + 48);
            seeder[i].setSeed(baseSeed);
            xyr[0] -= 190;
            xyr[1] -= 400;
            if (code[i].startsWith("1")) {
                cores[i] = new BossCore(i == 1 ? -xyr[0] : xyr[0], xyr[1], i);
            } else if (code[i].startsWith("2")) {
                cores[i] = new BossCore2(i == 1 ? -xyr[0] : xyr[0], xyr[1], i);
            } else {
                cores[i] = new Core(i == 1 ? -xyr[0] : xyr[0], xyr[1], i);
            }
            core_x[i] = cores[i].x;
            core_y[i] = cores[i].y;
            int wrkSeed;
            j = 6;
            int wrkType;
            while (j < code[i].length()) {
                xyr = to_xyr(code[i].substring(j + 1, j + 6));
                wrkSeed = baseSeed * (xyr[0] % 185 + 30) * (xyr[1] % 185 + 30);
                if (i == 1) {
                    xyr[0] = (380 - xyr[0]) + 1490;
                    xyr[2] = 180 - xyr[2];
                } else {
                    xyr[0] += 50;
                }
                xyr[2] = (xyr[2] % 360 + 360) % 360;
                xyr[1] += 132;
                wrkType = pskey.indexOf(code[i].charAt(j));
                unit_make(xyr[0], xyr[1], xyr[2], wrkType, i);
                if (wrkType == 13) {
                    ShotgunBall unit = (ShotgunBall) elements.get(ID - 1);
                    unit.setSeed(wrkSeed);
                } else if (wrkType == 23) {
                    HenBall unit = (HenBall) elements.get(ID - 1);
                    unit.setSeed(wrkSeed);
                }
                j += 6;
            }
            kekkaiFields[i] = new KekkaiField(i);   //界玉初始化
            i++;
        }
    }

    private static void base_move(){    //车板移动
        dokkan_flg[0] = false;
        dokkan_flg[1] = false;
        norikomi_flg = false;
        saihai_cnt[0]--;
        saihai_cnt[1]--;
        j = 0;
        while (j <= 1) {
            if (hp0_flg[j] > 0){
                j++;
                continue;
            }
            core_x[j] = cores[j].x;
            core_y[j] = cores[j].y;
            for (Shape s : unit[j].getShapes()) {
                if (s instanceof TargetBall) {
                    core_x[j] = s.x;
                    core_y[j] = s.y;
                    break;
                }
            }
            j++;
        }
        if (hp0_flg[0] == 0 && hp0_flg[1] == 0 && bases[1].x - bases[1].xs - (bases[0].x + bases[0].xs) <= 380) {
            norikomi_flg = true;
            System.out.println("要塞相撞，时间: " + time);
            j = 0;
            while (j <= 1) {
                boolean nie_flg = false;
                for (Shape s : unit[j].getShapes()) {
                    if (s instanceof NieBall nie) {
                        nie.alarm = 6;
                        nie_flg = true;
                        break;
                    }
                }
                if (nie_flg){
                    j++;
                    continue;
                }
                wrk = (float) (Math.floor(bases[(1 - j)].xs * 5) + 1);
                wrk = Math.round(wrk);
                if (wrk < 0) {
                    wrk = 0;
                }
                hp[j] -= (int) wrk;
                dokkan_flg[j] = true;
                j++;
            }
            wrk = bases[0].xs;
            bases[0].xs = -bases[1].xs;
            if (bases[0].xs > -1) { bases[0].xs = -1; }
            bases[1].xs = -wrk;
            if (bases[1].xs > -1) { bases[1].xs = -1; }
            bases[0].ys = (-bases[0].xs) * 2;
            bases[1].ys = (-bases[1].xs) * 2;
=======
        Setting.loadConfig();
        parseArgs(args);
        pool = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("正在导入阵容...");
        p1List = Setting.CompileForts("1P.txt");
        p2List = Setting.CompileForts("2P.txt");
        System.out.println("阵容导入完成！");

        switch (MODE) {
            case "server" -> runServer();
            case "batch" -> runBatch();
            default -> runInteractive();
>>>>>>> Stashed changes
        }
    }

<<<<<<< Updated upstream
    private static void judge(){    //裁决爆炸
        i = 0;
        while (i <= 1) {
            if (hp0_flg[i] == 0 && hp[i] < 1) {
                hp0_flg[i] = 1;
                dokkan_flg[i] = true;
            }
            if (hp0_flg[i] > 0) {
                hp0_flg[i]++;
                dokkan_flg[i] = true;
                if (hp0_flg[i] == 3) {
                    bases[i].kill();
                    cores[i].kill();
                }
                if (hp0_flg[i] > 120) {
                    wrk = 0;
                    hp[0] = Math.max(0, hp[0]);
                    hp[1] = Math.max(0, hp[1]);
                    end = true;
                    break;
                }
            }
            i++;
=======
    private static void runServer() {
        System.out.println("对战模拟服务启动中...");
        System.out.println("监听: http://localhost:" + PORT);
        System.out.println("已加载阵型: 1P=" + p1List.size() + "个, 2P=" + p2List.size() + "个");
        System.out.println("线程数: " + MAX_THREADS);
        System.out.println("按 Ctrl+C 停止");
        try {
            BattleServer server = new BattleServer(PORT, pool);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(2)));
            server.start();
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runBatch() {
        runOneBatchPass();
    }

    private static void runInteractive() {
        while (Setting.setting(scanner)) {
            runOneBatchPass();
        }
    }

    private static void runOneBatchPass() {
        long total_start = System.nanoTime();

        List<Future<Result>> futures = new ArrayList<>();
        List<String> meta = new ArrayList<>();
        for (int j = 0; j < p1List.size(); j++) {
            for (int i = 0; i < p2List.size(); i++) {
                CompiledFort f1 = p1List.get(j);
                CompiledFort f2 = p2List.get(i);

                int roundIndex = j * 200 + i + 1;

                futures.add(pool.submit(() -> {
                    GameTask g = new GameTask();
                    return g.run_single(f1, f2);
                }));

                // 保存元信息（保证顺序）
                meta.add("Round " + roundIndex +
                        " | 1P[" + f1.name + "] vs 2P[" + f2.name + "]");
            }
        }

        StringBuilder final_result = new StringBuilder();
        StringBuilder simple_result = new StringBuilder();
        int done = 0;
        int score = 0;
        int win = 0;
        int lose = 0;
        int draw = 0;
        int unknown = 0;
        for (int i = 0; i < futures.size(); i++) {
            try {
                Result r = futures.get(i).get();
                final_result.append(meta.get(i)).append("\n");
                String resultStr = switch (r.status) {
                    case 1 -> "1P 获胜";
                    case 2 -> "2P 获胜";
                    case 0 -> "平局";
                    case -1 -> "超时";
                    default -> "异常";
                };
                final_result.append("结果: ").append(resultStr)
                        .append(" | 剩余血量: ").append(r.winnerHp)
                        .append(" | 总帧数: ").append(r.framePassed)
                        .append(" | 用时: ").append(String.format("%.3f ms", r.timeUsed))
                        .append("\n\n");
                if (i % p2List.size() == 0){
                    if (i > 0) {
                        simple_result.append("\n\n");
                        score = 0;
                        win = 0;
                        lose = 0;
                        draw = 0;
                        unknown = 0;
                    }
                    simple_result.append(p1List.get(i / p2List.size()).name).append(": \n");
                }
                simple_result.append(r.getSimpleResult());
                score += r.getScore();
                win += r.status == 1 ? 1 : 0;
                lose += r.status == 2 ? 1 : 0;
                draw += r.status == 0 ? 1 : 0;
                unknown += r.status < 0 ? 1 : 0;
                if ((i+1) % p2List.size() == 0){
                    simple_result.append("\n")
                            .append("总场次: %d".formatted(p2List.size()))
                            .append(", 胜: %d".formatted(win))
                            .append(", 负: %d".formatted(lose))
                            .append(", 平: %d".formatted(draw))
                            .append(", 未定: %d".formatted(unknown))
                            .append(", 胜率: %.2f".formatted((2 * win + draw) * 50F / (win + lose + draw)))
                            .append("%, 血量积分: ").append(score);
                }
                System.out.print("\r进度: " + ++done + "/" + meta.size());
            } catch (Exception e) {
                Throwable cause = e.getCause();

                Objects.requireNonNullElse(cause, e).printStackTrace();
                final_result.append(meta.get(i))
                        .append("\nERROR: ")
                        .append(cause != null ? cause : e)
                        .append("\n\n");
            }
        }
        Setting.writeResult("simple_result.txt", simple_result.toString());
        float total_time = (System.nanoTime() - total_start) / 1000000.F;
        final_result.append("====SUMMARY====\n总轮次: ").append(meta.size()).append(String.format("\n总用时: %.3f ms", total_time));
        Setting.writeResult("result.txt", final_result.toString());
        System.out.printf("所有对局已完成\n总用时: %.3f ms%n", total_time);
    }

    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--mode" -> {
                    if (i + 1 < args.length) MODE = args[++i];
                    else { System.err.println("--mode 需要参数"); System.exit(1); }
                }
                case "--port" -> {
                    if (i + 1 < args.length) PORT = Integer.parseInt(args[++i]);
                    else { System.err.println("--port 需要参数"); System.exit(1); }
                }
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                default -> {
                    System.err.println("未知参数: " + args[i]);
                    printHelp();
                    System.exit(1);
                }
            }
        }
    }

    private static void printHelp() {
        System.out.println("""
                用法: java -jar gekitotsu_java.jar [选项]

                选项:
                  --mode <interactive|batch|server>  运行模式（默认 interactive）
                  --port <端口号>                    server 模式监听端口（默认 8080）
                  --help, -h                         显示帮助

                模式说明:
                  interactive 交互菜单，反复对战（默认）
                  batch       跑完所有 1P×2P 对战写文件后退出（等价于旧版 AUTO_PLAY=true）
                  server      启动 HTTP 服务常驻，接受外部程序调用

                配置:
                  上述选项也可通过 config.ini 配置（MODE / PORT / MAX_FRAME_LIMIT 等）
                  命令行参数优先于 config.ini
                """);
    }

    public static CompiledFort compileFort(Fort f) {
        String code = f.code();
        if (code.length() < 6) {
            throw new IllegalArgumentException("阵容代码长度不足: " + f.name() + " [" + code + "]");
        }

        int[] core = to_xyr(code.substring(1, 6));
        int baseSeed = (core[0] % 168 + 48) * (core[1] % 168 + 48);
        int coreX = core[0] - 190;
        int coreY = core[1] - 400;
        int coreType = code.charAt(0) - '0';
        int unitCount = code.length() / 6 - 1;
        int[] type = new int[unitCount];
        int[] x = new int[unitCount];
        int[] y = new int[unitCount];
        int[] r = new int[unitCount];
        int[] seed = new int[unitCount];

        for (int i = 0, j = 6; i < unitCount; i++, j += 6) {
            int[] u = to_xyr(code.substring(j + 1, j + 6));
            int t = Main.pskey.indexOf(code.charAt(j));
            type[i] = t;
            x[i] = u[0];
            y[i] = u[1];
            r[i] = u[2];
            seed[i] = baseSeed * (u[0] % 185 + 30) * (u[1] % 185 + 30);
>>>>>>> Stashed changes
        }
    }

    private static void update() {  //单位更新
        List<Integer> keys = new ArrayList<>(elements.keySet());

        for (Integer key : keys) {
            if (!elements.containsKey(key)) {
                continue;
            }
            elements.get(key).step();
        }
    }

    private static int[] to_xyr(String str){    //5位61进制转x y r数组
        int rxy = 0;
        rxy += pskey.indexOf(str.charAt(0)) * 13845841;
        rxy += pskey.indexOf(str.charAt(1)) * 226981;
        rxy += pskey.indexOf(str.charAt(2)) * 3721;
        rxy += pskey.indexOf(str.charAt(3)) * 61;
        rxy += pskey.indexOf(str.charAt(4));
        String rxy_str = String.format("%09d", rxy);
        return new int[]{Integer.parseInt(rxy_str.substring(3, 6)), Integer.parseInt(rxy_str.substring(6, 9)), Integer.parseInt(rxy_str.substring(0, 3))};
    }
<<<<<<< Updated upstream

    public static int addElement(Shape s){      //增加新元素，返回id值
        elements.put(ID, s);
        return ID++;
    }

    /**
     * @param X x
     * @param Y y
     * @param R 角度
     * @param TYPE 种类 (1-60 间整数)
     * @param S 所属方 (0 或 1)
     */
    public static void unit_make(float X, float Y, int R, int TYPE, int S){   //创建单位
        int wrk = 0;
        switch (TYPE){
            case 1: new BowBall(X, Y, R, S, TYPE);break;
            case 2: new GunBall(X, Y, R, S, TYPE);break;
            case 3: new SwordBall(X, Y, R, S, TYPE);break;
            case 4: new TateBall(X, Y, R, S, TYPE);break;
            case 5: new BombBall(X, Y, R, S, TYPE);break;
            case 6: new MagicBall(X, Y, R, S, TYPE);break;
            case 7: new DokyuBall(X, Y, R, S, TYPE);break;
            case 8: new YariBall(X, Y, R, S, TYPE);break;
            case 9: new CannonBall(X, Y, R, S, TYPE);break;
            case 10: new NagiBall(X, Y, R, S, TYPE);break;
            case 11: new HaneBall(X, Y, R, S, TYPE);break;
            case 12: new RetsuBall(X, Y, R, S, TYPE);break;
            case 13: new ShotgunBall(X, Y, R, S, TYPE);break;
            case 14: new SniperBall(X, Y, R, S, TYPE);break;
            case 15: new UkiBall(X, Y, R, S, TYPE);break;
            case 16: new GuideBall(X, Y, R, S, TYPE);break;
            case 17: new RepairBall(X, Y, R, S, TYPE);break;
            case 18: new HealBall(X, Y, R, S, TYPE);break;
            case 19: new KabeBall(X, Y, R, S, TYPE);break;
            case 20: new TobiBall(X, Y, R, S, TYPE);break;
            case 21: new SenBall(X, Y, R, S, TYPE);break;
            case 22: new MinigunBall(X, Y, R, S, TYPE);break;
            case 23: new HenBall(X, Y, R, S, TYPE);break;
            case 24: new KekkaiBall(X, Y, R, S, TYPE);break;
            case 25: new Wood(X, Y, S, TYPE); wrk = 1; break;
            case 26: new Stone(X, Y, S, TYPE); wrk = 1; break;
            case 27: new Paper(X, Y, S, TYPE); wrk = 1; break;
            case 28: new Iron(X, Y, S, TYPE); wrk = 1; break;
            case 29: new Jet(X, Y, S, TYPE); wrk = 1; break;
            case 30: new Turbo(X, Y, S, TYPE); wrk = 1; break;
            case 31: new BoneBall(X, Y, R, S, TYPE);break;
            case 32: new GekiBall(X, Y, R, S, TYPE);break;
            case 33: new TonBall(X, Y, R, S, TYPE);break;
            case 34: new HolyBall(X, Y, R, S, TYPE);break;
            case 35: new NinBall(X, Y, R, S, TYPE);break;
            case 36: new SyouBall(X, Y, R, S, TYPE);break;
            case 37: new HanaBall(X, Y, R, S, TYPE);break;
            case 38: new HanBall(X, Y, R, S, TYPE);break;
            case 39: new PushBall(X, Y, R, S, TYPE);break;
            case 40: new GeiBall(X, Y, R, S, TYPE);break;
            case 41: new NieBall(X, Y, R, S, TYPE);break;
            case 42: new TargetBall(X, Y, R, S, TYPE);break;
            case 43: new TuiBall(X, Y, R, S, TYPE);break;
            case 44: new BoxBall(X, Y, R, S, TYPE);break;
            case 45: new DarkBall(X, Y, R, S, TYPE);break;
            case 46: new HeriBall(X, Y, R, S, TYPE);break;
            case 47: new SaiBall(X, Y, R, S, TYPE);break;
            case 48: new KnightBall(X, Y, R, S, TYPE);break;
            case 49: new KakuBall(X, Y, R, S, TYPE);break;
            case 50: new ShaBall(X, Y, R, S, TYPE);break;
            case 51: new StarBall(X, Y, R, S, TYPE);break;
            case 52: new ConBall(X, Y, R, S, TYPE);break;
            case 53: new KanBall(X, Y, R, S, TYPE);break;
            case 54: new SearchBall(X, Y, R, S, TYPE);break;
            case 55: new Near(X, Y, S, TYPE); wrk = 1; break;
            case 56: new Far(X, Y, S, TYPE); wrk = 1; break;
            case 57: new Wide(X, Y, S, TYPE); wrk = 1; break;
            case 58: new Narrow(X, Y, S, TYPE); wrk = 1; break;
            case 59: new Snipe(X, Y, S, TYPE); wrk = 1; break;
            case 60: new Elevator(X, Y, S, TYPE); wrk = 1; break;
            default: new Ball(X, Y, R, S, TYPE);break;
        }
        switch (wrk){
            case 0: {
                Ball unit = (Ball) elements.get(ID - 1);
                if (unit.side != 0) {
                    unit.cnt = (int) (-(380 - (unit.x - 1490)) % unit.speed);
                }
                else {
                    unit.cnt = (int) (-(unit.x - 50) % unit.speed);
                }
                break;
            }
            case 1: break;
            default: break;
        }
    }

    private static void resetGame() {
        end = false;
        time = 0;
        ID = 0;

        elements.clear();

        hp = new int[]{100, 100};
        hp0_flg = new int[]{0, 0};
        dead_last = new int[]{0, 0};

        for (int i = 0; i < 2; i++) {
            wall[i] = new CompositeShape(0,0);
            unit[i] = new CompositeShape(0,0);
            shield[i] = new CompositeShape(0,0);
            atk[i] = new CompositeShape(0,0);
            fort[i] = new CompositeShape(0,0);
            team[i] = new CompositeShape(0,0);
            heal[i] = new CompositeShape(0,0);
            repair[i] = new CompositeShape(0,0);
            jump_u[i] = new CompositeShape(0,0);
            jump_f[i] = new CompositeShape(0,0);
            snipe[i] = new CompositeShape(0,0);
            turn_ccw[i] = new CompositeShape(0,0);
            turn_cw[i] = new CompositeShape(0,0);
            kekkaiIds = new LinkedList[]{new LinkedList<Integer>(), new LinkedList<Integer>()};
            kekkaiFields = new KekkaiField[]{null, null};
        }

        for (int i = 0; i <= 1; i++){
            fort[i].addShape(wall[i]);
            fort[i].addShape(unit[i]);
        }
        for (int i = 0; i <= 1; i++){
            team[i].addShape(wall[i]);
            team[i].addShape(unit[i]);
            team[i].addShape(shield[i]);
            team[i].addShape(atk[i]);
        }

        bases = new Base[]{new Base(240, 532, 0), new Base(1680, 532, 1)};
    }

    private static String getSimpleResult(){
        if (hp0_flg[0] > 0 || hp0_flg[1] > 0){
            if(hp0_flg[0] == 0){
                return "1,";
            }else if(hp0_flg[1] == 0) {
                return "2,";
            }else {
                return "d,";
            }
        }else {
            return "?,";
        }
    }
=======
>>>>>>> Stashed changes
}
