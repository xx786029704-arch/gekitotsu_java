package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 全自动更新安装器：下载新版本 zip → 解压校验 → 生成延迟替换脚本 →
 * 退出当前进程，由脚本等待进程结束后覆盖安装目录并重启新版本。
 * 仅在 jpackage app-image 环境（安装目录可写、非临时目录）可用，
 * 其余场景由调用方回退为浏览器手动下载。
 */
final class UpdateInstaller {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private UpdateInstaller() {}

    /** app-image 的安装目录与启动器 exe。 */
    record Target(Path dir, Path exe) {}

    /** 是否具备自动更新条件：存在 zip 附件且运行于可写的 app-image 安装目录。 */
    static boolean canAutoUpdate(UpdateChecker.Release rel) {
        return rel.zipUrl() != null && resolveTarget() != null;
    }

    /** 在 EDT 调用：显示进度对话框并后台完成下载、替换与重启；失败时回退打开浏览器。 */
    static void install(Component parent, UpdateChecker.Release rel, Runnable exitAction) {
        Target target = resolveTarget();
        if (target == null || rel.zipUrl() == null) {
            UpdateChecker.openInBrowser(parent, rel.zipUrl() != null ? rel.zipUrl() : rel.pageUrl());
            return;
        }

        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;

        JLabel statusLabel = new JLabel(I18n.t("update.downloading"));
        statusLabel.setFont(I18n.font(Font.PLAIN, 12));
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(380, 20));
        JButton cancelBtn = new JButton(I18n.t("btn.cancel"));
        cancelBtn.setFont(I18n.font(Font.PLAIN, 12));

        JPanel content = new JPanel(new BorderLayout(10, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        content.add(statusLabel, BorderLayout.NORTH);
        content.add(bar, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.add(cancelBtn);
        content.add(south, BorderLayout.SOUTH);

        JDialog dialog = new JDialog(owner, I18n.t("update.title"), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);

        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancelBtn.addActionListener(e -> {
            cancelled.set(true);
            dialog.dispose();
        });

        Thread worker = new Thread(() -> {
            Path base = null;
            try {
                base = Files.createTempDirectory("gekitotsu_update_");
                Path zip = base.resolve("package.zip");
                download(rel.zipUrl(), zip, bar, cancelled);
                if (cancelled.get()) throw new CancellationException();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText(I18n.t("update.extracting"));
                    bar.setIndeterminate(true);
                    bar.setString("");
                });
                Path src = extract(zip, base.resolve("new"));
                if (cancelled.get()) throw new CancellationException();
                Path script = writeScript(base, zip, src, target);
                new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                        "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString())
                        .redirectErrorStream(true)
                        .start();
                SwingUtilities.invokeLater(() -> {
                    dialog.dispose();
                    exitAction.run();
                });
            } catch (CancellationException c) {
                cleanup(base);
                SwingUtilities.invokeLater(dialog::dispose);
            } catch (Exception ex) {
                cleanup(base);
                SwingUtilities.invokeLater(() -> {
                    dialog.dispose();
                    JOptionPane.showMessageDialog(parent,
                            I18n.t("update.autoFailed", String.valueOf(ex.getMessage())),
                            I18n.t("update.title"), JOptionPane.ERROR_MESSAGE);
                    UpdateChecker.openInBrowser(parent, rel.zipUrl() != null ? rel.zipUrl() : rel.pageUrl());
                });
            }
        }, "update-install");
        worker.setDaemon(true);
        worker.start();

        // 模态等待：worker 完成或用户取消后 dispose 返回
        dialog.setVisible(true);
    }

