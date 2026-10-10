package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 更新检查。数据源为 GitHub Releases 的 latest 接口，
 * 网络请求全部在后台线程执行、失败静默，弹窗回到 EDT。
 * 用户勾选「不再提示此版本」时把版本号写入 config.ini（SKIP_UPDATE_VERSION），
 * 更高版本发布后仍会提示。
 */
public final class UpdateChecker {

    private static final String REPO = "xx786029704-arch/gekitotsu_java";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final String RELEASES_PAGE = "https://github.com/" + REPO + "/releases";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** 每次进程启动只自动检查一次（语言切换会重建主窗口）。 */
    private static boolean startupChecked = false;

    private UpdateChecker() {}

    /** 最新 Release 信息。 */
    public record Release(String version, String name, String pageUrl, String zipUrl) {}

    /** 启动时自动检查；发现新版本且未被用户跳过时，延迟片刻弹窗（不阻塞启动）。 */
    public static synchronized void checkOnStartup(Component parent, Runnable exitAction) {
        if (startupChecked) return;
        startupChecked = true;
        Thread t = new Thread(() -> {
            Release rel = fetchLatest();
            if (rel == null || !isNewer(rel.version(), Main.VERSION)) return;
            if (rel.version().equals(Main.SKIP_UPDATE_VERSION)) return;
            SwingUtilities.invokeLater(() -> {
                Timer timer = new Timer(1500, e -> showUpdateDialog(parent, rel, exitAction));
                timer.setRepeats(false);
                timer.start();
            });
        }, "update-check");
        t.setDaemon(true);
        t.start();
    }

    /** 手动检查：无论是否已跳过都显示结果；完成后在 EDT 调用 onDone。 */
    public static void checkManual(Component parent, Runnable onDone, Runnable exitAction) {
        Thread t = new Thread(() -> {
            Release rel = fetchLatest();
            boolean newer = rel != null && isNewer(rel.version(), Main.VERSION);
            SwingUtilities.invokeLater(() -> {
                if (rel == null) {
                    JOptionPane.showMessageDialog(parent, I18n.t("update.checkFailed"),
                            I18n.t("update.title"), JOptionPane.WARNING_MESSAGE);
                } else if (!newer) {
                    JOptionPane.showMessageDialog(parent, I18n.t("update.upToDate", Main.VERSION),
                            I18n.t("update.title"), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showUpdateDialog(parent, rel, exitAction);
                }
                if (onDone != null) onDone.run();
            });
        }, "update-check-manual");
        t.setDaemon(true);
        t.start();
    }

    /** 请求 GitHub latest Release；任何异常或非 200 均返回 null。 */
    private static Release fetchLatest() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "GekitotsuKit/" + Main.VERSION)
                    .GET()
                    .build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            return parseRelease(resp.body());
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析 GitHub Release JSON；版本号非法或 JSON 损坏时返回 null。优先取 zip 附件的下载地址。 */
    static Release parseRelease(String body) {
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            String version = normalizeVersion(root.path("tag_name").asText(""));
            if (version == null) return null;
            String name = root.path("name").asText("");
            String pageUrl = root.path("html_url").asText(RELEASES_PAGE);
            String zipUrl = null;
            for (JsonNode asset : root.path("assets")) {
                if (asset.path("name").asText("").toLowerCase().endsWith(".zip")) {
                    zipUrl = asset.path("browser_download_url").asText(null);
                    break;
                }
            }
            return new Release(version, name, pageUrl, zipUrl);
        } catch (Exception e) {
            return null;
        }
    }

    /** 去除 tag 的 v 前缀并校验 x.y.z 数字形式；非法返回 null。 */
    private static String normalizeVersion(String tag) {
        if (tag == null) return null;
        String v = tag.trim().replaceFirst("^[vV]\\s*", "");
        return v.matches("\\d+(\\.\\d+)*") ? v : null;
    }

    /** remote 是否严格新于 local（按数字段比较，如 1.10.0 > 1.9.9）。 */
    static boolean isNewer(String remote, String local) {
        String[] a = remote.split("\\.");
        String[] b = local.split("\\.");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length ? Integer.parseInt(a[i]) : 0;
            int y = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    /** 更新提示对话框：立即更新（或前往下载）/ 稍后再说 + 「不再提示此版本」勾选。 */
    private static void showUpdateDialog(Component parent, Release rel, Runnable exitAction) {
        JCheckBox skipBox = new JCheckBox(I18n.t("update.skipVersion"));
        skipBox.setSelected(rel.version().equals(Main.SKIP_UPDATE_VERSION));

        StringBuilder html = new StringBuilder("<html><div style='width:280px;'>");
        html.append(I18n.t("update.found", rel.version(), Main.VERSION));
        if (!rel.name().isEmpty()) {
            html.append("<br><br>").append(escapeHtml(rel.name()));
        }
        html.append("</div></html>");
        JLabel label = new JLabel(html.toString());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        skipBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(skipBox);

        boolean auto = UpdateInstaller.canAutoUpdate(rel);
        Object[] options = { I18n.t(auto ? "update.installNow" : "update.download"), I18n.t("update.later") };
        int choice = JOptionPane.showOptionDialog(parent, panel, I18n.t("update.foundTitle"),
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (skipBox.isSelected()) {
            Main.SKIP_UPDATE_VERSION = rel.version();
            Setting.saveConfig();
        } else if (rel.version().equals(Main.SKIP_UPDATE_VERSION)) {
            Main.SKIP_UPDATE_VERSION = "";
            Setting.saveConfig();
        }
        if (choice != 0) {
            return;
        }
        if (auto) {
            int confirm = JOptionPane.showConfirmDialog(parent, I18n.t("update.confirmRestart"),
                    I18n.t("update.foundTitle"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                UpdateInstaller.install(parent, rel, exitAction);
            }
        } else {
            openInBrowser(parent, rel.zipUrl() != null ? rel.zipUrl() : rel.pageUrl());
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 用默认浏览器打开链接；失败时提示手动访问。 */
    static void openInBrowser(Component parent, String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {}
        JOptionPane.showMessageDialog(parent, I18n.t("update.openFailed", url),
                I18n.t("update.title"), JOptionPane.INFORMATION_MESSAGE);
    }
}
