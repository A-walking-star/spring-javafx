package com.cc3.springjavafx.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.cc3.springjavafx.util.StringUtils;
import javafx.stage.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /**
     * app名称（主窗口标题）
     *
     * <p>兼容两种写法：推荐 {@code app.name}，同时兼容历史配置 {@code app.appName}。</p>
     */
    @Value("${app.name:${app.appName:}}")
    private String appName;

    /**
     * logo路径
     */
    @Value("${app.icon:#{null}}")
    private String windowIcon;

    /**
     * 默认宽度（配置缺失时使用屏幕宽度）
     */
    @Value("${app.defaultWidth:#{null}}")
    private Double defaultWidth;

    /**
     * 默认高度（配置缺失时使用屏幕高度-30）
     */
    @Value("${app.defaultHeight:#{null}}")
    private Double defaultHeight;

    /**
     * 默认最小宽度
     */
    @Value("${app.defaultMinWidth:#{null}}")
    private Integer defaultMinWidth;

    /**
     * 默认最小高度
     */
    @Value("${app.defaultMinHeight:#{null}}")
    private Integer defaultMinHeight;

    /**
     * 首页html文件名
     */
    @Value("${app.resourceUrl:web/index.html}")
    private String resourceUrl;

    /**
     * 退出码
     */
    @Value("${app.exitCode:0}")
    private Integer exitCode;

    /**
     * 属性注入完成后，对未配置的项使用动态默认值
     */
    @PostConstruct
    public void applyDynamicDefaults() {
        if (defaultWidth == null) {
            defaultWidth = Screen.getPrimary().getBounds().getWidth() / 2;
            if (defaultMinWidth != null && defaultMinWidth > defaultWidth) {
                defaultWidth = Double.valueOf(defaultMinWidth);
            }
        }
        if (defaultHeight == null) {
            defaultHeight = (Screen.getPrimary().getBounds().getHeight() - 30) / 2;
            if (defaultMinHeight != null && defaultMinHeight > defaultHeight) {
                defaultHeight = Double.valueOf(defaultMinHeight);
            }
        }
    }

    /**
     * debug
     */
    private Boolean debug = false;

    public boolean hasWindowIcon() {
        return StringUtils.isNotBlank(windowIcon);
    }

    // ----------------------- GET/SET -----------------------

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getWindowIcon() {
        return windowIcon;
    }

    public void setWindowIcon(String windowIcon) {
        this.windowIcon = windowIcon;
    }

    public Double getDefaultWidth() {
        return defaultWidth;
    }

    public void setDefaultWidth(Double defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public Double getDefaultHeight() {
        return defaultHeight;
    }

    public void setDefaultHeight(Double defaultHeight) {
        this.defaultHeight = defaultHeight;
    }

    public Integer getDefaultMinWidth() {
        return defaultMinWidth;
    }

    public void setDefaultMinWidth(Integer defaultMinWidth) {
        this.defaultMinWidth = defaultMinWidth;
    }

    public Integer getDefaultMinHeight() {
        return defaultMinHeight;
    }

    public void setDefaultMinHeight(Integer defaultMinHeight) {
        this.defaultMinHeight = defaultMinHeight;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    // ----------------------- Bean定义 -----------------------

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return mapper;
    }
}