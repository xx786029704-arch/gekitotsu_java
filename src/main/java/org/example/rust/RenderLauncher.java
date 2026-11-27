package org.example.rust;

import org.example.Fort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 定位并启动 Rust 对战渲染器 {@code gkt-render.exe}（独立进程的观战窗口）。
 *
 * <p>阵容由工具箱按自身解析规则（{@code Setting.parseFortsRaw}）预先清洗，
 * 写成渲染器的逐行预设格式后通过 {@code --p1-file/--p2-file} 传入，
 * 保证观战窗口与模拟使用同一份阵容；第一条为默认对局，其余可在窗口内切换。
 * 渲染器在独立进程中运行，崩溃不会影响工具箱。
 */
public final class RenderLauncher {

    /** 启动后在此时间内非零退出视为启动失败（如显卡不可用），回报给调用方。 */
    private static final long EARLY_EXIT_WINDOW_MS = 5000;

    private RenderLauncher() {
    }

    /** 定位结果：找到时 {@code path} 非空，否则 {@code attempts} 列出尝试过的位置。 */
    public record Location(Path path, List<String> attempts) {
    }

    /** 按 系统属性 → 环境变量 → 工作目录 → JAR/classes 同目录 的顺序查找渲染器。 */
    public static Location locate() {
        List<String> attempts = new ArrayList<>();
        List<Path> candidates = new ArrayList<>();
        String explicit = System.getProperty("gekitotsu.render.path");
        if (explicit == null || explicit.isEmpty()) {
            explicit = System.getenv("GEKITOTSU_RENDER_PATH");
        }
        if (explicit != null && !explicit.isEmpty()) {
            candidates.add(Paths.get(explicit));
        }
        String exe = exeFileName();
        Path cwd = Paths.get("").toAbsolutePath();
        candidates.add(cwd.resolve("native").resolve(exe));
        candidates.add(cwd.resolve(exe));
        candidates.add(cwd.resolve("rust").resolve(exe));
        try {
            Path jar = Paths.get(RenderLauncher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path base = Files.isDirectory(jar) ? jar : jar.getParent();
            if (base != null) {
                candidates.add(base.resolve(exe));
                candidates.add(base.resolve("native").resolve(exe));
                candidates.add(base.getParent() != null ? base.getParent().resolve(exe) : null);
            }
        } catch (Exception ignored) {
        }
        candidates.removeIf(Objects::isNull);
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return new Location(candidate.toAbsolutePath(), attempts);
            }
            attempts.add(candidate.toAbsolutePath().toString());
        }
        return new Location(null, attempts);
    }

    /**
     * 启动观战窗口。
     *
     * @param onEarlyExit 渲染器在启动后短时间内异常退出时回调（退出码, 日志末尾），在后台线程调用
     */
    public static Process launch(Path exe, List<Fort> p1, List<Fort> p2, int maxFrames,
                                 BiConsumer<Integer, String> onEarlyExit) throws IOException {
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "gekitotsu-render");
        Files.createDirectories(dir);
        Path p1File = writePresets(dir, "1P-", p1);
        Path p2File = writePresets(dir, "2P-", p2);
        Path log = Files.createTempFile(dir, "gkt-render-", ".log");
        log.toFile().deleteOnExit();

        List<String> cmd = new ArrayList<>();
        cmd.add(exe.toString());
        cmd.add("--p1-file");
        cmd.add(p1File.toString());
        cmd.add("--p2-file");
        cmd.add(p2File.toString());
        cmd.add("--max-frames");
        cmd.add(String.valueOf(Math.max(1, maxFrames)));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        Path parent = exe.getParent();
        if (parent != null) {
            pb.directory(parent.toFile());
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(log.toFile());
        long start = System.currentTimeMillis();
        Process process = pb.start();
        process.onExit().thenAccept(p -> {
            int code = p.exitValue();
            if (code != 0 && System.currentTimeMillis() - start < EARLY_EXIT_WINDOW_MS
                    && onEarlyExit != null) {
                onEarlyExit.accept(code, tail(log));
            }
        });
        return process;
    }

    /** 写出渲染器预设文件：每行 {@code /名称&代码}（前导 '/' 防止以 '#' 开头的名称被当作注释）。 */
    private static Path writePresets(Path dir, String prefix, List<Fort> forts) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Fort fort : forts) {
            String name = fort.name().replaceAll("[\\r\\n]", " ");
            sb.append('/').append(name).append('&').append(fort.code()).append('\n');
        }
        Path file = Files.createTempFile(dir, prefix, ".txt");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        file.toFile().deleteOnExit();
        return file;
    }

    private static String tail(Path log) {
        try {
            // 等待输出落盘
            TimeUnit.MILLISECONDS.sleep(100);
            String text = Files.readString(log, StandardCharsets.UTF_8).strip();
            return text.length() > 600 ? "..." + text.substring(text.length() - 600) : text;
        } catch (Exception e) {
            return "";
        }
    }

    private static String exeFileName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "gkt-render.exe" : "gkt-render";
    }
}
