package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.dto.ErrandCreateRequest;
import com.example.campusmarketserver.dto.ErrandPriceRequest;
import com.example.campusmarketserver.entity.ErrandOrder;
import com.example.campusmarketserver.service.ErrandOrderService;
import com.example.campusmarketserver.vo.ErrandOrderVO;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 跑腿需求的发布、接单、支付和履约接口。 */
@RestController
@RequestMapping("/errand")
public class ErrandOrderController {

    private final ErrandOrderService errandOrderService;

    public ErrandOrderController(ErrandOrderService errandOrderService) {
        this.errandOrderService = errandOrderService;
    }

    /**
     * 计算预估价格：基础 3 元 + 每公里 1.5 元 + 超出 1kg 的部分每公斤 0.8 元，
     * 加急另加 3 元。金额由服务端重算，客户端传入的 price 不会被采用。
     */
    @PostMapping("/calculate")
    public Result<Map<String, Object>> calculate(@RequestBody ErrandPriceRequest request) {
        if (request == null || invalidMeasure(request.getDistanceKm(), request.getWeightKg())) {
            return Result.error(400, "距离和重量必须是非负数字");
        }
        BigDecimal price = errandOrderService.calculatePrice(
                request.getDistanceKm(), request.getWeightKg(), request.getUrgent());
        Map<String, Object> data = new HashMap<>();
        data.put("distanceKm", request.getDistanceKm());
        data.put("weightKg", request.getWeightKg());
        data.put("urgent", Integer.valueOf(1).equals(request.getUrgent()) ? 1 : 0);
        data.put("price", price);
        return Result.success(data);
    }

    /** GET 形式便于调试和微信开发者工具直接查验。 */
    @GetMapping("/price")
    public Result<Map<String, Object>> price(
            @RequestParam BigDecimal distanceKm,
            @RequestParam BigDecimal weightKg,
            @RequestParam(defaultValue = "0") Integer urgent) {
        ErrandPriceRequest request = new ErrandPriceRequest();
        request.setDistanceKm(distanceKm);
        request.setWeightKg(weightKg);
        request.setUrgent(urgent);
        return calculate(request);
    }

    @PostMapping("/create")
    public Result<ErrandOrderVO> create(@RequestBody ErrandCreateRequest request) {
        String error = validateCreate(request);
        if (error != null) return Result.error(400, error);
        Long userId = currentUserId();
        ErrandOrder order = errandOrderService.create(userId, request);
        return Result.success(errandOrderService.toViews(List.of(order)).get(0));
    }

