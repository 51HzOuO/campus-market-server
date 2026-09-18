package com.example.campusmarketserver.util;

/**
 * 头像 URL 工具类
 */
public class AvatarUtil {

    private static final String BASE_URL = "http://localhost:8080";

    /**
     * 将相对路径转为完整 URL
     * 例如 /avatars/xxx.jpeg → http://localhost:8080/avatars/xxx.jpeg
     */
    public static String fullUrl(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return "";
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            return avatar;
        }
        return BASE_URL + (avatar.startsWith("/") ? "" : "/") + avatar;
    }
}
