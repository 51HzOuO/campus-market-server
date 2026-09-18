package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer module;

    private String title;

    private String content;

    /**
     * 图片URL数组，JSON 格式存储
     */
    private String images;

    private Integer likeCount;

    private Integer commentCount;

    private Integer viewCount;

    private Integer isTop;

    private LocalDateTime topExpireTime;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
