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
                        "/post/list",        // 帖子列表不需要登录
                        "/post/detail/**",   // 帖子详情不需要登录
                        "/comment/list",     // 评论列表不需要登录
                        "/errand/list",       // 跑腿列表不需要登录
                        "/errand/detail/**",  // 跑腿详情不需要登录
                        "/second-hand/list", // 二手列表不需要登录
                        "/second-hand/detail/**", // 二手详情不需要登录
                        "/club/list",        // 社团列表不需要登录
                        "/club/detail/**",   // 社团详情不需要登录
                        "/club/activity/list", // 活动列表不需要登录
                        "/club/activity/detail/**", // 活动详情不需要登录
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
