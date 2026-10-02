package com.example.campusmarketserver.config;

import com.example.campusmarketserver.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final String avatarPath;
    private final String uploadPath;

    public WebConfig(AuthInterceptor authInterceptor,
                     @Value("${upload.avatar-path:/tmp/avatars}") String avatarPath,
                     @Value("${upload.path:/tmp/uploads}") String uploadPath) {
        this.authInterceptor = authInterceptor;
        this.avatarPath = avatarPath;
        this.uploadPath = uploadPath;
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
        // 把 /avatars/** 映射到可配置的头像目录
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + normalized(avatarPath));
        // 把 /uploads/** 映射到可配置的图片目录
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + normalized(uploadPath));
    }

    private static String normalized(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
