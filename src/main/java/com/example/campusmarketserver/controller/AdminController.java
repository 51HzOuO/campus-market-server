package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.ActivityUtil;
import com.example.campusmarketserver.util.AvatarUtil;
import com.example.campusmarketserver.vo.PostVO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final PostService postService;

    public AdminController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    /**
     * 数据统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            return Result.error(403, "无权限");
        }

        Map<String, Object> data = new HashMap<>();
        
        // 总用户数
        long totalUsers = userService.count();
        data.put("totalUsers", totalUsers);
        
        // 总帖子数
        long totalPosts = postService.count();
        data.put("totalPosts", totalPosts);
        
        // 今日新增用户
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ge(User::getCreateTime, todayStart);
        long todayUsers = userService.count(userWrapper);
        data.put("todayUsers", todayUsers);
        
        // 今日新增帖子
        LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.ge(Post::getCreateTime, todayStart);
        long todayPosts = postService.count(postWrapper);
        data.put("todayPosts", todayPosts);

        return Result.success(data);
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public Result<IPage<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            return Result.error(403, "无权限");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(User::getCreateTime);
        IPage<User> userPage = userService.page(new Page<>(page, size), wrapper);

        IPage<Map<String, Object>> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<Map<String, Object>> list = userPage.getRecords().stream().map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("nickname", user.getNickname());
            item.put("avatar", AvatarUtil.fullUrl(user.getAvatar()));
            item.put("status", user.getStatus());
            item.put("role", user.getRole());
            item.put("activityScore", user.getActivityScore());
            item.put("createTime", user.getCreateTime());
            return item;
        }).collect(Collectors.toList());
        resultPage.setRecords(list);

        return Result.success(resultPage);
    }

    /**
     * 封禁用户
     */
    @PostMapping("/ban")
    public Result<String> ban(@RequestBody Map<String, Long> params) {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            return Result.error(403, "无权限");
        }

        Long userId = params.get("userId");
        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 切换封禁状态
        int newStatus = (user.getStatus() != null && user.getStatus() == 0) ? 1 : 0;
        user.setStatus(newStatus);
        userService.updateById(user);

        return Result.success(newStatus == 0 ? "已封禁" : "已解封");
    }

    /**
     * 置顶帖子
     */
    @PostMapping("/top")
    public Result<String> top(@RequestBody Map<String, Long> params) {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || currentUser.getRole() != 1) {
            return Result.error(403, "无权限");
        }

        Long postId = params.get("postId");
        if (postId == null) {
            return Result.error(400, "帖子ID不能为空");
        }

        Post post = postService.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 切换置顶状态
        int newTop = (post.getIsTop() != null && post.getIsTop() == 1) ? 0 : 1;
        post.setIsTop(newTop);
        postService.updateById(post);

        return Result.success(newTop == 1 ? "已置顶" : "已取消置顶");
    }

    /**
     * 帖子列表（管理员/审核员）
     */
    @GetMapping("/posts")
    public Result<IPage<PostVO>> posts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Result.error(403, "无权限");
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
               .orderByDesc(Post::getIsTop)
               .orderByDesc(Post::getCreateTime);

        IPage<Post> postPage = postService.page(new Page<>(page, size), wrapper);

        List<Long> userIds = postPage.getRecords().stream()
                .map(Post::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        IPage<PostVO> voPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        List<PostVO> voList = postPage.getRecords().stream()
                .map(post -> {
                    PostVO vo = PostVO.fromPost(post);
                    User user = userMap.get(post.getUserId());
                    if (user != null) {
                        vo.setNickname(user.getNickname());
                        vo.setAvatar(AvatarUtil.fullUrl(user.getAvatar()));
                        int score = user.getActivityScore() != null ? user.getActivityScore() : 0;
                        vo.setActivityScore(score);
                        Map<String, Object> levelInfo = ActivityUtil.getLevel(score);
                        vo.setActivityLevel((String) levelInfo.get("level"));
                        vo.setActivityColor((String) levelInfo.get("color"));
                    } else {
                        vo.setNickname("匿名用户");
                        vo.setActivityScore(0);
                        vo.setActivityLevel("新手上路");
                        vo.setActivityColor("#999999");
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }
}
