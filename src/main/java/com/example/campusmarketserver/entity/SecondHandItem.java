package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二手商品。
 *
 * <p>status 表示商品生命周期：0=已下架，1=在售，2=已售出；
 * auditStatus 表示审核状态：0=待审核，1=审核通过，2=审核驳回。</p>
 */
@Data
@TableName("second_hand_item")
public class SecondHandItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发布人 */
    private Long userId;

    private String title;

    private String description;

    /** 图片 URL 数组，JSON 格式存储 */
    private String images;

    private BigDecimal price;

    /** 书籍、数码、生活用品等 */
    private String category;

    /** 校内交易地点 */
    private String location;

    /** 发布人选择公开的联系方式 */
    private String contact;

    /** 0=已下架，1=在售，2=已售出 */
    private Integer status;

    /** 0=待审核，1=通过，2=驳回 */
    private Integer auditStatus;

    private String auditRemark;

    /** 成交后买家 ID */
    private Long buyerId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
