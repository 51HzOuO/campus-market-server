package com.example.campusmarketserver.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 头像 URL 工具类
 */
@Component
public class AvatarUtil {

    private static final String FALLBACK_URL = "https://springboot-4xrc-316010-10-1490372189.sh.run.tcloudbase.com";
    private static volatile String baseUrl = FALLBACK_URL;

    public AvatarUtil(@Value("${app.public-url:" + FALLBACK_URL + "}") String configuredUrl) {
        baseUrl = normalize(configuredUrl);
    }

    /**
     * 将相对路径转为完整 URL
     * 例如 /avatars/xxx.jpeg → https://xxx/avatars/xxx.jpeg
     */
    public static String fullUrl(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return "";
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            return avatar;
        }
        return baseUrl + (avatar.startsWith("/") ? "" : "/") + avatar;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return FALLBACK_URL;
        return value.trim().replaceAll("/+$", "");
    }
}
