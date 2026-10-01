package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 社团活动报名记录。status: 0=已取消，1=已报名。 */
@Data
@TableName("club_activity_member")
public class ClubActivityMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long userId;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
