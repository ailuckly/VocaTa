package com.vocata.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${vocata.static.character-avatar-path:file:/app/data/img/}")
    private String characterAvatarPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/avatar/**")
                .addResourceLocations(ensureTrailingSlash(characterAvatarPath))
                .setCachePeriod(3600);

        // 配置静态资源处理 - 避免拦截WebSocket路径
        registry.addResourceHandler("/static/**", "/images/**", "/css/**", "/js/**",
                                  "*.html", "*.css", "*.js", "*.ico", "*.png", "*.jpg", "*.gif")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 缓存1小时
    }

    private String ensureTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
