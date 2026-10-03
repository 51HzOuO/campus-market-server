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

/** 管理员对跑腿、二手和社团内容的统一管理接口。 */
@RestController
@RequestMapping("/admin/modules")
public class ModuleAdminController {
    private final UserService userService;
    private final ErrandOrderService errandOrderService;
    private final SecondHandItemService secondHandItemService;
    private final ClubService clubService;
    private final ClubActivityService clubActivityService;

    public ModuleAdminController(UserService userService, ErrandOrderService errandOrderService,
                                 SecondHandItemService secondHandItemService, ClubService clubService,
                                 ClubActivityService clubActivityService) {
        this.userService = userService; this.errandOrderService = errandOrderService;
        this.secondHandItemService = secondHandItemService; this.clubService = clubService;
        this.clubActivityService = clubActivityService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam String type) {
        if (!isAdmin()) return Result.error(403, "无权限");
        List<Map<String, Object>> result = new ArrayList<>();
        switch (normalize(type)) {
            case "errand": errandOrderService.list(new LambdaQueryWrapper<ErrandOrder>().orderByDesc(ErrandOrder::getCreateTime)).forEach(x -> {
                Map<String, Object> row = item(x.getId(), "errand", x.getTitle(), x.getStatus(), x.getAuditStatus());
                row.put("description", x.getDescription()); row.put("route", safe(x.getPickupLocation()) + " → " + safe(x.getDeliveryLocation()));
                row.put("price", x.getPrice()); row.put("urgent", x.getUrgent()); row.put("createTime", x.getCreateTime()); result.add(row);
            }); break;
            case "market": secondHandItemService.list(new LambdaQueryWrapper<SecondHandItem>().orderByDesc(SecondHandItem::getCreateTime)).forEach(x -> {
                Map<String, Object> row = item(x.getId(), "market", x.getTitle(), x.getStatus(), x.getAuditStatus());
                row.put("description", x.getDescription()); row.put("category", x.getCategory()); row.put("price", x.getPrice());
                row.put("location", x.getLocation()); row.put("images", x.getImages()); row.put("createTime", x.getCreateTime()); result.add(row);
            }); break;
            case "club": clubService.list(new LambdaQueryWrapper<Club>().orderByDesc(Club::getCreateTime)).forEach(x -> {
                Map<String, Object> row = item(x.getId(), "club", x.getName(), x.getStatus(), null);
                row.put("description", x.getDescription()); row.put("memberCount", x.getMemberCount()); row.put("logo", x.getLogo());
                row.put("createTime", x.getCreateTime()); result.add(row);
            }); break;
            case "activity": clubActivityService.list(new LambdaQueryWrapper<ClubActivity>().orderByDesc(ClubActivity::getCreateTime)).forEach(x -> result.add(item(x.getId(), "activity", x.getTitle(), x.getStatus(), null))); break;
            default: return Result.error(400, "未知业务类型");
        }
        return Result.success(result);
    }

    @PostMapping("/status")
    public Result<String> updateStatus(@RequestBody Map<String, Object> body) {
        if (!isAdmin()) return Result.error(403, "无权限");
        String type = normalize(String.valueOf(body.get("type")));
        Long id = longValue(body.get("id"));
        Integer status = intValue(body.get("status"));
        if (id == null || status == null) return Result.error(400, "参数错误");
        boolean updated;
        switch (type) {
            case "errand": { ErrandOrder x = errandOrderService.getById(id); if (x == null) return Result.error(404, "订单不存在"); x.setStatus(status); updated = errandOrderService.updateById(x); break; }
            case "market": { SecondHandItem x = secondHandItemService.getById(id); if (x == null) return Result.error(404, "商品不存在"); x.setStatus(status); updated = secondHandItemService.updateById(x); break; }
            case "club": { Club x = clubService.getById(id); if (x == null) return Result.error(404, "社团不存在"); x.setStatus(status); updated = clubService.updateById(x); break; }
            case "activity": { ClubActivity x = clubActivityService.getById(id); if (x == null) return Result.error(404, "活动不存在"); x.setStatus(status); updated = clubActivityService.updateById(x); break; }
            default: return Result.error(400, "未知业务类型");
        }
        return updated ? Result.success("状态已更新") : Result.error("更新失败");
    }

    private boolean isAdmin() { Long id = UserContext.getUserId(); User u = id == null ? null : userService.getById(id); return u != null && Integer.valueOf(1).equals(u.getRole()); }
    private static String normalize(String type) { if (type == null) return ""; String v = type.trim().toLowerCase(); return ("second-hand".equals(v) || "secondhand".equals(v) || "item".equals(v)) ? "market" : v; }
    private static Long longValue(Object v) { try { return v == null ? null : Long.valueOf(String.valueOf(v)); } catch (Exception e) { return null; } }
    private static Integer intValue(Object v) { try { return v == null ? null : Integer.valueOf(String.valueOf(v)); } catch (Exception e) { return null; } }
    private static String safe(String value) { return value == null || value.isBlank() ? "未填写" : value; }
    private static Map<String,Object> item(Long id, String type, String title, Integer status, Integer auditStatus) { Map<String,Object> m = new HashMap<>(); m.put("id", id); m.put("type", type); m.put("title", title); m.put("status", status); m.put("auditStatus", auditStatus); return m; }
}
