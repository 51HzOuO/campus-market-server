package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学校社团。
 * status: 0=待审核，1=已通过，2=已驳回，3=已下架。
 */
@Data
@TableName("club")
public class Club {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long creatorId;

    private String name;

    private String description;

    private String logo;

    private String contact;

    private Integer memberCount;

    private Integer status;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
