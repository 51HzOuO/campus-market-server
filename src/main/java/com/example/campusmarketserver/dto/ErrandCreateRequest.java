package com.example.campusmarketserver.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 发布跑腿需求的请求参数。
 * userId、price、status 等服务端字段由后端生成，客户端不能覆盖。
 */
@Data
public class ErrandCreateRequest {

    private String title;
    private String description;
    private String pickupLocation;
    private String deliveryLocation;
    private BigDecimal distanceKm;
    private BigDecimal weightKg;
    private Integer urgent;
    private String contactPhone;
}
