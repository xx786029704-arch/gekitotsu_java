package org.example;

import java.util.Arrays;

public class IntShapeMap {

    // ===== 常量 =====
    private static final int EMPTY = -1;
    private static final int TOMBSTONE = -2;

    // ===== 哈希表 =====
    private int[] hashKeys;
    private int[] hashValues;
    private int capacity;
    private int mask;
    private int usedSlots; // 包含 tombstone

    // ===== 顺序存储 =====
    public Shape[] items;
    private int[] itemKeys;
    public int size;

    // ===== 删除统计 =====
    public int deadCount;

    // ===== 构造 =====
    public IntShapeMap(int initialCapacity) {
        int cap = 1;
        while (cap < initialCapacity * 2) cap <<= 1; // hash 表更大

        capacity = cap;
        mask = capacity - 1;

        hashKeys = new int[capacity];
        Arrays.fill(hashKeys, EMPTY);
        hashValues = new int[capacity];

        items = new Shape[initialCapacity];
        itemKeys = new int[initialCapacity];

        size = 0;
        deadCount = 0;
        usedSlots = 0;
    }

    // ===== 哈希函数 =====
    private int hash(int key) {
        return (key ^ (key >>> 16)) & mask;
    }

    // ===== put =====
    public void put(int key, Shape value) {

        //  负载因子控制（必须）
        if (usedSlots >= capacity * 0.7) {
            rehash(capacity << 1);
        }

        int idx = hash(key);
        int start = idx;
        int firstTombstone = -1;

        while (true) {
            int k = hashKeys[idx];

            if (k == EMPTY) {
                if (firstTombstone != -1) idx = firstTombstone;

                hashKeys[idx] = key;
                hashValues[idx] = size;

                usedSlots++;

                // append（保证顺序）
                if (size == items.length) {
                    items = Arrays.copyOf(items, items.length << 1);
                    itemKeys = Arrays.copyOf(itemKeys, itemKeys.length << 1);
                }

                items[size] = value;
                itemKeys[size] = key;
                size++;
                return;
            }

            if (k == key) {
                items[hashValues[idx]] = value;
                return;
            }

            if (k == TOMBSTONE && firstTombstone == -1) {
                firstTombstone = idx;
            }

            idx = (idx + 1) & mask;

            //  死循环保护
            if (idx == start) {
                rehash(capacity << 1);
                put(key, value);
                return;
            }
        }
    }

    // ===== get =====
    public Shape get(int key) {
        int idx = hash(key);

        while (true) {
            int k = hashKeys[idx];

            if (k == EMPTY) return null;
            if (k == key) return items[hashValues[idx]];

            idx = (idx + 1) & mask;
        }
    }

    // ===== remove =====
    public Shape remove(int key) {
        int idx = hash(key);

        while (true) {
            int k = hashKeys[idx];

            if (k == EMPTY) return null;

            if (k == key) {
                int itemIdx = hashValues[idx];
                Shape removed = items[itemIdx];

                // 顺序数组打洞（不破坏顺序）
                items[itemIdx] = null;
                deadCount++;

                // tombstone
                hashKeys[idx] = TOMBSTONE;

                return removed;
            }

            idx = (idx + 1) & mask;
        }
    }

    // ===== 紧凑（帧外调用！）=====
    public void compact() {
        if (deadCount == 0) return;

        int write = 0;

        for (int read = 0; read < size; read++) {
            Shape s = items[read];
            if (s != null) {
                if (write != read) {
                    items[write] = s;
                    int key = itemKeys[read];
                    itemKeys[write] = key;

                    // 更新 hashValues
                    int idx = hash(key);
                    while (hashKeys[idx] != key) {
                        idx = (idx + 1) & mask;
                    }
                    hashValues[idx] = write;
                }
                write++;
            }
        }

        // 清尾
        for (int i = write; i < size; i++) {
            items[i] = null;
        }

        size = write;
        deadCount = 0;

        // 🔴 强制重建 hash（关键）
        rebuildHash();
    }

    // ===== 重建 hash（清除 tombstone）=====
    private void rebuildHash() {
        Arrays.fill(hashKeys, EMPTY);
        usedSlots = 0;

        for (int i = 0; i < size; i++) {
            int key = itemKeys[i];
            int idx = hash(key);

            while (hashKeys[idx] != EMPTY) {
                idx = (idx + 1) & mask;
            }

            hashKeys[idx] = key;
            hashValues[idx] = i;
            usedSlots++;
        }
    }

    // ===== 扩容 =====
    private void rehash(int newCapacity) {
        int[] oldKeys = hashKeys;
        int[] oldValues = hashValues;

        capacity = newCapacity;
        mask = capacity - 1;

        hashKeys = new int[capacity];
        Arrays.fill(hashKeys, EMPTY);
        hashValues = new int[capacity];

        usedSlots = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            int key = oldKeys[i];
            if (key >= 0) {
                int idx = hash(key);
                while (hashKeys[idx] != EMPTY) {
                    idx = (idx + 1) & mask;
                }
                hashKeys[idx] = key;
                hashValues[idx] = oldValues[i];
                usedSlots++;
            }
        }
    }
}