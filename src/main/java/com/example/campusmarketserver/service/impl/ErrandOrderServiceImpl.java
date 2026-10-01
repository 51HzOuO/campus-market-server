package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.dto.ErrandCreateRequest;
import com.example.campusmarketserver.entity.ErrandOrder;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.mapper.ErrandOrderMapper;
import com.example.campusmarketserver.service.ErrandOrderService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.vo.ErrandOrderVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ErrandOrderServiceImpl extends ServiceImpl<ErrandOrderMapper, ErrandOrder> implements ErrandOrderService {

    private static final BigDecimal BASE_FEE = new BigDecimal("3.00");
    private static final BigDecimal DISTANCE_FEE = new BigDecimal("1.50");
    private static final BigDecimal WEIGHT_FEE = new BigDecimal("0.80");
    private static final BigDecimal URGENT_FEE = new BigDecimal("3.00");

    private final UserService userService;

    public ErrandOrderServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public BigDecimal calculatePrice(BigDecimal distanceKm, BigDecimal weightKg, Integer urgent) {
        BigDecimal distance = nonNegative(distanceKm);
        BigDecimal weight = nonNegative(weightKg);
        BigDecimal extraWeight = weight.subtract(BigDecimal.ONE).max(BigDecimal.ZERO);
        BigDecimal amount = BASE_FEE
                .add(distance.multiply(DISTANCE_FEE))
                .add(extraWeight.multiply(WEIGHT_FEE));
        if (Integer.valueOf(1).equals(urgent)) {
            amount = amount.add(URGENT_FEE);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public ErrandOrder create(Long publisherId, ErrandCreateRequest request) {
        ErrandOrder order = new ErrandOrder();
        order.setPublisherId(publisherId);
        order.setTitle(request.getTitle().trim());
        order.setDescription(request.getDescription().trim());
        order.setPickupLocation(request.getPickupLocation().trim());
        order.setDeliveryLocation(request.getDeliveryLocation().trim());
        order.setDistanceKm(nonNegative(request.getDistanceKm()));
        order.setWeightKg(nonNegative(request.getWeightKg()));
        order.setUrgent(Integer.valueOf(1).equals(request.getUrgent()) ? 1 : 0);
        order.setContactPhone(request.getContactPhone() == null ? null : request.getContactPhone().trim());
        order.setPrice(calculatePrice(order.getDistanceKm(), order.getWeightKg(), order.getUrgent()));
        order.setStatus(ErrandOrder.STATUS_WAITING);
        order.setPaymentStatus(ErrandOrder.PAYMENT_UNPAID);
        // 新订单先进入审核队列，由审核员审核通过后才会出现在可接单列表。
        order.setAuditStatus(ErrandOrder.AUDIT_PENDING);
        LocalDateTime now = LocalDateTime.now();
        order.setCreateTime(now);
        order.setUpdateTime(now);
        save(order);
        return order;
    }

    @Override
    public List<ErrandOrderVO> toViews(List<ErrandOrder> orders) {
        if (orders == null || orders.isEmpty()) return Collections.emptyList();
        List<Long> ids = orders.stream()
                .flatMap(order -> java.util.stream.Stream.of(order.getPublisherId(), order.getRunnerId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> users = ids.isEmpty() ? Collections.emptyMap() : userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));
        return orders.stream().map(order -> {
            ErrandOrderVO vo = ErrandOrderVO.from(order);
            User publisher = users.get(order.getPublisherId());
            User runner = users.get(order.getRunnerId());
            vo.setPublisherNickname(publisher == null ? "匿名用户" : publisher.getNickname());
            vo.setRunnerNickname(runner == null ? null : runner.getNickname());
            return vo;
        }).collect(Collectors.toList());
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.signum() < 0) return BigDecimal.ZERO.setScale(2);
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
