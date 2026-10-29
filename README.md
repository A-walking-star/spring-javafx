# spring-javafx

[![CI](https://github.com/A-walking-star/spring-javafx/actions/workflows/ci.yml/badge.svg)](https://github.com/A-walking-star/spring-javafx/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.a-walking-star/spring-javafx?label=maven%20central)](https://central.sonatype.com/artifact/io.github.a-walking-star/spring-javafx)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

用前端页面写界面的 JavaFX 桌面应用框架：JavaFX 负责窗口与 WebView，Spring 容器负责 Bean 管理与依赖注入，
`JavaApiBridge` 把 Java 能力按窗口暴露给 JavaScript。

对使用方而言，它是一个**独立的框架**，不依赖 Spring Boot、不需要任何 starter 或自动装配机制：
**一条依赖 + 继承一个类** 即可跑起来。

---

## 1. 引入依赖

```xml
<dependency>
    <groupId>io.github.a-walking-star</groupId>
    <artifactId>spring-javafx</artifactId>
    <version>1.0.0</version>
</dependency>
```

框架会传递带入：`spring-context`、`jackson-databind`、`slf4j-api`、`snakeyaml`（解析 yml 配置必需）、
`jakarta.annotation-api`（JDK 11+ 的 JSR-250，缺了 `@PostConstruct` 会被静默忽略）。
如需替换版本，在你的 `dependencyManagement` 中覆盖即可。

日志实现由使用方自己选择（框架只依赖 `slf4j-api`），例如：

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.12</version>
</dependency>
```

### JDK 11+ 还要引入 openjfx（JDK 8 跳过）

JavaFX 11 起不再随 JDK 发布：

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-web</artifactId>
    <version>17.0.9</version>
</dependency>
```

- `javafx-web` 会传递带入 `javafx-controls` / `javafx-graphics` / `javafx-base` / `javafx-media`。
- **平台相关的类与原生库在 `-win` / `-linux` / `-mac` 分类包里**：openjfx 的父 POM 会按构建机 OS
  自动选择 `javafx.platform`，正常单机开发无需手写 classifier；**交叉打包**（在 Windows 上打 Linux 包）时加
  `-Djavafx.platform=linux`。
- 只有不带 classifier 的 `javafx-web` 是**空壳 jar（约 300 字节）**，别只放它到 `lib/` 里就以为装好了。
- JDK 17+ 建议用 17.0.x 及以上（`11.0.2` 能跑，但不在官方支持矩阵内）。

### 从 GitHub Packages 引入

GitHub Packages 即使公开包也要求认证，需要两处配置。

`~/.m2/settings.xml`（用环境变量，避免把 token 写进文件；token 只需要 `read:packages`）：

```xml
<servers>
    <server>
        <id>github</id>
        <username>${env.GITHUB_ACTOR}</username>
        <password>${env.GITHUB_TOKEN}</password>
    </server>
</servers>
```

运行前设置两个环境变量：`GITHUB_ACTOR` 填 GitHub 账号，`GITHUB_TOKEN` 填勾了 `read:packages` 的 PAT。

`pom.xml`：

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/A-walking-star/spring-javafx</url>
    </repository>
</repositories>
```

## 2. 最小使用

**第一步：继承 `JavaFxApplication`**

```java
package com.example.app;

import com.cc3.springjavafx.JavaFxApplication;

public class MyApp extends JavaFxApplication {
    public static void main(String[] args) {
        launch(args);
    }
}
```

**第二步：在 `src/main/resources/app.yml` 写配置**

```yaml
app:
  name: 我的应用
  resourceUrl: web/index.html
```

**第三步：把你的页面放进 `src/main/resources/web/index.html`**

完成。执行 `main` 即可看到窗口。

> 需要自己的 Bean 时，再覆写 `configs()` 返回一个带 `@ComponentScan` 的配置类即可（见下）。

## 3. 配置

配置文件放在 `classpath` 根目录，`app.yml` 与 `app.properties` 都会被自动加载。

**优先级（高 → 低）**：JVM 参数 / 环境变量 → `app.yml` → `app.properties` → 框架内置 `spring-javafx/defaults.yml`

框架默认值随 jar 分发（`spring-javafx/defaults.yml`），**不会**占用 `app.yml` 这个名字，因此不会与使用方配置互相覆盖。

| 键 | 说明 | 默认值 |
|---|---|---|
| `app.name` | 主窗口标题（兼容旧写法 `app.appName`） | `spring-javafx` |
| `app.resourceUrl` | 首页地址，支持 http(s)、`file:`、classpath 资源 | `web/index.html` |
| `app.icon` | 窗口图标文件路径，留空用内置图标 | 空 |
| `app.defaultWidth` / `app.defaultHeight` | 窗口初始尺寸 | 主屏幕的一半 |
| `app.defaultMinWidth` / `app.defaultMinHeight` | 最小尺寸 | 无 |
| `app.exitCode` | 主窗口关闭时的进程退出码 | `0` |
| `app.dialog.enabled` | 关闭后 alert/confirm/prompt 只记日志 | `true` |
| `app.dialog.handler` | 自定义对话框处理器（bean 名或全类名） | 空（自动选择） |
| `app.dialog.title` / `confirmText` / `cancelText` | 弹窗文案 | `提示` / `确定` / `取消` |
| `app.dialog.stylesheet` | 追加样式表 | 空 |
| `app.dialog.animate` | 入场动画 | `true` |
| `app.dialog.jsErrorDialog` | JS 报错是否弹窗 | `false` |

## 4. 三个扩展点

### ① 主窗口控制器（`mainControllerClass()`）

```java
public class MyMainController extends DefaultMainController {
    public MyMainController(Stage stage) { super(stage); }

    @Override
    public void loadConfig() {
        // 页面加载完成后执行
        getStage().setMaximized(true);
    }
}
```

```java
@Override
protected Class<? extends BaseController> mainControllerClass() {
    return MyMainController.class;
}
```

自定义控制器必须保留 `(Stage)` 构造参数；其他依赖用 `@Autowired` 字段/方法注入。

### ② 子窗口控制器（`StageControllerFactory`）

`ContextUtils.createScene(...)` 通过该工厂创建子窗口。实现它并注册为 Bean 即可接管所有子窗口：

```java
@Component
public class MyStageControllerFactory implements StageControllerFactory {
    @Override
    public GenericBeanDefinition create(String key, Stage stage, BaseController parent,
                                        String url, double width, double height) {
        GenericBeanDefinition def = new GenericBeanDefinition();
        def.setBeanClass(MyChildController.class);
        // ... 填构造参数与属性
        return def;
    }
}
```

返回 `BeanDefinition` 而不是实例，是为了让子窗口控制器仍由 Spring 管理生命周期
（`@PostConstruct` 打开窗口、销毁时关闭）。

### ③ 启动入口（`JavaFxApplication`）

```java
public class MyApp extends JavaFxApplication {
    public static void main(String[] args) { launch(args); }

    @Override
    protected Class<?>[] configs() {
        return new Class<?>[]{AppConfig.class};   // 你自己的 @Configuration
    }

    @Override
    protected void onContextReady(ConfigurableApplicationContext ctx) {
        // 容器就绪、主窗口已创建
    }
}
```

## 5. 页面侧能力

页面加载完成后，框架会往 `window` 注入两个命名空间（可自由扩展）：

- `window.WindowApi` —— 窗口标题/尺寸/父子关系/关闭等
- `window.LoggerApi` —— 前端日志转发到后端

约定：`$controllerKey`（子窗口才有）、`$parentControllerKey`、`$$_apiReady`、`$$_onChildReady`、`$$_onChildClosed`。

**JS 侧取值约定（重要）**：Java `String` 返回值到 JS 是原生字符串，但 **`String[]` 数组的元素是 Java 包装对象**。
实测 `getChildKeys()` 返回的是真正的 JS 数组（`Array.isArray` 为 true、`join` 正常），
但 `keys[0] === 'xxx'` 恒为 `false`、`JSON.stringify(keys)` 得到 `[{}]`。
前端取值请统一转换：

```js
var raw = WindowApi.getChildKeys();
var keys = [];
for (var i = 0; i < raw.length; i++) {
    keys.push(String(raw[i]));   // 或 raw.join(',').split(',')
}
```

**窗口父子关系约定**：`createScene` 的 `parentControllerKey` **只有 `null` 表示主窗口**；
传空串 `""` 或未知 key 时，窗口仍会打开但不会挂到任何父窗口，因此**不会出现在 `getChildKeys()` 里**
（框架会打 WARN 提示）。

### 5.1 WindowApi 速查

`window.WindowApi` 的完整可运行示例见
[`demo/src/main/resources/web/windowapi-demo.html`](demo/src/main/resources/web/windowapi-demo.html)：
每个方法都有签名、说明、可复制的代码片段和"点一下就能运行"的按钮。

**方法分组**

| 分组 | 代表方法 | 说明 |
|---|---|---|
| 标识与层级 | `getKey()` `getParentKey()` `getChildKeys()` `getAllKeys()` `getWindowCount()` `getStatus()` | 主窗口 key 为 `null`；`getAllKeys()` 不含主窗口 |
| 标题与尺寸 | `getTitle/setTitle` `getWidth/setWidth` `getHeight/setHeight` `setSize` `getMinWidth/setMinWidth` `setMinSize` `getResizable/setResizable` | 写方法会校验参数，非法值只记日志 |
| 位置与外观 | `getX/getY` `setX/setY` `setPosition` `centerOnScreen` `getOpacity/setOpacity` | 坐标支持负值（多显示器） |
| 状态控制 | `minimizeWindow` `restoreWindow` `showWindow` `toFront` `setMaximized/isMaximized` `toggleMaximized` `setFullScreen/isFullScreen` `toggleFullScreen` `setAlwaysOnTop/isAlwaysOnTop` | 均作用于当前窗口 |
| 关闭 | `closeWindow` `requestCloseWindow` `closeChildWindows` | `requestCloseWindow` 会先触发 `setOnCloseRequest`，可被 `consume()` 取消 |
| 创建窗口 | `createChildScene` `createChildWindowScene` `createChildApplicationScene` `createScene` | 前三个自动以当前窗口为父窗口 |
| 跨窗口 | 所有 `xxxByControllerKey(controllerKey, ...)` | 第一个参数为目标窗口 key，`null` 表示主窗口 |
| 回调 | `onCallByControllerKey` `onCallAllStage` `executeJsByControllerKey` | 触发前端 JS 函数 / 执行 JS |

**最小示例**

```js
window['$$_apiReady'] = function () {
    // 当前窗口（主窗口 key 为 null）
    var key = WindowApi.getKey();

    // 打开子窗口：父窗口自动 = 当前窗口；宽高传 null/0 使用 app.defaultWidth/Height
    var childKey = WindowApi.createChildScene('web/child.html', 900, 680);

    // 跨窗口操作：null = 主窗口
    WindowApi.setTitleByControllerKey(childKey, '子窗口');
    WindowApi.centerOnScreenByControllerKey(null);

    // 给子窗口发回调（子窗口里定义 window.onParentMessage）
    WindowApi.onCallByControllerKey(childKey, 'onParentMessage');

    // 关闭确认：窗口自己的 setOnCloseRequest 里 consume() 即可取消
    // WindowApi.requestCloseWindow();
};
```

**关键语义**

- 无 key 的方法只作用于**当前窗口**；每个窗口持有独立的 `WindowApi` 实例。
- `xxxByControllerKey` 的 `controllerKey` **只有 `null` 表示主窗口**；`""`/未知 key 视为找不到窗口。
- 读方法在窗口不可用时返回安全默认值（`0` / `false` / `null`），不会 NPE；当前窗口版本的 `getWidth()` 等返回原始类型。
- 写方法在 JavaFX 线程同步生效；从后台线程调用会自动投递到 JavaFX 线程（异步生效）。
- 创建/选择窗口的同步方法必须在 JavaFX 线程调用；前端调用天然满足。
- `getChildKeys()` / `getAllKeys()` 的数组元素是 Java String 包装对象，记得 `String(keys[i])`。

**安全提示（重要）**：`window.WindowApi` 是**完整的 Java 对象**，JS 能调用它的所有 public 方法，
包括基类的 `getController()` / `getStage()` / `destroy()`、`executeJsByControllerKey()`，
以及 `openDir` / `deleteFiles` 这类文件操作。

- 只给**可信页面**（随应用打包的本地 HTML）注入 JavaApiBridge；
- 不要用注入了桥接器的 WebView 去加载**远程页面或用户提供的 HTML**（邮件正文、Markdown/HTML 预览等），
  否则页面脚本可以操作窗口、读写或删除文件；
- 确需展示不可信内容时，另开一个不注入桥接器的 WebView。

**注意**：JavaFX 8 的 WebKit 538 不支持 CSS 变量、CSS Grid、flex `gap`、`::placeholder`，JS 仅支持 ES5。

## 6. JDK 版本

框架以 **JDK 8** 为编译基线，产物可在 JDK 8 / 11 / 17 运行。

- **JDK 8**：JavaFX 内置，无需额外依赖。
- **JDK 11+**：JavaFX 已从 JDK 移除，需要自行引入 openjfx。框架对 openjfx 只做**编译期**
  依赖（`provided` + `optional`，不传递给使用方，也不绑定平台），运行时由使用方自己选版本与平台：

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-web</artifactId>
    <version>17.0.9</version>
</dependency>
```

若需要把前端 `console.log` 转发到后端日志，JDK 11+ 需加启动参数（框架已做反射降级，不加只是少这项日志，不影响运行）：

```
--add-exports javafx.web/com.sun.javafx.webkit=ALL-UNNAMED
```

> **JDK 11+ 必读：`javax.annotation`（JSR-250）**
>
> Spring 只有在运行时 classpath 上能找到 `javax.annotation.Resource` 时，才会注册
> `CommonAnnotationBeanPostProcessor` —— 否则 `@PostConstruct` / `@PreDestroy` 会被**静默跳过**
> （不打日志、不报错）。而框架的窗口正是靠 `@PostConstruct` 打开的，于是表现为：
> **日志停在启动中途、之后一直等待、窗口永远不出现，却没有任何异常**。
>
> JDK 8 自带 `javax.annotation`，所以 IDEA 里用 JDK 8 跑/调试完全正常，换成 JDK 17 的
> `java -jar` 才复现 —— 这是最容易误判成"死锁 / JavaFX 卡住"的原因。
>
> 框架 `core` 已传递依赖 `jakarta.annotation:jakarta.annotation-api:1.3.5`（**必须 1.3.x**：
> 它提供的才是 `javax.annotation` 包；2.x 已改名 `jakarta.annotation`，Spring 5 不识别）。
> 启动时框架还会做前置校验，缺失就直接失败并打印依赖坐标，不会再静默卡住。

## 7. 模块

| 模块 | 说明 |
|---|---|
| `core/` | 框架本体，使用方依赖的就是它 |
| `demo/` | 示例应用：一个完整可运行的最小工程 |
| `cli/` | 分发用的启动脚本样例（自带 JDK + jar） |

> Maven `groupId` 是 `io.github.a-walking-star`，Java 包名仍是 `com.cc3.springjavafx`（两者可以不同；
> Java 包名不允许带连字符，所以没有跟着改）。升级到本坐标时只需改 `groupId`，`import` 不用动。


## 8. 常见问题

**控制台日志中文乱码（cmd 里显示 `閰嶇疆瑁呴厤瀹屾垚…`）**

Windows 控制台默认代码页是 936(GBK)，而日志按 UTF-8 写字节，于是被按 GBK 解码成乱码。
正确做法是让**控制台 appender 的编码跟随控制台代码页**（文件 appender 仍保持 UTF-8）：

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <charset>${sun.stdout.encoding:-UTF-8}</charset>
        <pattern>${LOG_PATTERN}</pattern>
    </encoder>
</appender>
```

- cmd（默认 936）：JDK 会把 `sun.stdout.encoding` 置为 `ms936` → 按 GBK 输出 → 正常；
- 执行过 `chcp 65001` 的窗口：`sun.stdout.encoding=UTF-8` → 正常；
- 重定向到文件 / IDEA 控制台：该属性不存在，回落 UTF-8 → 正常。

不要写死 `<charset>GBK</charset>`（UTF-8 控制台会反过来乱码），也不要写死 UTF-8（就是本问题）。
`demo/src/main/resources/logback.xml` 已按上面的写法配置，直接复制即可。

---

## 9. License

[MIT](LICENSE) © 2026 A-walking-star

使用、修改、商用、再分发都可以，保留版权声明与许可声明即可。
