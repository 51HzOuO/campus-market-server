package com.example.campusmarketserver.controller;

import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.LikeService;
import com.example.campusmarketserver.service.CommentService;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.ActivityUtil;
import com.example.campusmarketserver.util.AvatarUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final PostService postService;
    private final LikeService likeService;
    private final CommentService commentService;

    public UserController(UserService userService, PostService postService, LikeService likeService, CommentService commentService) {
        this.userService = userService;
        this.postService = postService;
        this.likeService = likeService;
        this.commentService = commentService;
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        int score = user.getActivityScore() != null ? user.getActivityScore() : 0;
        Map<String, Object> levelInfo = ActivityUtil.getLevel(score);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("avatar", AvatarUtil.fullUrl(user.getAvatar()));
        data.put("studentNo", user.getStudentNo());
        data.put("college", user.getCollege());
        data.put("activityScore", score);
        data.put("activityLevel", levelInfo.get("level"));
        data.put("activityColor", levelInfo.get("color"));
        data.put("role", user.getRole() != null ? user.getRole() : 0);

        // 统计发帖数、总点赞数、总评论数
        LambdaQueryWrapper<Post> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(Post::getUserId, userId);
        List<Post> userPosts = postService.list(postWrapper);
        data.put("postCount", userPosts.size());

        long totalLikes = userPosts.stream()
                .mapToLong(p -> p.getLikeCount() != null ? p.getLikeCount() : 0)
                .sum();
        long totalComments = userPosts.stream()
                .mapToLong(p -> p.getCommentCount() != null ? p.getCommentCount() : 0)
                .sum();
        data.put("likeCount", totalLikes);
        data.put("commentCount", totalComments);

        return Result.success(data);
    }

    /**
     * 更新用户信息（支持 nickname、avatar、studentNo，传哪个更新哪个）
     */
    @PostMapping("/update")
    public Result<String> update(@RequestBody Map<String, String> params) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        User user = new User();
        user.setId(userId);
        boolean hasUpdate = false;

        // 更新昵称
        if (params.containsKey("nickname")) {
            String nickname = params.get("nickname");
            if (nickname != null && !nickname.trim().isEmpty()) {
                if (nickname.trim().length() > 20) {
                    return Result.error(400, "昵称不能超过20个字");
                }
                user.setNickname(nickname.trim());
                hasUpdate = true;
            }
        }

        // 更新头像 URL
        if (params.containsKey("avatar")) {
            String avatar = params.get("avatar");
            if (avatar != null && !avatar.trim().isEmpty()) {
                user.setAvatar(avatar.trim());
                hasUpdate = true;
            }
        }

        // 更新学号
        if (params.containsKey("studentNo")) {
            String studentNo = params.get("studentNo");
            if (studentNo != null) {
                if (studentNo.trim().length() > 20) {
                    return Result.error(400, "学号不能超过20位");
                }
                user.setStudentNo(studentNo.trim());
                hasUpdate = true;
            }
        }

        if (!hasUpdate) {
            return Result.error(400, "没有需要更新的内容");
        }

        userService.updateById(user);
        return Result.success("更新成功");
    }

    /**
     * 上传头像图片
     */
    @PostMapping("/upload-avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            return Result.error(400, "仅支持 jpg/png/gif/webp 格式的图片");
        }

        // 生成唯一文件名，保存到 static/avatars/ 目录
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String dir = System.getProperty("user.dir") + "/src/main/resources/static/avatars";
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            return Result.error(500, "头像上传失败");
        }

        // 返回可访问的 URL
        String avatarUrl = "/avatars/" + fileName;

        // 同时更新 user 表的 avatar 字段
        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        userService.updateById(user);

        Map<String, String> result = new HashMap<>();
        result.put("url", avatarUrl);
        return Result.success(result);
    }
}
