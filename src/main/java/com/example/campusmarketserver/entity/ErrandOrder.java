package com.example.campusmarketserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿代办订单。
 *
 * <p>status：1 待接单，2 已接单，3 进行中，4 待确认，5 已完成，6 已取消；
 * auditStatus：0 待审核，1 审核通过，2 审核驳回；
 * paymentStatus：0 未支付，1 已支付，2 已退款。</p>
 */
@Data
@TableName("errand_order")
public class ErrandOrder {

    public static final int STATUS_WAITING = 1;
    public static final int STATUS_ACCEPTED = 2;
    public static final int STATUS_IN_PROGRESS = 3;
    public static final int STATUS_WAITING_CONFIRM = 4;
    public static final int STATUS_COMPLETED = 5;
    public static final int STATUS_CANCELLED = 6;

    public static final int AUDIT_PENDING = 0;
    public static final int AUDIT_APPROVED = 1;
    public static final int AUDIT_REJECTED = 2;

    public static final int PAYMENT_UNPAID = 0;
    public static final int PAYMENT_PAID = 1;
    public static final int PAYMENT_REFUNDED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long publisherId;

    private Long runnerId;

    private String title;

    private String description;

    private String pickupLocation;

    private String deliveryLocation;

    private BigDecimal distanceKm;

    private BigDecimal weightKg;

    private Integer urgent;

    private String contactPhone;

    private BigDecimal price;

    private Integer status;

    private Integer paymentStatus;

    private Integer auditStatus;

    private String auditRemark;

    private LocalDateTime acceptedTime;

    private LocalDateTime paidTime;

    private LocalDateTime completedTime;

    private LocalDateTime cancelledTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
