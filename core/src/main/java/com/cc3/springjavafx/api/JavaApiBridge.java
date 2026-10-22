package com.cc3.springjavafx.api;

import com.cc3.springjavafx.controller.BaseController;
import org.springframework.beans.factory.DisposableBean;

/**
 * Java 与前端 JS 之间的桥接接口。
 *
 * <p>每个窗口（{@link BaseController}）都持有一套自己的桥接器实例：</p>
 * <ul>
 *     <li>窗口页面加载完成时，由 {@link BaseController#initBaseApi()} 创建实例并调用
 *     {@link #bindController(BaseController)} 绑定到该窗口；</li>
 *     <li>前端通过 {@code window.<命名空间>} 访问，例如 {@code window.WindowApi}；</li>
 *     <li>窗口关闭（或页面重新加载）时，由 {@link BaseController#destroyApiBridges()} 统一销毁。</li>
 * </ul>
 *
 * <p><b>实现类必须声明为 prototype 作用域</b>，否则各窗口会共享同一个实例，
 * 绑定关系会被后加载的窗口覆盖，无法做到窗口级隔离：</p>
 *
 * <pre><code>
 * &#64;Component
 * &#64;Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
 * public class DemoApi extends AbstractJavaApiBridge {
 *     // 通过 getController() / getStage() 访问所属窗口
 * }
 * </code></pre>
 *
 * @author A-walking-star
 */
public interface JavaApiBridge extends DisposableBean {

    /**
     * 注入到 JS 上下文中的命名空间，默认取实现类的简单类名
     * @return 命名空间，例如 WindowApi
     */
    default String getNamespace() {
        return getClass().getSimpleName();
    }

    /**
     * 绑定所属窗口控制器，一个实例只应绑定一个窗口
     * @param controller 所属窗口控制器
     */
    void bindController(BaseController controller);

    /**
     * 销毁当前实例，释放持有的资源。
     *
     * <p>prototype 实例不会触发 Spring 的 {@code @PreDestroy} 回调，
     * 需要在窗口关闭时通过本方法显式释放；实现类应保证重复调用安全。</p>
     */
    default void destroy() {
    }
}
