package com.example.campusmarketserver.config;

import com.example.campusmarketserver.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",       // 登录接口不需要 token
                        "/error",            // 保留实际 HTTP 错误，避免被未登录错误覆盖
                        "/avatars/**",       // 头像图片不需要登录
                        "/uploads/**"        // 帖子图片不需要登录
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把 /avatars/** 映射到 /tmp/avatars/ 目录
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:/tmp/avatars/");
        // 把 /uploads/** 映射到 /tmp/uploads/ 目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/tmp/uploads/");
    }
}
