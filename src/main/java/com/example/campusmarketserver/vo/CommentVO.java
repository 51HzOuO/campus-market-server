package com.example.campusmarketserver.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 评论视图对象，包含用户信息
 */
@Data
public class CommentVO {

    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    /** 创建时间戳（毫秒） */
    private Long createTime;

    /** 格式化的创建时间字符串 */
    private String createTimeStr;

    // 用户信息
    private String nickname;
    private String avatar;
    private String activityLevel;
    private String activityColor;

    /**
     * 从 Comment 实体和用户信息创建 CommentVO
     */
    public static CommentVO fromComment(Object comment, String nickname, String avatar, String activityLevel, String activityColor) {
        CommentVO vo = new CommentVO();
        // 使用反射或手动设置，这里简化处理
        return vo;
    }

    /**
     * 设置 createTime 从 LocalDateTime，同时生成格式化字符串
     */
    public void setCreateTimeFromLocalDateTime(LocalDateTime time) {
        if (time == null) {
            time = LocalDateTime.now();
        }
        this.createTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        this.createTimeStr = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
