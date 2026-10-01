package com.example.campusmarketserver.interceptor;

import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_GET_PATHS = List.of(
            "/post/list", "/post/detail/**", "/post/search", "/post/hot", "/comment/list",
            "/errand/list", "/errand/detail/**", "/second-hand/list", "/second-hand/detail/**",
            "/club/list", "/club/detail/**", "/club/activity/list", "/club/activity/detail/**");

    private final UserService userService;

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserContext.clear();
        // 放行 OPTIONS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            if (isPublicRead(request)) return true;
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }

        // 去掉 "Bearer " 前缀（如果有）
        token = token.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }

        // 根据 token 查找用户
        User user = userService.getByToken(token);
        if (user == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"token 无效或已过期\"}");
            return false;
        }

        if (Integer.valueOf(1).equals(user.getStatus())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"账号已被封禁，请联系管理员\"}");
            return false;
        }

        // 把用户 ID 放入上下文
        UserContext.setUserId(user.getId());
        return true;
    }

    private boolean isPublicRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        // This public endpoint also supports the authenticated personal list.
        if ("/errand/list".equals(path) && "mine".equals(request.getParameter("scope"))) return false;
        return PUBLIC_GET_PATHS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理，防止内存泄漏
        UserContext.clear();
    }
}