    /** 定位当前安装目录；无法定位或不可写时返回 null。 */
    static Target resolveTarget() {
        Path dir = null;
        Path exe = null;
        // 1) jpackage 启动器会设置 jpackage.app-path 指向正在运行的 exe
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isEmpty()) {
            Path p = Paths.get(appPath).toAbsolutePath().normalize();
            if (Files.isRegularFile(p)) {
                dir = p.getParent();
                exe = p;
            }
        }
        // 2) fat JAR 位于 <app-image>/app/ 目录时反推
        if (dir == null) {
            try {
                java.security.CodeSource cs = UpdateInstaller.class.getProtectionDomain().getCodeSource();
                if (cs != null && cs.getLocation() != null) {
                    Path loc = Paths.get(cs.getLocation().toURI()).toAbsolutePath().normalize();
                    if (Files.isRegularFile(loc) && loc.toString().toLowerCase().endsWith(".jar")
                            && loc.getParent() != null && "app".equals(loc.getParent().getFileName().toString())) {
                        dir = loc.getParent().getParent();
                    }
                }
            } catch (Exception ignored) {}
        }
        if (dir == null || !Files.isDirectory(dir.resolve("app")) || !Files.isWritable(dir)) {
            return null;
        }
        // 从压缩包临时解压目录运行时无法自替换
        String tmp = System.getProperty("java.io.tmpdir", "");
        if (!tmp.isEmpty() && dir.startsWith(Paths.get(tmp).toAbsolutePath().normalize())) {
            return null;
        }
        if (exe == null || !Files.isRegularFile(exe)) {
            exe = findExe(dir);
        }
        return exe != null ? new Target(dir, exe) : null;
    }

    /** 在安装目录中找启动器 exe（优先与目录同名者）。 */
    private static Path findExe(Path dir) {
        String want = dir.getFileName() + ".exe";
        try (var stream = Files.list(dir)) {
            List<Path> exes = stream
                    .filter(f -> f.getFileName().toString().toLowerCase().endsWith(".exe"))
                    .toList();
            for (Path f : exes) {
                if (f.getFileName().toString().equalsIgnoreCase(want)) {
                    return f;
                }
            }
            return exes.isEmpty() ? null : exes.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /** 下载 zip（带进度与取消检查）。 */
    private static void download(String url, Path out, JProgressBar bar, AtomicBoolean cancelled) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("User-Agent", "GekitotsuKit/" + Main.VERSION)
                .GET()
                .build();
        HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode());
        }
        long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
        long done = 0;
        long lastUpdate = 0;
        byte[] buf = new byte[65536];
        try (InputStream in = resp.body(); OutputStream fos = Files.newOutputStream(out)) {
            int n;
            while ((n = in.read(buf)) >= 0) {
                if (cancelled.get()) throw new CancellationException();
                fos.write(buf, 0, n);
                done += n;
                long now = System.nanoTime();
                if (now - lastUpdate > 100_000_000L) {
                    lastUpdate = now;
                    final long d = done;
                    final long t = total;
                    SwingUtilities.invokeLater(() -> {
                        if (t > 0) {
                            bar.setValue((int) Math.min(100, d * 100 / t));
                        }
                        bar.setString(formatSize(d) + (t > 0 ? " / " + formatSize(t) : ""));
                    });
                }
            }
        }
        final long d = done;
        final long t = total;
        SwingUtilities.invokeLater(() -> {
            if (t > 0) bar.setValue(100);
            bar.setString(formatSize(d) + (t > 0 ? " / " + formatSize(t) : ""));
        });
    }

    /** 解压 zip 到 dest 并返回实际内容目录（兼容带一层顶层目录的压缩包）；防 Zip Slip。 */
    private static Path extract(Path zip, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(zip), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                Path out = dest.resolve(entry.getName()).normalize();
                if (!out.startsWith(dest)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Path src = dest;
        if (!Files.isDirectory(src.resolve("app"))) {
            try (var stream = Files.list(dest)) {
                List<Path> subDirs = stream.filter(Files::isDirectory).toList();
                if (subDirs.size() == 1 && Files.isDirectory(subDirs.get(0).resolve("app"))) {
                    src = subDirs.get(0);
                }
            }
        }
        if (!Files.isDirectory(src.resolve("app"))) {
            throw new IOException("invalid update package: app/ not found");
        }
        return src;
    }

    /** 生成延迟替换脚本（UTF-8 BOM，PowerShell）：等待本进程退出 → robocopy 覆盖 → 重启新版 → 清理。 */
    private static Path writeScript(Path base, Path zip, Path src, Target target) throws IOException {
        String content = "$ErrorActionPreference = 'SilentlyContinue'\r\n"
                + "$targetPid = " + ProcessHandle.current().pid() + "\r\n"
                + "$src = '" + psQuote(src.toString()) + "'\r\n"
                + "$dst = '" + psQuote(target.dir().toString()) + "'\r\n"
                + "$exe = '" + psQuote(target.exe().toString()) + "'\r\n"
                + "$zip = '" + psQuote(zip.toString()) + "'\r\n"
                + "for ($i = 0; $i -lt 240; $i++) {\r\n"
                + "    if (-not (Get-Process -Id $targetPid -ErrorAction SilentlyContinue)) { break }\r\n"
                + "    Start-Sleep -Milliseconds 500\r\n"
                + "}\r\n"
                + "Start-Sleep -Milliseconds 800\r\n"
                + "for ($i = 0; $i -lt 30; $i++) {\r\n"
                + "    robocopy $src $dst /E /R:1 /W:1 /NJH /NJS /NDL /NFL /NP"
                + " /XF config.ini 1P.txt 2P.txt result.txt simple_result.txt battle_cache.bin strength_cache.bin"
                + " | Out-Null\r\n"
                + "    if ($LASTEXITCODE -lt 8) { break }\r\n"
                + "    Start-Sleep -Seconds 1\r\n"
                + "}\r\n"
                + "Start-Process -FilePath $exe -WorkingDirectory (Split-Path -LiteralPath $exe)\r\n"
                + "Remove-Item -LiteralPath $src -Recurse -Force\r\n"
                + "Remove-Item -LiteralPath $zip -Force\r\n"
                + "Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force\r\n";
        Path script = base.resolve("update.ps1");
        Files.write(script, ("\uFEFF" + content).getBytes(StandardCharsets.UTF_8));
        return script;
    }

    /** PowerShell 单引号字符串转义。 */
    private static String psQuote(String s) {
        return s.replace("'", "''");
    }

    private static String formatSize(long bytes) {
        return String.format("%.1f MB", bytes / 1048576.0);
    }

    /** 失败/取消时清理临时目录。 */
    private static void cleanup(Path base) {
        if (base == null) return;
        try (var stream = Files.walk(base)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {}
            });
        } catch (Exception ignored) {}
    }
}
