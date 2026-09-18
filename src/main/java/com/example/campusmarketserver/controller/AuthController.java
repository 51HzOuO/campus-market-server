package com.example.campusmarketserver.controller;

import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.ActivityUtil;
import com.example.campusmarketserver.util.AvatarUtil;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    // 微信小程序配置
    private static final String APP_ID = "wx32e6ea903cabff40";
    private static final String APP_SECRET = "ca0502220470bebc452ed315fb1201b7";

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 微信登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String code = params.get("code");

        // 调用微信接口换 openid
        String openid = getOpenidFromWechat(code);
        if (openid == null) {
            return Result.error(400, "微信登录失败");
        }

        // 根据 openid 查找用户，不存在就创建
        User user = userService.getByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("校园用户" + openid.substring(0, 6));
            user.setAvatar("");
            user.setActivityScore(0);
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userService.save(user);
        }

        // 生成 token 并保存
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setToken(token);
        user.setUpdateTime(LocalDateTime.now());
        userService.updateById(user);

        // 计算活跃度等级
        int score = user.getActivityScore() != null ? user.getActivityScore() : 0;
        Map<String, Object> levelInfo = ActivityUtil.getLevel(score);

        // 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("avatar", AvatarUtil.fullUrl(user.getAvatar()));
        data.put("activityScore", score);
        data.put("activityLevel", levelInfo.get("level"));
        data.put("activityColor", levelInfo.get("color"));

        return Result.success(data);
    }

    /**
     * 调用微信接口换 openid
     */
    private String getOpenidFromWechat(String code) {
        try {
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                APP_ID, APP_SECRET, code
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // 简单解析 JSON，提取 openid
            // 返回格式：{"openid":"xxx","session_key":"xxx"}
            if (responseBody.contains("\"openid\"")) {
                int start = responseBody.indexOf("\"openid\":\"") + 10;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
