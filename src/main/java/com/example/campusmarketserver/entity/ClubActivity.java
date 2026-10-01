package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 社团活动。status: 0=待审核，1=已通过，2=已驳回，3=已下架。 */
@Data
@TableName("club_activity")
public class ClubActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clubId;

    private Long creatorId;

    private String title;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private Integer maxParticipants;

    private Integer participantCount;

    private Integer status;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
