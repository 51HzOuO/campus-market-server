package com.example.campusmarketserver.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 活跃度等级工具类
 */
public class ActivityUtil {

    /**
     * 根据活跃度分数获取等级信息
     *
     * @param score 活跃度分数
     * @return 包含 level(等级名)、color(颜色)、minScore(最低分) 的 Map
     */
    public static Map<String, Object> getLevel(int score) {
        Map<String, Object> result = new HashMap<>();

        if (score >= 500) {
            result.put("level", "集市大佬");
            result.put("color", "#FF4D4F"); // 红色
            result.put("minScore", 500);
        } else if (score >= 200) {
            result.put("level", "校园红人");
            result.put("color", "#FF8C00"); // 橙色
            result.put("minScore", 200);
        } else if (score >= 50) {
            result.put("level", "活跃成员");
            result.put("color", "#2DBE9A"); // 绿色
            result.put("minScore", 50);
        } else {
            result.put("level", "新手上路");
            result.put("color", "#999999"); // 灰色
            result.put("minScore", 0);
        }

        result.put("score", score);
        return result;
    }

    /**
     * 获取等级名称
     */
    public static String getLevelName(int score) {
        return (String) getLevel(score).get("level");
    }

    /**
     * 获取等级颜色
     */
    public static String getLevelColor(int score) {
        return (String) getLevel(score).get("color");
    }
}
