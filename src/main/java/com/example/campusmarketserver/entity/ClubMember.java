package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 社团成员。role: 0=成员，1=社团负责人；status: 0=已退出，1=正常。 */
@Data
@TableName("club_member")
public class ClubMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clubId;

    private Long userId;

    private Integer role;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
