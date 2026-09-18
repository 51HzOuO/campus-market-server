package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.entity.Report;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.ReportService;
import com.example.campusmarketserver.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;
    private final PostService postService;
    private final UserService userService;

    public ReportController(ReportService reportService, PostService postService, UserService userService) {
        this.reportService = reportService;
        this.postService = postService;
        this.userService = userService;
    }

    /**
     * 举报列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Result.error(403, "无权限");
        }

        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Report::getCreateTime);
        List<Report> reports = reportService.list(wrapper);

        List<Map<String, Object>> result = reports.stream().map(report -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", report.getId());
            item.put("postId", report.getPostId());
            item.put("reason", report.getReason());
            item.put("reporterId", report.getReporterId());
            item.put("status", report.getStatus());
            item.put("createTime", report.getCreateTime());

            // 查询帖子信息
            Post post = postService.getById(report.getPostId());
            if (post != null) {
                item.put("postTitle", post.getTitle());
                item.put("postContent", post.getContent());
            }

            // 查询举报人信息
            User reporter = userService.getById(report.getReporterId());
            if (reporter != null) {
                item.put("reporterName", reporter.getNickname());
            }

            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 处理举报
     */
    @PostMapping("/handle")
    public Result<String> handle(@RequestBody Map<String, Long> params) {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Result.error(403, "无权限");
        }

        Long reportId = params.get("reportId");
        if (reportId == null) {
            return Result.error(400, "举报ID不能为空");
        }

        Report report = reportService.getById(reportId);
        if (report == null) {
            return Result.error(404, "举报不存在");
        }

        report.setStatus(1);
        reportService.updateById(report);

        return Result.success("已处理");
    }

    /**
     * 提交举报
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, Object> params) {
        Long currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            return Result.error(401, "未登录");
        }

        Object postIdObj = params.get("postId");
        Long postId = postIdObj != null ? Long.valueOf(postIdObj.toString()) : null;
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;

        if (postId == null) {
            return Result.error(400, "帖子ID不能为空");
        }
        if (reason == null || reason.trim().isEmpty()) {
            return Result.error(400, "举报原因不能为空");
        }

        Post post = postService.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        Report report = new Report();
        report.setPostId(postId);
        report.setReason(reason);
        report.setReporterId(currentUserId);
        report.setStatus(0);
        report.setCreateTime(LocalDateTime.now());
        reportService.save(report);

        return Result.success("举报成功");
    }
}
