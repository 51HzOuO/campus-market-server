package com.example.campusmarketserver.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.campusmarketserver.dto.ErrandCreateRequest;
import com.example.campusmarketserver.entity.ErrandOrder;
import com.example.campusmarketserver.vo.ErrandOrderVO;

import java.math.BigDecimal;
import java.util.List;

public interface ErrandOrderService extends IService<ErrandOrder> {

    /** 按距离、重量和加急标记计算跑腿费用。 */
    BigDecimal calculatePrice(BigDecimal distanceKm, BigDecimal weightKg, Integer urgent);

    /** 创建订单，价格、审核和生命周期字段由服务端设置。 */
    ErrandOrder create(Long publisherId, ErrandCreateRequest request);

    /** 批量填充发布人和接单人昵称，供小程序直接展示。 */
    List<ErrandOrderVO> toViews(List<ErrandOrder> orders);
}
