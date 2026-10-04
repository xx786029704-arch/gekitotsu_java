package org.example.GUI;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;

/** 扫描 effects/ 目录下的 JAR，通过 ServiceLoader 发现 Effect 实现并注册。 */
public class PluginLoader {

    private static final String PLUGIN_DIR = "effects";

    public static void scanPlugins() {
        Path dir = Paths.get(PLUGIN_DIR);
        if (!Files.isDirectory(dir)) return;

        File[] jars = dir.toFile().listFiles((d, name) -> name.endsWith(".jar") && !name.startsWith("_"));
        if (jars == null) return;

        for (File jar : jars) {
            try {
                URL[] urls = { jar.toURI().toURL() };
                URLClassLoader cl = new URLClassLoader(urls, PluginLoader.class.getClassLoader());
                ServiceLoader<Effect> loader = ServiceLoader.load(Effect.class, cl);
                for (Effect effect : loader) {
                    EffectRegistry.register(effect);
                }
            } catch (Exception e) {
                System.err.println("[PluginLoader] 加载插件失败: " + jar.getName() + " - " + e.getMessage());
            }
        }
    }
}
