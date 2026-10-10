package org.example.GUI;

/**
 * 行为分析页的输入快照：原始阵型（玩家阵型）与测试阵集（评判阵集）的文本。
 *
 * <p>构造时统一去除首尾空白；原始阵型为单个阵型，换行一律去掉，避免输入框中误按回车拆分阵型名称。
 */
public record BehaviorInput(String playerText, String evalText) {

    public BehaviorInput {
        playerText = playerText == null ? "" : playerText.replaceAll("[\\r\\n]+", "").trim();
        evalText = evalText == null ? "" : evalText.trim();
    }
}
