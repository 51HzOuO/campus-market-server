package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.Notice;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.NoticeService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.AvatarUtil;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notice")
public class NoticeController {

    private final NoticeService noticeService;
    private final UserService userService;

    public NoticeController(NoticeService noticeService, UserService userService) {
        this.noticeService = noticeService;
        this.userService = userService;
    }

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getUserId, userId)
               .orderByDesc(Notice::getCreateTime);
        List<Notice> notices = noticeService.list(wrapper);

        if (notices.isEmpty()) {
            return Result.success(List.of());
        }

        // 批量查询触发通知的用户信息
        List<Long> fromUserIds = notices.stream()
                .map(Notice::getFromUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> fromUsers = userService.listByIds(fromUserIds);
        Map<Long, User> userMap = fromUsers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = notices.stream().map(notice -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", notice.getId());
            item.put("postId", notice.getPostId());
            item.put("type", notice.getType());
            item.put("content", notice.getContent());
            item.put("isRead", notice.getIsRead());
            item.put("createTime", notice.getCreateTime());
            LocalDateTime ct = notice.getCreateTime();
            if (ct == null) ct = LocalDateTime.now();
            item.put("createTimeStr", ct.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            User fromUser = userMap.get(notice.getFromUserId());
            if (fromUser != null) {
                item.put("fromNickname", fromUser.getNickname());
                item.put("avatar", AvatarUtil.fullUrl(fromUser.getAvatar()));
            } else {
                item.put("fromNickname", "匿名用户");
                item.put("avatar", "");
            }

            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getUserId, userId)
               .eq(Notice::getIsRead, 0);
        long count = noticeService.count(wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return Result.success(data);
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public Result<String> readAll() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        LambdaUpdateWrapper<Notice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notice::getUserId, userId)
               .set(Notice::getIsRead, 1);
        noticeService.update(wrapper);

        return Result.success("已标记全部已读");
    }
}
