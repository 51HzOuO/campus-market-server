package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 通知接收人（帖子作者） */
    private Long userId;

    /** 触发通知的人（点赞者/评论者） */
    private Long fromUserId;

    /** 关联帖子 ID */
    private Long postId;

    /** 通知类型：like / comment */
    private String type;

    /** 通知内容文本 */
    private String content;

    /** 是否已读：0 未读，1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}
