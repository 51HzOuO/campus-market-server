package com.example.campusmarketserver.vo;

import com.example.campusmarketserver.entity.ErrandOrder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 小程序使用的跑腿订单视图。 */
@Data
public class ErrandOrderVO {

    private Long id;
    private Long publisherId;
    private String publisherNickname;
    private Long runnerId;
    private String runnerNickname;
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
    private String statusText;
    private Integer paymentStatus;
    private String paymentStatusText;
    private Integer auditStatus;
    private String auditRemark;
    private LocalDateTime acceptedTime;
    private LocalDateTime paidTime;
    private LocalDateTime completedTime;
    private LocalDateTime cancelledTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ErrandOrderVO from(ErrandOrder order) {
        ErrandOrderVO vo = new ErrandOrderVO();
        vo.id = order.getId();
        vo.publisherId = order.getPublisherId();
        vo.runnerId = order.getRunnerId();
        vo.title = order.getTitle();
        vo.description = order.getDescription();
        vo.pickupLocation = order.getPickupLocation();
        vo.deliveryLocation = order.getDeliveryLocation();
        vo.distanceKm = order.getDistanceKm();
        vo.weightKg = order.getWeightKg();
        vo.urgent = order.getUrgent();
        vo.contactPhone = order.getContactPhone();
        vo.price = order.getPrice();
        vo.status = order.getStatus();
        vo.statusText = statusText(order.getStatus());
        vo.paymentStatus = order.getPaymentStatus();
        vo.paymentStatusText = paymentStatusText(order.getPaymentStatus());
        vo.auditStatus = order.getAuditStatus();
        vo.auditRemark = order.getAuditRemark();
        vo.acceptedTime = order.getAcceptedTime();
        vo.paidTime = order.getPaidTime();
        vo.completedTime = order.getCompletedTime();
        vo.cancelledTime = order.getCancelledTime();
        vo.createTime = order.getCreateTime();
        vo.updateTime = order.getUpdateTime();
        return vo;
    }

    public static String statusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case ErrandOrder.STATUS_WAITING -> "待接单";
            case ErrandOrder.STATUS_ACCEPTED -> "已接单";
            case ErrandOrder.STATUS_IN_PROGRESS -> "进行中";
            case ErrandOrder.STATUS_WAITING_CONFIRM -> "待确认";
            case ErrandOrder.STATUS_COMPLETED -> "已完成";
            case ErrandOrder.STATUS_CANCELLED -> "已取消";
            default -> "未知";
        };
    }

    public static String paymentStatusText(Integer status) {
        if (status == null || status == ErrandOrder.PAYMENT_UNPAID) return "待支付";
        if (status == ErrandOrder.PAYMENT_PAID) return "已支付（模拟）";
        if (status == ErrandOrder.PAYMENT_REFUNDED) return "已退款";
        return "未知";
    }
}
