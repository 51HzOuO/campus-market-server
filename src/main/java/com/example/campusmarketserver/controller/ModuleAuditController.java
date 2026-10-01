package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.*;
import com.example.campusmarketserver.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 审核员和管理员共用的三类业务内容审核接口。 */
@RestController
@RequestMapping("/module-audit")
public class ModuleAuditController {

    private final UserService userService;
    private final ErrandOrderService errandOrderService;
    private final SecondHandItemService secondHandItemService;
    private final ClubService clubService;
    private final ClubActivityService clubActivityService;

    public ModuleAuditController(UserService userService,
                                 ErrandOrderService errandOrderService,
                                 SecondHandItemService secondHandItemService,
                                 ClubService clubService,
                                 ClubActivityService clubActivityService) {
        this.userService = userService;
        this.errandOrderService = errandOrderService;
        this.secondHandItemService = secondHandItemService;
        this.clubService = clubService;
        this.clubActivityService = clubActivityService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam String type) {
        if (!isReviewer()) return Result.error(403, "无权限");
        List<Map<String, Object>> result = new ArrayList<>();
        switch (normalize(type)) {
            case "errand":
                errandOrderService.list(new LambdaQueryWrapper<ErrandOrder>()
                        .eq(ErrandOrder::getAuditStatus, ErrandOrder.AUDIT_PENDING)
                        .orderByDesc(ErrandOrder::getCreateTime))
                        .forEach(item -> result.add(errand(item)));
                break;
            case "market":
                secondHandItemService.list(new LambdaQueryWrapper<SecondHandItem>()
                        .eq(SecondHandItem::getAuditStatus, 0)
                        .orderByDesc(SecondHandItem::getCreateTime))
                        .forEach(item -> result.add(market(item)));
                break;
            case "club":
                clubService.list(new LambdaQueryWrapper<Club>()
                        .eq(Club::getStatus, 0)
                        .orderByDesc(Club::getCreateTime))
                        .forEach(item -> result.add(club(item)));
                break;
            case "activity":
                clubActivityService.list(new LambdaQueryWrapper<ClubActivity>()
                        .eq(ClubActivity::getStatus, 0)
                        .orderByDesc(ClubActivity::getCreateTime))
                        .forEach(item -> result.add(activity(item)));
                break;
            default:
                return Result.error(400, "未知业务类型");
        }
        return Result.success(result);
    }

    @PostMapping("/review")
    public Result<String> review(@RequestBody Map<String, Object> body) {
        if (!isReviewer()) return Result.error(403, "无权限");
        String type = normalize(String.valueOf(body.get("type")));
        Long id = longValue(body.get("id"));
        Integer status = intValue(body.get("status"));
        if (id == null || status == null || (status != 1 && status != 2)) {
            return Result.error(400, "审核参数错误");
        }
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        boolean updated;
        switch (type) {
            case "errand": {
                ErrandOrder item = errandOrderService.getById(id);
                if (item == null) return Result.error(404, "跑腿订单不存在");
                item.setAuditStatus(status);
                item.setAuditRemark(remark);
                updated = errandOrderService.updateById(item);
                break;
            }
            case "market": {
                SecondHandItem item = secondHandItemService.getById(id);
                if (item == null) return Result.error(404, "商品不存在");
                item.setAuditStatus(status);
                item.setAuditRemark(remark);
                updated = secondHandItemService.updateById(item);
                break;
            }
            case "club": {
                Club item = clubService.getById(id);
                if (item == null) return Result.error(404, "社团不存在");
                item.setStatus(status);
                item.setRejectReason(status == 2 ? remark : null);
                updated = clubService.updateById(item);
                break;
            }
            case "activity": {
                ClubActivity item = clubActivityService.getById(id);
                if (item == null) return Result.error(404, "活动不存在");
                item.setStatus(status);
                item.setRejectReason(status == 2 ? remark : null);
                updated = clubActivityService.updateById(item);
                break;
            }
            default:
                return Result.error(400, "未知业务类型");
        }
        return updated ? Result.success(status == 1 ? "审核通过" : "已驳回") : Result.error("审核失败");
    }

    private boolean isReviewer() {
        Long id = UserContext.getUserId();
        User user = id == null ? null : userService.getById(id);
        return user != null && (Integer.valueOf(1).equals(user.getRole()) || Integer.valueOf(2).equals(user.getRole()));
    }

    private static String normalize(String type) {
        if (type == null) return "";
        String value = type.trim().toLowerCase();
        if ("second-hand".equals(value) || "secondhand".equals(value) || "item".equals(value)) return "market";
        return value;
    }

    private static Long longValue(Object value) {
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException e) { return null; }
    }

    private static Integer intValue(Object value) {
        try { return value == null ? null : Integer.valueOf(String.valueOf(value)); }
        catch (NumberFormatException e) { return null; }
    }

    private static Map<String, Object> base(Long id, String type, String title, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id); map.put("type", type); map.put("title", title); map.put("status", status);
        return map;
    }

    private static Map<String, Object> errand(ErrandOrder item) {
        Map<String, Object> map = base(item.getId(), "errand", item.getTitle(), item.getStatus());
        map.put("description", item.getDescription()); map.put("price", item.getPrice());
        map.put("auditStatus", item.getAuditStatus()); map.put("createTime", item.getCreateTime());
        return map;
    }

    private static Map<String, Object> market(SecondHandItem item) {
        Map<String, Object> map = base(item.getId(), "market", item.getTitle(), item.getStatus());
        map.put("description", item.getDescription()); map.put("price", item.getPrice());
        map.put("auditStatus", item.getAuditStatus()); map.put("createTime", item.getCreateTime());
        return map;
    }

    private static Map<String, Object> club(Club item) {
        Map<String, Object> map = base(item.getId(), "club", item.getName(), item.getStatus());
        map.put("description", item.getDescription()); map.put("createTime", item.getCreateTime());
        return map;
    }

    private static Map<String, Object> activity(ClubActivity item) {
        Map<String, Object> map = base(item.getId(), "activity", item.getTitle(), item.getStatus());
        map.put("description", item.getContent()); map.put("location", item.getLocation());
        map.put("startTime", item.getStartTime()); map.put("createTime", item.getCreateTime());
        return map;
    }
}
