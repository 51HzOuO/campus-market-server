package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String openid;

    private String phone;

    private String nickname;

    private String avatar;

    private String studentNo;

    private String college;

    private Integer activityScore;

    private String token;

    private Integer status;

    /** 角色：0=普通用户 1=管理员 2=审核员 */
    private Integer role;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
