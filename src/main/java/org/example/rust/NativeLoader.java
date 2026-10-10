package org.example.rust;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** 定位并加载 gkt-jni 原生库（gkt_jni.dll）；失败时记录原因供 GUI 展示。 */
final class NativeLoader {

    private static final String LIB_NAME = "gkt_jni";

    private NativeLoader() {
    }

    /** 尝试加载原生库；成功返回 null，失败返回原因描述。 */
    static synchronized String load() {
        List<String> attempts = new ArrayList<>();

        String explicit = System.getProperty("gekitotsu.native.path");
        if (explicit == null || explicit.isEmpty()) {
            explicit = System.getenv("GEKITOTSU_NATIVE_PATH");
        }
        if (explicit != null && !explicit.isEmpty()) {
            Path path = Paths.get(explicit);
            String error = tryLoadFile(path);
            if (error == null) {
                return null;
            }
            attempts.add(path + " -> " + error);
        }

        for (Path candidate : fileCandidates()) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            String error = tryLoadFile(candidate);
            if (error == null) {
                return null;
            }
            attempts.add(candidate + " -> " + error);
        }

        String resourceError = tryLoadResource(attempts);
        if (resourceError == null) {
            return null;
        }
        attempts.add(resourceError);

        return "未找到可用的 " + libraryFileName() + "；尝试过：" + String.join("；", attempts);
    }

    private static List<Path> fileCandidates() {
        List<Path> out = new ArrayList<>();
        // 工作目录（1P.txt/config.ini 所在目录）
        Path cwd = Paths.get("").toAbsolutePath();
        out.add(cwd.resolve("native").resolve(libraryFileName()));
        out.add(cwd.resolve(libraryFileName()));
        out.add(cwd.resolve("rust").resolve(libraryFileName()));
        // 程序/JAR 所在目录
        try {
            Path jar = Paths.get(NativeLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path base = Files.isDirectory(jar) ? jar : jar.getParent();
            if (base != null) {
                out.add(base.resolve(libraryFileName()));
                out.add(base.resolve("native").resolve(libraryFileName()));
                out.add(base.getParent() != null
                        ? base.getParent().resolve(libraryFileName()) : null);
            }
        } catch (Exception ignored) {
        }
        out.removeIf(java.util.Objects::isNull);
        return out;
    }

    private static String tryLoadFile(Path path) {
        try {
            System.load(path.toAbsolutePath().toString());
            return null;
        } catch (Throwable t) {
            return t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    /** 从 classpath（fat JAR 内 /native/）提取到临时目录后加载；成功返回 null。 */
    private static String tryLoadResource(List<String> attempts) {
        try (InputStream in = NativeLoader.class.getResourceAsStream("/native/" + libraryFileName())) {
            if (in == null) {
                return "classpath:/native/" + libraryFileName() + " 不存在";
            }
            byte[] bytes = in.readAllBytes();
            String hash = sha256Hex(bytes).substring(0, 16);
            Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "gekitotsu-native");
            Files.createDirectories(dir);
            Path target = dir.resolve(LIB_NAME + "-" + hash + suffix());
            if (!Files.isRegularFile(target)) {
                Path tmp = Files.createTempFile(dir, LIB_NAME, suffix());
                Files.write(tmp, bytes);
                try {
                    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.load(target.toAbsolutePath().toString());
            return null;
        } catch (Throwable t) {
            return "classpath:/native/" + libraryFileName() + " -> "
                    + t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    private static String libraryFileName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return LIB_NAME + ".dll";
        }
        if (os.contains("mac")) {
            return "lib" + LIB_NAME + ".dylib";
        }
        return "lib" + LIB_NAME + ".so";
    }

    private static String suffix() {
        String name = libraryFileName();
        return name.substring(name.lastIndexOf('.'));
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(bytes.length);
        }
    }
}
