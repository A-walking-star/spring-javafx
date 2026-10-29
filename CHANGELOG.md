# Changelog

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

### 新增

- `WindowApi` 补齐窗口控制能力：
  - 状态：`isShowing` / `isIconified` / `isMaximized` / `isFullScreen` / `isAlwaysOnTop`、
    `minimizeWindow` / `restoreWindow` / `showWindow` / `toFront`、
    `setMaximized` / `toggleMaximized` / `setFullScreen` / `toggleFullScreen` / `setAlwaysOnTop`
  - 位置与外观：`getX` / `getY` / `setX` / `setY` / `setPosition` / `centerOnScreen` / `getOpacity` / `setOpacity`
  - 批量尺寸：`setSize` / `setMinSize`
  - 窗口清单：`getAllKeys` / `getWindowCount` / `getStatus` / `isOpened` / `hasWindow`
- 便捷开窗：`createChildScene` / `createChildWindowScene` / `createChildApplicationScene`
  （自动以当前窗口为父窗口，无需手拼 parentKey）
- 跨窗口操作补全：`isShowingByControllerKey`、`isIconifiedByControllerKey`、
  `isMaximizedByControllerKey`、`isFullScreenByControllerKey`、`isAlwaysOnTopByControllerKey`、
  `getXByControllerKey` / `getYByControllerKey` / `getOpacityByControllerKey`、
  `setSizeByControllerKey` / `setMinSizeByControllerKey` / `setPositionByControllerKey` /
  `centerOnScreenByControllerKey` / `setOpacityByControllerKey`、
  `setMaximizedByControllerKey` / `setFullScreenByControllerKey` / `setAlwaysOnTopByControllerKey` /
  `toFrontByControllerKey`、`restoreWindowByControllerKey` / `showWindowByControllerKey` 等
- 关闭语义：新增 `requestCloseWindow()` / `requestCloseWindowByControllerKey()`，
  先触发 `WINDOW_CLOSE_REQUEST`（可被 `setOnCloseRequest` 的 `consume()` 取消），未拦截时兜底关闭；
  `closeWindow()` 保持强制关闭
- `closeChildWindows()`：关闭当前窗口全部子窗口并返回数量
- `executeJsByControllerKey(key, script)`：在指定窗口执行任意 JS
- 详细示例页 `demo/src/main/resources/web/windowapi-demo.html`：
  分组讲解、每个 API 的可运行按钮、可复制代码、完整 API 表、Java 侧用法与常见问题

### 变更

- **Maven 坐标变更**：`com.cc3:spring-javafx` → `io.github.a-walking-star:spring-javafx`
  （Java 包名仍是 `com.cc3.springjavafx`，`import` 不需要改）。旧坐标已废弃
- `setTitleByControllerKey` / `setWidthByControllerKey` / `setHeightByControllerKey` /
  `setMinWidthByControllerKey` / `setMinHeightByControllerKey` 的返回类型由 `void` 改为 `boolean`
  （返回目标窗口是否存在）。忽略返回值的 Java 调用源码兼容，重新编译即可
- `getChildKeys()` / `getChildKeysByControllerKey()` 现在过滤已结束窗口并按 key 排序，
  找不到窗口返回空数组而不是 `null`

### 修复

- `WindowApi.getWidth()` / `getHeight()` / `getMinWidth()` / `getMinHeight()` 在窗口已关闭
  （`BaseController.getWidth()` 返回 `null`）时自动拆箱 NPE，现改为直接读 Stage 并返回安全默认值
- `WindowApi` 的所有当前窗口方法在桥接器销毁后（`controller == null`）会 NPE，现统一判空
- `getChildKeys()` / `getChildKeysByControllerKey()` 未过滤已结束窗口、顺序不稳定；
  现过滤并排序，找不到窗口返回空数组
- `setWidth` / `setHeight` 等尺寸设置未校验 `NaN` / 负数 / `Infinity`，现忽略非法值并记 WARN
- `setOpacity` 超出 `0~1` 时由 JavaFX 抛异常改为自动截断
- 从后台线程调用窗口写方法会抛 "Not on FX application thread"，现自动投递到 JavaFX 线程

## [1.0.0] - 2026-10-22

首个可发布版本。

### 特性

- `JavaFxApplication` 启动基类：容器在 JavaFX 线程创建，窗口/Bean 生命周期由 Spring 管理
- 三处扩展点：`mainControllerClass()`、`StageControllerFactory`、`JavaApiBridge`
- 配置装配：`app.yml` / `app.properties` 覆盖框架 `spring-javafx/defaults.yml`（`ConfigSourcesPostProcessor`）
- 窗口能力：多窗口、父子关系、模态窗口、按窗口隔离的 `WindowApi` / `LoggerApi`
- 框架级对话框：`WebDialogManager` + 可替换的 `WebDialogHandler`（TDesign 风格内置实现）
- 前端 `console.log` 转发（JDK 11+ 需 `--add-exports`，缺失自动降级）

### 修复

- **JDK 11+ 启动后一直等待、窗口不出现**：缺少 JSR-250（`javax.annotation`）导致 Spring 静默跳过
  `@PostConstruct`。`core` 现传递依赖 `jakarta.annotation-api:1.3.x`，并在启动时前置校验、缺失直接报错
- **控制台中文乱码**：控制台 appender 编码改为跟随控制台代码页（`${sun.stdout.encoding:-UTF-8}`）
- `BaseController.close()` 在自定义控制器无 Stage 时 NPE
- 全局未捕获异常处理器被每个窗口重复覆盖（泄漏 + 日志归属错误）
- `loadBaseJavaApi` 注入失败时前端永远收不到 `$$_apiReady`；命名空间未校验就拼进 JS
- `ContextUtils.createScene`：JS 传 null 宽高拆箱 NPE；创建失败遗留 Stage/Bean；`hasControllerKey(null)` 语义矛盾
- `NewStageWebController` 构造期就注册到父窗口，初始化失败的窗口永远出现在 `getChildKeys()`
- `ParamsUtils.onCallBack` 把 boolean/number 参数转成字符串；未转义 U+2028 等字符
- `SpringUtils` 静态持有 BeanFactory 不释放；`AppPathUtils.deleteFiles` 失败仍返回 true
