package org.example;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.zip.CRC32;

/**
 * 对局级持久缓存：以「引擎 + 帧上限 + 双方阵型代码」为键缓存单局结果，各分析共用。
 *
 * <p>阵集增加/删除阵型时，只需补跑缺失的对局，其余直接复用；缓存跨进程有效。
 * 内存按最近最少使用（LRU）上限 {@value #MAX_ENTRIES} 条；磁盘为追加日志
 * （工作目录 {@code battle_cache.bin}，每条 {@value #RECORD_BYTES} 字节、带 CRC 校验），
 * 超过 16 MB 时压缩重写为当前条目。文件损坏或版本不符时按空缓存处理。
 */
public final class BattleCache {

    private static final String FILE_NAME = "battle_cache.bin";
    private static final int MAGIC = 0x474B4C47;
    private static final int VERSION = 1;
    private static final int MAX_ENTRIES = 200_000;
    private static final long MAX_FILE_BYTES = 16L * 1024 * 1024;
    private static final int DATA_BYTES = 32;
    private static final int RECORD_BYTES = DATA_BYTES + 4;

    private record Game(int status, int winnerHp, int framePassed, float timeUsed) {}

    private record Key(long a, long b) {}

    private static final Object LOCK = new Object();
    private static final LinkedHashMap<Key, Game> MAP = new LinkedHashMap<>(1 << 14, 0.75f, true);
    private static boolean loaded;
    private static DataOutputStream out;
    private static long fileBytes;

    private BattleCache() {}

    /** 读取单局缓存；未命中返回 null。 */
    public static Result get(String engine, String code1, String code2, int maxFrames) {
        synchronized (LOCK) {
            ensureLoaded();
            Game game = MAP.get(key(engine, code1, code2, maxFrames));
            return game == null ? null
                    : new Result(game.status(), game.winnerHp(), game.framePassed(), game.timeUsed());
        }
    }

    /** 便捷方法：命中直接返回，未命中执行 {@code supplier} 并写入缓存。 */
    public static Result cached(String engine, String code1, String code2, int maxFrames,
                                java.util.function.Supplier<Result> supplier) {
        Result result = get(engine, code1, code2, maxFrames);
        if (result != null) {
            return result;
        }
        result = supplier.get();
        put(engine, code1, code2, maxFrames, result);
        return result;
    }

    /** 写入单局缓存；异常结果（status=-2）不缓存。 */
    public static void put(String engine, String code1, String code2, int maxFrames, Result result) {
        if (result == null || result.status == -2) {
            return;
        }
        synchronized (LOCK) {
            ensureLoaded();
            Key k = key(engine, code1, code2, maxFrames);
            Game game = new Game(result.status, result.winnerHp, result.framePassed, result.timeUsed);
            MAP.put(k, game);
            append(k, game);
            while (MAP.size() > MAX_ENTRIES) {
                var it = MAP.keySet().iterator();
                it.next();
                it.remove();
            }
        }
    }

