package com.example.campusmarketserver.controller;

import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.service.WechatLoginClient;
import com.example.campusmarketserver.util.ActivityUtil;
import com.example.campusmarketserver.util.AvatarUtil;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    private final WechatLoginClient wechatLoginClient;

    public AuthController(UserService userService, WechatLoginClient wechatLoginClient) {
        this.userService = userService;
        this.wechatLoginClient = wechatLoginClient;
    }

    /**
     * 微信登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String code = params == null ? null : params.get("code");
        if (code == null || code.isBlank()) {
            return Result.error(400, "缺少微信登录凭证，请重新点击登录");
        }

        // 调用微信接口换 openid
        final String openid;
        try {
            openid = wechatLoginClient.getOpenid(code);
        } catch (WechatLoginClient.LoginException ex) {
            return Result.error(ex.getCode(), ex.getMessage());
        }

        // 根据 openid 查找用户，不存在就创建
        User user = userService.getByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("校园用户" + openid.substring(0, Math.min(6, openid.length())));
            user.setAvatar("");
            user.setActivityScore(0);
            user.setStatus(0);
            user.setRole(0);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            if (!userService.save(user)) {
                return Result.error(503, "用户注册失败，请稍后重试");
            }
        }
        if (Integer.valueOf(1).equals(user.getStatus())) {
            return Result.error(403, "账号已被封禁，请联系管理员");
        }

        // 生成 token 并保存
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setToken(token);
        user.setUpdateTime(LocalDateTime.now());
        if (!userService.updateById(user)) {
            return Result.error(503, "登录状态保存失败，请稍后重试");
        }

        // 计算活跃度等级
        int score = user.getActivityScore() != null ? user.getActivityScore() : 0;
        Map<String, Object> levelInfo = ActivityUtil.getLevel(score);

        // 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("avatar", AvatarUtil.fullUrl(user.getAvatar()));
        data.put("role", user.getRole() != null ? user.getRole() : 0);
        data.put("status", user.getStatus() != null ? user.getStatus() : 0);
        data.put("activityScore", score);
        data.put("activityLevel", levelInfo.get("level"));
        data.put("activityColor", levelInfo.get("color"));

        return Result.success(data);
    }

}
