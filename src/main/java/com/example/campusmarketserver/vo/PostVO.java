package com.example.campusmarketserver.vo;

import com.example.campusmarketserver.entity.Post;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 帖子视图对象，包含用户活跃度信息
 */
@Data
public class PostVO {

    private Long id;
    private Long userId;
    private Integer module;
    private String title;
    private String content;
    private String images;
    private Integer likeCount;
    private Integer commentCount;
    private Integer viewCount;
    private Integer isTop;
    private LocalDateTime topExpireTime;
    private Integer status;
    /** 创建时间戳（毫秒） */
    private Long createTime;

    /** 格式化的创建时间字符串 */
    private String createTimeStr;
    private LocalDateTime updateTime;

    // 用户信息
    private String nickname;
    private String avatar;

    // 活跃度信息
    private Integer activityScore;
    private String activityLevel;
    private String activityColor;

    // 当前用户是否已点赞
    private Boolean isLiked;

    /**
     * 从 Post 实体创建 PostVO
     */
    public static PostVO fromPost(Post post) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setModule(post.getModule());
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setImages(post.getImages());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setViewCount(post.getViewCount());
        vo.setIsTop(post.getIsTop());
        vo.setTopExpireTime(post.getTopExpireTime());
        vo.setStatus(post.getStatus());
        // 转为时间戳（毫秒），确保不为 null
        LocalDateTime ct = post.getCreateTime();
        if (ct == null) {
            ct = LocalDateTime.now();
        }
        vo.setCreateTime(ct.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        vo.setCreateTimeStr(ct.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        vo.setUpdateTime(post.getUpdateTime());
        return vo;
    }
}
