package org.example.GUI;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 行为分析的分析项基类：每个分析项占用「行为分析」页下方选择区的一个标签页。
 *
 * <p>新增分析项的步骤：
 * <ol>
 *   <li>继承本类，实现 {@link #titleKey()} 与 {@link #onInputChanged}，在构造中设置自身布局；</li>
 *   <li>在 {@link BehaviorAnalysisTab} 的构造函数中调用 {@code addItem(...)} 注册。</li>
 * </ol>
 * 共享输入通过 {@link #currentInput()} 同步读取（启动分析时取到的一定是最新内容），
 * 输入变化（防抖后）由 {@link #onInputChanged} 推送；分析进行中用 {@link #setInputBusy} 锁定共享输入区。
 * 本类只持有回调，不持有父组件引用。
 */
public abstract class BehaviorAnalysisPanel extends JPanel {

    private final Supplier<BehaviorInput> inputSupplier;
    private final Consumer<Boolean> busyListener;

    protected BehaviorAnalysisPanel(Supplier<BehaviorInput> inputSupplier, Consumer<Boolean> busyListener) {
        this.inputSupplier = inputSupplier;
        this.busyListener = busyListener;
    }

    /** 标签标题的 i18n 词条键。 */
    public abstract String titleKey();

    /** 共享输入变化（防抖后）时回调，用于刷新本分析项的校验结果与按钮状态。 */
    public abstract void onInputChanged(BehaviorInput input);

    /** 深色模式切换时由 {@link BehaviorAnalysisTab} 调用。 */
    public void updateDarkMode() {
    }

    /** 同步读取当前输入，不受输入防抖影响。 */
    protected final BehaviorInput currentInput() {
        return inputSupplier.get();
    }

    /** 分析进行中锁定共享输入区（true），结束后解锁（false）。 */
    protected final void setInputBusy(boolean busy) {
        busyListener.accept(busy);
    }
}
