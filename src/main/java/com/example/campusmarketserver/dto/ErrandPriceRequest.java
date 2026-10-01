package com.example.campusmarketserver.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 跑腿订单计价请求。 */
@Data
public class ErrandPriceRequest {

    private BigDecimal distanceKm;
    private BigDecimal weightKg;
    private Integer urgent;
}
