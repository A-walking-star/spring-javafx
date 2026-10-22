package com.cc3.springjavafx.web.dialog;

/**
 * 对话框处理器扩展点（SPI）——框架级 WebEngine 弹窗的统一出口。
 *
 * <p>框架把 {@code WebEngine} 的原生事件（{@code window.alert / confirm / prompt}、JS 错误）
 * 全部收敛到本接口，因此只要替换实现，就能整体换掉弹窗的外观与交互：</p>
 *
 * <table summary="原生事件到处理器的映射">
 *     <tr><th>前端事件</th><th>对应方法</th></tr>
 *     <tr><td>{@code window.alert(msg)}</td><td>{@link WebDialogManager#alert}</td></tr>
 *     <tr><td>{@code window.confirm(msg)}</td><td>{@link WebDialogManager#confirm}</td></tr>
 *     <tr><td>{@code window.prompt(msg, default)}</td><td>{@link WebDialogManager#prompt}</td></tr>
 * </table>
 *
 * <h3>如何自定义</h3>
 * <ol>
 *     <li>写一个实现类并标注 {@code @Component}（建议 prototype 之外的单例即可）；
 *     只要容器里存在"非内置"的实现，框架会自动优先使用它，无需改配置；</li>
 *     <li>有多个实现时，用 {@code app.dialog.handler=beanName} 明确指定；</li>
 *     <li>只想微调内置样式时，不必实现本接口：继承 {@link DefaultWebDialogHandler} 覆写
 *     {@code getStylesheets()}，或在 {@code app.dialog.stylesheet} 里追加一份 CSS（后加载，优先级更高）。</li>
 * </ol>
 *
 * <p><b>线程约定：</b>{@link #show} 会在 JavaFX 应用线程上被调用，实现可以安全地阻塞
 * （例如 {@code Stage.showAndWait()}）；若在非 FX 线程被调用，实现类应自行切线程。</p>
 *
 * @author A-walking-star
 */
public interface WebDialogHandler {

    /**
     * 显示对话框并返回结果，方法可以阻塞直到用户操作完成。
     *
     * @param request 对话框请求（标题、按钮文案、所属窗口等已由框架补齐）
     * @return 用户操作结果，不允许返回 null；取消/关闭返回 {@link WebDialogResult#cancel()}
     */
    WebDialogResult show(WebDialogRequest request);

    /**
     * 处理器名称，用于日志与配置排查
     * @return 名称
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * 是否支持该类型的对话框，返回 false 时框架会回退到内置实现
     * @param type 对话框类型
     * @return 是否支持
     */
    default boolean supports(WebDialogType type) {
        return true;
    }
}