    /** 缓存键：引擎 + 帧上限 + 双方代码，SHA-256 截断为 128 位。 */
    private static Key key(String engine, String code1, String code2, int maxFrames) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(engine.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '|');
            digest.update(code1.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '|');
            digest.update(code2.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) '|');
            digest.update(ByteBuffer.allocate(4).putInt(maxFrames).array());
            byte[] hash = digest.digest();
            ByteBuffer buffer = ByteBuffer.wrap(hash);
            return new Key(buffer.getLong(), buffer.getLong());
        } catch (Exception ex) {
            String s = engine + '|' + code1 + '|' + code2 + '|' + maxFrames;
            return new Key(s.hashCode(), s.hashCode() * 31L);
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = Paths.get(FILE_NAME);
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path), 1 << 16))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                return;
            }
            byte[] record = new byte[RECORD_BYTES];
            CRC32 crc = new CRC32();
            while (true) {
                try {
                    in.readFully(record);
                } catch (EOFException eof) {
                    break; // 文件尾部的半条记录直接忽略
                }
                crc.reset();
                crc.update(record, 0, DATA_BYTES);
                if ((int) crc.getValue() != readInt(record, DATA_BYTES)) {
                    continue; // 校验失败（如并发写入交错）则跳过该条
                }
                ByteBuffer buffer = ByteBuffer.wrap(record);
                Key k = new Key(buffer.getLong(), buffer.getLong());
                Game game = new Game(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getFloat());
                MAP.put(k, game);
                while (MAP.size() > MAX_ENTRIES) {
                    var it = MAP.keySet().iterator();
                    it.next();
                    it.remove();
                }
            }
        } catch (IOException | RuntimeException ex) {
            MAP.clear();
        }
    }

    private static void append(Key k, Game game) {
        try {
            if (out == null) {
                openForAppend();
            }
            byte[] record = new byte[RECORD_BYTES];
            ByteBuffer buffer = ByteBuffer.wrap(record);
            buffer.putLong(k.a());
            buffer.putLong(k.b());
            buffer.putInt(game.status());
            buffer.putInt(game.winnerHp());
            buffer.putInt(game.framePassed());
            buffer.putFloat(game.timeUsed());
            CRC32 crc = new CRC32();
            crc.update(record, 0, DATA_BYTES);
            buffer.putInt((int) crc.getValue());
            out.write(record);
            out.flush();
            fileBytes += RECORD_BYTES;
            if (fileBytes > MAX_FILE_BYTES) {
                compact();
            }
        } catch (IOException ex) {
            closeQuietly();
        }
    }

    private static void openForAppend() throws IOException {
        Path path = Paths.get(FILE_NAME);
        long size = Files.isRegularFile(path) ? Files.size(path) : 0;
        boolean validHeader = false;
        if (size >= 8) {
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(path)))) {
                validHeader = in.readInt() == MAGIC && in.readInt() == VERSION;
            } catch (IOException ignored) {
                validHeader = false;
            }
        }
        if (!validHeader) {
            // 文件不存在、过短或头部不符：重建（截断）后写入头部
            try (DataOutputStream fresh = new DataOutputStream(new BufferedOutputStream(
                    Files.newOutputStream(path, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING), 1 << 16))) {
                fresh.writeInt(MAGIC);
                fresh.writeInt(VERSION);
            }
            size = 8;
        }
        out = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND),
                1 << 16));
        fileBytes = size;
    }

    /** 压缩重写：只保留当前内存中的条目。 */
    private static void compact() {
        closeQuietly();
        Path path = Paths.get(FILE_NAME);
        Path tmp = Paths.get(FILE_NAME + ".tmp");
        try (DataOutputStream w = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(tmp, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING), 1 << 16))) {
            w.writeInt(MAGIC);
            w.writeInt(VERSION);
            byte[] record = new byte[RECORD_BYTES];
            CRC32 crc = new CRC32();
            for (var e : MAP.entrySet()) {
                ByteBuffer buffer = ByteBuffer.wrap(record);
                buffer.putLong(e.getKey().a());
                buffer.putLong(e.getKey().b());
                buffer.putInt(e.getValue().status());
                buffer.putInt(e.getValue().winnerHp());
                buffer.putInt(e.getValue().framePassed());
                buffer.putFloat(e.getValue().timeUsed());
                crc.reset();
                crc.update(record, 0, DATA_BYTES);
                buffer.putInt((int) crc.getValue());
                w.write(record);
            }
        } catch (IOException ex) {
            return;
        }
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            fileBytes = 8L + (long) MAP.size() * RECORD_BYTES;
        } catch (IOException ignored) {
            // 移动失败时保持旧文件，下次追加再试
        }
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }

    private static void closeQuietly() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
                // 忽略
            }
            out = null;
        }
    }
}