    /**
     * 可接单列表。默认只返回审核通过且处于待接单状态的需求。
     * scope=mine 返回当前用户发布或接取的全部订单；status 可用于筛选生命周期状态。
     */
    @GetMapping("/list")
    public Result<List<ErrandOrderVO>> list(
            @RequestParam(defaultValue = "available") String scope,
            @RequestParam(required = false) Integer status) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<ErrandOrder> wrapper = new LambdaQueryWrapper<>();
        if ("mine".equalsIgnoreCase(scope)) {
            if (userId == null) return Result.error(401, "未登录");
            wrapper.and(w -> w.eq(ErrandOrder::getPublisherId, userId)
                    .or().eq(ErrandOrder::getRunnerId, userId));
            if (status != null) wrapper.eq(ErrandOrder::getStatus, status);
        } else {
            wrapper.eq(ErrandOrder::getAuditStatus, ErrandOrder.AUDIT_APPROVED)
                    .eq(ErrandOrder::getStatus,
                            status == null ? ErrandOrder.STATUS_WAITING : status);
        }
        wrapper.orderByDesc(ErrandOrder::getUrgent)
                .orderByDesc(ErrandOrder::getCreateTime);
        return Result.success(errandOrderService.toViews(errandOrderService.list(wrapper)));
    }

    @GetMapping("/my")
    public Result<List<ErrandOrderVO>> my() {
        return list("mine", null);
    }

    @GetMapping("/detail/{id}")
    public Result<ErrandOrderVO> detail(@PathVariable Long id) {
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) return Result.error(404, "跑腿订单不存在");
        Long viewerId = UserContext.getUserId();
        boolean participant = viewerId != null
                && (viewerId.equals(order.getPublisherId()) || viewerId.equals(order.getRunnerId()));
        if (!Integer.valueOf(ErrandOrder.AUDIT_APPROVED).equals(order.getAuditStatus()) && !participant) {
            return Result.error(404, "跑腿订单不存在");
        }
        ErrandOrderVO view = errandOrderService.toViews(List.of(order)).get(0);
        // Contact information is only needed by the two parties to the order.
        if (!participant) {
            view.setContactPhone(null);
        }
        return Result.success(view);
    }

    /** 抢单使用带 status 条件的更新，两个用户同时点击时只有一个能成功。 */
    @PostMapping("/{id}/accept")
    public Result<ErrandOrderVO> accept(@PathVariable Long id) {
        Long userId = currentUserId();
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) return Result.error(404, "跑腿订单不存在");
        if (userId.equals(order.getPublisherId())) return Result.error(400, "不能接自己发布的需求");
        if (!Integer.valueOf(ErrandOrder.AUDIT_APPROVED).equals(order.getAuditStatus())) {
            return Result.error(400, "订单尚未通过审核");
        }
        LambdaUpdateWrapper<ErrandOrder> update = new LambdaUpdateWrapper<>();
        update.eq(ErrandOrder::getId, id)
                .eq(ErrandOrder::getStatus, ErrandOrder.STATUS_WAITING)
                .set(ErrandOrder::getRunnerId, userId)
                .set(ErrandOrder::getStatus, ErrandOrder.STATUS_ACCEPTED)
                .set(ErrandOrder::getAcceptedTime, LocalDateTime.now())
                .set(ErrandOrder::getUpdateTime, LocalDateTime.now());
        if (errandOrderService.update(update)) {
            return Result.success(errandOrderService.toViews(List.of(errandOrderService.getById(id))).get(0));
        }
        return Result.error(409, "订单已被其他人接单或已失效");
    }

    /**
     * 模拟支付：不调用任何扣款或微信支付接口，点击后直接把订单标记为交易成功。
     * 真实支付接入时只需替换这里的状态变更，并保留支付回调校验。
     */
    @PostMapping("/{id}/pay")
    public Result<ErrandOrderVO> pay(@PathVariable Long id) {
        Long userId = currentUserId();
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) return Result.error(404, "跑腿订单不存在");
        if (!userId.equals(order.getPublisherId())) return Result.error(403, "只有发布者可以支付");
        if (!Integer.valueOf(ErrandOrder.STATUS_ACCEPTED).equals(order.getStatus())) {
            return Result.error(400, "当前订单不能支付");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentStatus(ErrandOrder.PAYMENT_PAID);
        order.setStatus(ErrandOrder.STATUS_COMPLETED);
        order.setPaidTime(now);
        order.setCompletedTime(now);
        order.setUpdateTime(now);
        errandOrderService.updateById(order);
        return Result.success(errandOrderService.toViews(List.of(order)).get(0));
    }

    @PostMapping("/{id}/complete")
    public Result<ErrandOrderVO> complete(@PathVariable Long id) {
        Long userId = currentUserId();
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) return Result.error(404, "跑腿订单不存在");
        if (!userId.equals(order.getRunnerId())) return Result.error(403, "只有接单人可以标记完成");
        if (!Integer.valueOf(ErrandOrder.STATUS_IN_PROGRESS).equals(order.getStatus())) {
            return Result.error(400, "当前订单不能标记完成");
        }
        order.setStatus(ErrandOrder.STATUS_WAITING_CONFIRM);
        order.setUpdateTime(LocalDateTime.now());
        errandOrderService.updateById(order);
        return Result.success(errandOrderService.toViews(List.of(order)).get(0));
    }

    /** 发布者确认收货并完成订单，必须已支付。 */
    @PostMapping("/{id}/confirm")
    public Result<ErrandOrderVO> confirm(@PathVariable Long id) {
        Long userId = currentUserId();
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) return Result.error(404, "跑腿订单不存在");
        if (!userId.equals(order.getPublisherId())) return Result.error(403, "只有发布者可以确认收货");
        if (!Integer.valueOf(ErrandOrder.STATUS_WAITING_CONFIRM).equals(order.getStatus())
                || !Integer.valueOf(ErrandOrder.PAYMENT_PAID).equals(order.getPaymentStatus())) {
            return Result.error(400, "订单尚未达到确认条件");
        }
        order.setStatus(ErrandOrder.STATUS_COMPLETED);
        order.setCompletedTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        errandOrderService.updateById(order);
        return Result.success(errandOrderService.toViews(List.of(order)).get(0));
    }

    @PostMapping("/{id}/cancel")
    public Result<ErrandOrderVO> cancel(@PathVariable Long id) {
        Long userId = currentUserId();
        ErrandOrder order = errandOrderService.getById(id);
        if (order == null) return Result.error(404, "跑腿订单不存在");
        if (!userId.equals(order.getPublisherId()) && !userId.equals(order.getRunnerId())) {
            return Result.error(403, "无权取消该订单");
        }
        Integer status = order.getStatus();
        if (!(Integer.valueOf(ErrandOrder.STATUS_WAITING).equals(status)
                || Integer.valueOf(ErrandOrder.STATUS_ACCEPTED).equals(status)
                || Integer.valueOf(ErrandOrder.STATUS_IN_PROGRESS).equals(status))) {
            return Result.error(400, "当前订单不能取消");
        }
        order.setStatus(ErrandOrder.STATUS_CANCELLED);
        if (Integer.valueOf(ErrandOrder.PAYMENT_PAID).equals(order.getPaymentStatus())) {
            order.setPaymentStatus(ErrandOrder.PAYMENT_REFUNDED);
        }
        order.setCancelledTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        errandOrderService.updateById(order);
        return Result.success(errandOrderService.toViews(List.of(order)).get(0));
    }

    private Long currentUserId() {
        Long id = UserContext.getUserId();
        if (id == null) throw new IllegalStateException("未登录");
        return id;
    }

    private static boolean invalidMeasure(BigDecimal distance, BigDecimal weight) {
        return distance == null || weight == null || distance.signum() < 0 || weight.signum() < 0
                || distance.compareTo(new BigDecimal("1000")) > 0
                || weight.compareTo(new BigDecimal("100")) > 0;
    }

    private static String validateCreate(ErrandCreateRequest request) {
        if (request == null) return "请求不能为空";
        if (blank(request.getTitle()) || request.getTitle().trim().length() > 80) return "标题不能为空且不超过80字";
        if (blank(request.getDescription()) || request.getDescription().trim().length() > 1000) return "请填写需求说明（不超过1000字）";
        if (blank(request.getPickupLocation()) || request.getPickupLocation().trim().length() > 255) return "请填写取件地点";
        if (blank(request.getDeliveryLocation()) || request.getDeliveryLocation().trim().length() > 255) return "请填写送达地点";
        if (invalidMeasure(request.getDistanceKm(), request.getWeightKg())) return "距离和重量必须在合理范围内";
        if (request.getUrgent() != null && request.getUrgent() != 0 && request.getUrgent() != 1) return "加急参数错误";
        if (request.getContactPhone() != null && request.getContactPhone().trim().length() > 32) return "联系方式过长";
        return null;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
