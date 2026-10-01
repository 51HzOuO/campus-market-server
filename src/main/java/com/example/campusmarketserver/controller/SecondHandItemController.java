package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.SecondHandItem;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.SecondHandItemService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.AvatarUtil;
import com.example.campusmarketserver.util.SensitiveWordUtil;
import com.example.campusmarketserver.vo.SecondHandItemVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 二手商品发布、浏览和交易接口。 */
@RestController
@RequestMapping("/second-hand")
public class SecondHandItemController {

    private final SecondHandItemService itemService;
    private final UserService userService;

    public SecondHandItemController(SecondHandItemService itemService, UserService userService) {
        this.itemService = itemService;
        this.userService = userService;
    }

    /** 查询审核通过且仍在售的商品。 */
    @GetMapping("/list")
    public Result<IPage<SecondHandItemVO>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 50);
        LambdaQueryWrapper<SecondHandItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecondHandItem::getAuditStatus, 1)
                .eq(SecondHandItem::getStatus, 1);
        if (StringUtils.hasText(category)) {
            wrapper.eq(SecondHandItem::getCategory, category.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String text = keyword.trim();
            wrapper.and(w -> w.like(SecondHandItem::getTitle, text)
                    .or().like(SecondHandItem::getDescription, text));
        }
        wrapper.orderByDesc(SecondHandItem::getCreateTime);
        IPage<SecondHandItem> source = itemService.page(new Page<>(page, size), wrapper);
        return Result.success(toPage(source));
    }

    @GetMapping("/detail/{id}")
    public Result<SecondHandItemVO> detail(@PathVariable Long id) {
        SecondHandItem item = itemService.getById(id);
        if (item == null) return Result.error(404, "商品不存在");
        Long viewerId = UserContext.getUserId();
        boolean ownerOrBuyer = viewerId != null
                && (viewerId.equals(item.getUserId()) || viewerId.equals(item.getBuyerId()));
        if (!Integer.valueOf(1).equals(item.getAuditStatus()) && !ownerOrBuyer) {
            return Result.error(404, "商品不存在");
        }
        return Result.success(toVO(item));
    }

    /** 当前用户发布的商品（包含待审核、下架和已售出商品）。 */
    @GetMapping("/my")
    public Result<List<SecondHandItemVO>> my() {
        Long userId = currentUserId();
        if (userId == null) return Result.error(401, "未登录");
        List<SecondHandItem> items = itemService.list(new LambdaQueryWrapper<SecondHandItem>()
                .eq(SecondHandItem::getUserId, userId)
                .orderByDesc(SecondHandItem::getCreateTime));
        return Result.success(toVOList(items));
    }

    /** 当前用户已经购买的商品。 */
    @GetMapping("/purchases")
    public Result<List<SecondHandItemVO>> purchases() {
        Long userId = currentUserId();
        if (userId == null) return Result.error(401, "未登录");
        List<SecondHandItem> items = itemService.list(new LambdaQueryWrapper<SecondHandItem>()
                .eq(SecondHandItem::getBuyerId, userId)
                .orderByDesc(SecondHandItem::getUpdateTime));
        return Result.success(toVOList(items));
    }

    @PostMapping("/create")
    public Result<SecondHandItemVO> create(@RequestBody SecondHandItem request) {
        Long userId = currentUserId();
        if (userId == null) return Result.error(401, "未登录");
        String validation = validateInput(request);
        if (validation != null) return Result.error(400, validation);
        if (hasSensitiveWord(request.getTitle(), request.getDescription())) {
            return Result.error(400, "商品标题或描述包含敏感词，请修改后重新发布");
        }
        SecondHandItem item = new SecondHandItem();
        copyEditableFields(request, item);
        item.setUserId(userId);
        item.setStatus(1);
        item.setAuditStatus(0);
        item.setAuditRemark(null);
        item.setBuyerId(null);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        itemService.save(item);
        return Result.success(toVO(item));
    }

    /** 修改自己的商品，修改后需要重新审核。 */
    @PostMapping("/update")
    public Result<SecondHandItemVO> update(@RequestBody SecondHandItem request) {
        Long userId = currentUserId();
        if (userId == null) return Result.error(401, "未登录");
        if (request == null || request.getId() == null) return Result.error(400, "商品ID不能为空");
        SecondHandItem old = itemService.getById(request.getId());
        if (old == null) return Result.error(404, "商品不存在");
        if (!userId.equals(old.getUserId())) return Result.error(403, "只能修改自己的商品");
        if (Integer.valueOf(2).equals(old.getStatus())) return Result.error(400, "已售出商品不能修改");
        String validation = validateInput(request);
        if (validation != null) return Result.error(400, validation);
        if (hasSensitiveWord(request.getTitle(), request.getDescription())) {
            return Result.error(400, "商品标题或描述包含敏感词，请修改后重新发布");
        }
        copyEditableFields(request, old);
        old.setAuditStatus(0);
        old.setAuditRemark(null);
        old.setStatus(1);
        old.setUpdateTime(LocalDateTime.now());
        itemService.updateById(old);
        return Result.success(toVO(old));
    }

    /** 下架自己的商品；管理员也可使用该接口。 */
    @PostMapping("/off-shelf")
    public Result<String> offShelf(@RequestBody Map<String, Object> body) {
        Long id = longValue(body == null ? null : body.get("itemId"));
        if (id == null) return Result.error(400, "商品ID不能为空");
        SecondHandItem item = itemService.getById(id);
        if (item == null) return Result.error(404, "商品不存在");
        if (!canManage(item)) return Result.error(403, "无权限");
        if (Integer.valueOf(2).equals(item.getStatus())) return Result.error(400, "商品已售出");
        item.setStatus(0);
        item.setUpdateTime(LocalDateTime.now());
        itemService.updateById(item);
        return Result.success("商品已下架");
    }

    /** 恢复已审核通过的商品，或将商品下架。 */
    @PostMapping("/status")
    public Result<String> status(@RequestBody Map<String, Object> body) {
        Long id = longValue(body == null ? null : body.get("itemId"));
        Integer status = intValue(body == null ? null : body.get("status"));
        if (id == null || status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态参数错误");
        }
        SecondHandItem item = itemService.getById(id);
        if (item == null) return Result.error(404, "商品不存在");
        if (!canManage(item)) return Result.error(403, "无权限");
        if (status == 1 && !Integer.valueOf(1).equals(item.getAuditStatus())) {
            return Result.error(400, "商品尚未审核通过");
        }
        if (Integer.valueOf(2).equals(item.getStatus())) return Result.error(400, "商品已售出");
        item.setStatus(status);
        item.setUpdateTime(LocalDateTime.now());
        itemService.updateById(item);
        return Result.success(status == 1 ? "商品已上架" : "商品已下架");
    }

    /** 购买商品。采用带状态条件的更新，避免同一商品被重复购买。 */
    @PostMapping("/buy")
    public Result<SecondHandItemVO> buy(@RequestBody Map<String, Object> body) {
        Long userId = currentUserId();
        if (userId == null) return Result.error(401, "未登录");
        Long id = longValue(body == null ? null : body.get("itemId"));
        if (id == null) return Result.error(400, "商品ID不能为空");
        SecondHandItem old = itemService.getById(id);
        if (old == null) return Result.error(404, "商品不存在");
        if (userId.equals(old.getUserId())) return Result.error(400, "不能购买自己发布的商品");
        if (!Integer.valueOf(1).equals(old.getStatus()) || !Integer.valueOf(1).equals(old.getAuditStatus())) {
            return Result.error(400, "商品当前不可购买");
        }
        LambdaUpdateWrapper<SecondHandItem> update = new LambdaUpdateWrapper<>();
        update.eq(SecondHandItem::getId, id)
                .eq(SecondHandItem::getStatus, 1)
                .eq(SecondHandItem::getAuditStatus, 1)
                .isNull(SecondHandItem::getBuyerId)
                .set(SecondHandItem::getStatus, 2)
                .set(SecondHandItem::getBuyerId, userId)
                .set(SecondHandItem::getUpdateTime, LocalDateTime.now());
        if (!itemService.update(update)) return Result.error(409, "商品刚刚被其他用户购买");
        SecondHandItem bought = itemService.getById(id);
        return Result.success(toVO(bought));
    }

    private Long currentUserId() {
        return UserContext.getUserId();
    }

    private boolean canManage(SecondHandItem item) {
        Long userId = currentUserId();
        if (userId == null) return false;
        if (userId.equals(item.getUserId())) return true;
        User user = userService.getById(userId);
        return user != null && Integer.valueOf(1).equals(user.getRole());
    }

    private String validateInput(SecondHandItem item) {
        if (item == null) return "商品内容不能为空";
        if (!StringUtils.hasText(item.getTitle()) || item.getTitle().trim().length() > 80) {
            return "商品标题不能为空且不能超过80字";
        }
        if (!StringUtils.hasText(item.getDescription()) || item.getDescription().trim().length() > 2000) {
            return "商品描述不能为空且不能超过2000字";
        }
        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return "请输入有效价格";
        }
        if (item.getPrice().scale() > 2) return "价格最多保留两位小数";
        if (item.getCategory() != null && item.getCategory().trim().length() > 30) return "分类不能超过30字";
        if (item.getLocation() != null && item.getLocation().trim().length() > 100) return "交易地点不能超过100字";
        if (item.getContact() != null && item.getContact().trim().length() > 100) return "联系方式不能超过100字";
        return null;
    }

    private static boolean hasSensitiveWord(String title, String description) {
        String text = (title == null ? "" : title) + (description == null ? "" : description);
        return !SensitiveWordUtil.detect(text).isEmpty();
    }

    private static void copyEditableFields(SecondHandItem from, SecondHandItem to) {
        to.setTitle(from.getTitle().trim());
        to.setDescription(from.getDescription().trim());
        to.setImages(StringUtils.hasText(from.getImages()) ? from.getImages() : "[]");
        to.setPrice(from.getPrice());
        to.setCategory(StringUtils.hasText(from.getCategory()) ? from.getCategory().trim() : null);
        to.setLocation(StringUtils.hasText(from.getLocation()) ? from.getLocation().trim() : null);
        to.setContact(StringUtils.hasText(from.getContact()) ? from.getContact().trim() : null);
    }

    private IPage<SecondHandItemVO> toPage(IPage<SecondHandItem> source) {
        IPage<SecondHandItemVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(toVOList(source.getRecords()));
        return result;
    }

    private List<SecondHandItemVO> toVOList(List<SecondHandItem> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        List<Long> userIds = items.stream().map(SecondHandItem::getUserId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> buyerIds = items.stream().map(SecondHandItem::getBuyerId).filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        List<Long> ids = new java.util.ArrayList<>(userIds);
        buyerIds.forEach(id -> { if (!ids.contains(id)) ids.add(id); });
        Map<Long, User> users = ids.isEmpty() ? Collections.emptyMap() : userService.listByIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return items.stream().map(item -> {
            SecondHandItemVO vo = SecondHandItemVO.fromItem(item);
            User seller = users.get(item.getUserId());
            if (seller != null) { vo.setSellerNickname(seller.getNickname()); vo.setSellerAvatar(AvatarUtil.fullUrl(seller.getAvatar())); }
            User buyer = users.get(item.getBuyerId());
            if (buyer != null) vo.setBuyerNickname(buyer.getNickname());
            return vo;
        }).collect(Collectors.toList());
    }

    private SecondHandItemVO toVO(SecondHandItem item) {
        List<SecondHandItemVO> list = toVOList(Collections.singletonList(item));
        return list.isEmpty() ? null : list.get(0);
    }

    private static Long longValue(Object value) { try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; } }
    private static Integer intValue(Object value) { try { return value == null ? null : Integer.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; } }
}
