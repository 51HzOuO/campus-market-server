package com.example.campusmarketserver.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 敏感词工具类
 */
public class SensitiveWordUtil {

    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "代课", "代考", "代写", "包过", "答案"
    );

    /**
     * 检测内容是否包含敏感词
     *
     * @param content 待检测内容
     * @return 命中的敏感词列表，为空则表示通过
     */
    public static List<String> detect(String content) {
        List<String> hits = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return hits;
        }
        for (String word : SENSITIVE_WORDS) {
            if (content.contains(word)) {
                hits.add(word);
            }
        }
        return hits;
    }

    /**
     * 判断内容是否包含敏感词
     */
    public static boolean containsSensitiveWord(String content) {
        return !detect(content).isEmpty();
    }
}
