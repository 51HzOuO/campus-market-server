package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.Like;
import com.example.campusmarketserver.entity.Notice;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.LikeService;
import com.example.campusmarketserver.service.NoticeService;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.ActivityUtil;
import com.example.campusmarketserver.util.AvatarUtil;
import com.example.campusmarketserver.util.SensitiveWordUtil;
import com.example.campusmarketserver.vo.PostVO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/post")
public class PostController {

    private final PostService postService;
    private final UserService userService;
    private final LikeService likeService;
    private final NoticeService noticeService;

    public PostController(PostService postService, UserService userService, LikeService likeService, NoticeService noticeService) {
        this.postService = postService;
        this.userService = userService;
        this.likeService = likeService;
        this.noticeService = noticeService;
    }

    /**
     * 分页查询帖子列表
     */
    @GetMapping("/list")
    public Result<IPage<PostVO>> list(
            @RequestParam(required = false) Integer module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<PostVO> result = postService.listPosts(page, size, module);
        return Result.success(result);
    }

    /**
     * 搜索帖子（根据关键词模糊匹配标题和内容）
     */
    @GetMapping("/search")
    public Result<IPage<PostVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error(400, "关键词不能为空");
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
               .and(w -> w.like(Post::getTitle, keyword)
                          .or()
                          .like(Post::getContent, keyword))
               .orderByDesc(Post::getCreateTime);

        IPage<Post> postPage = postService.page(new Page<>(page, size), wrapper);

        // 批量查询用户信息
        List<Long> userIds = postPage.getRecords().stream()
                .map(Post::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为 PostVO
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

    /**
     * 发布帖子
     */
    @PostMapping("/create")
    public Result<Post> create(@RequestBody Post post) {
        // 敏感词检测
        String checkContent = (post.getTitle() != null ? post.getTitle() : "")
                + (post.getContent() != null ? post.getContent() : "");
        List<String> hits = SensitiveWordUtil.detect(checkContent);
        if (!hits.isEmpty()) {
            return Result.error(400, "内容包含敏感词：" + String.join("、", hits) + "，请修改后重新发布");
        }

        Long currentUserId = UserContext.getUserId();
        if (post.getUserId() == null) {
            post.setUserId(currentUserId);
        }

        // 设置默认值
        if (post.getLikeCount() == null) post.setLikeCount(0);
        if (post.getCommentCount() == null) post.setCommentCount(0);
        if (post.getViewCount() == null) post.setViewCount(0);
        if (post.getIsTop() == null) post.setIsTop(0);
        if (post.getStatus() == null) post.setStatus(1);

        post.setCreateTime(LocalDateTime.now());
        postService.save(post);

        // 发帖成功，活跃度 +10
        userService.addActivityScore(post.getUserId(), 10);

        return Result.success(post);
    }

    /**
     * 帖子详情（带用户信息和点赞状态）
     */
    @GetMapping("/detail/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        Post post = postService.getById(id);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        PostVO vo = PostVO.fromPost(post);

        // 填充用户信息
        User user = userService.getById(post.getUserId());
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
            vo.setActivityLevel("新手上路");
            vo.setActivityColor("#999999");
        }

        // 查询当前用户是否已点赞
        Long currentUserId = UserContext.getUserId();
        LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(Like::getPostId, id);
        if (currentUserId != null) {
            likeWrapper.eq(Like::getUserId, currentUserId);
        }
        boolean liked = currentUserId != null && likeService.count(likeWrapper) > 0;
        vo.setIsLiked(liked);

        return Result.success(vo);
    }

    /**
     * 查询当前用户发布的帖子
     */
    @GetMapping("/my")
    public Result<List<PostVO>> myPosts() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId)
               .orderByDesc(Post::getCreateTime);
        List<Post> posts = postService.list(wrapper);

        List<PostVO> voList = posts.stream().map(post -> {
            PostVO vo = PostVO.fromPost(post);
            vo.setIsLiked(false);
            return vo;
        }).collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 查询当前用户点赞过的帖子
     */
    @GetMapping("/liked")
    public Result<List<PostVO>> likedPosts() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        // 先查出用户点赞的所有 postId
        LambdaQueryWrapper<Like> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(Like::getUserId, userId)
                   .orderByDesc(Like::getCreateTime);
        List<Like> likes = likeService.list(likeWrapper);

        if (likes.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Long> postIds = likes.stream()
                .map(Like::getPostId)
                .collect(Collectors.toList());

        // 批量查询帖子
        List<Post> posts = postService.listByIds(postIds);
        // 按点赞顺序排列
        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));
        List<PostVO> voList = new ArrayList<>();
        for (Long postId : postIds) {
            Post post = postMap.get(postId);
            if (post != null) {
                PostVO vo = PostVO.fromPost(post);
                vo.setIsLiked(true);
                voList.add(vo);
            }
        }

        return Result.success(voList);
    }

    /**
     * 点赞/取消点赞（切换）
     */
    @PostMapping("/like")
    public Result<Map<String, Object>> toggleLike(@RequestBody Map<String, Long> params) {
        Long postId = params.get("postId");
        if (postId == null) {
            return Result.error(400, "帖子ID不能为空");
        }

        Post post = postService.getById(postId);
        if (post == null) {
            return Result.error(404, "帖子不存在");
        }

        // 查询是否已点赞
        Long currentUserId = UserContext.getUserId();
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getPostId, postId)
               .eq(Like::getUserId, currentUserId);
        Like existingLike = likeService.getOne(wrapper);

        Map<String, Object> result = new HashMap<>();

        if (existingLike != null) {
            // 已点赞 → 取消点赞
            likeService.removeById(existingLike.getId());
            int newCount = Math.max((post.getLikeCount() != null ? post.getLikeCount() : 0) - 1, 0);
            post.setLikeCount(newCount);
            postService.updateById(post);
            result.put("liked", false);
            result.put("likeCount", newCount);
        } else {
            // 未点赞 → 点赞
            Like like = new Like();
            like.setPostId(postId);
            like.setUserId(currentUserId);
            like.setCreateTime(LocalDateTime.now());
            likeService.save(like);
            int newCount = (post.getLikeCount() != null ? post.getLikeCount() : 0) + 1;
            post.setLikeCount(newCount);
            postService.updateById(post);

            // 点赞人活跃度 +2
            userService.addActivityScore(currentUserId, 2);

            // 给帖子作者发通知（不能通知自己）
            if (!currentUserId.equals(post.getUserId())) {
                User fromUser = userService.getById(currentUserId);
                String fromName = fromUser != null ? fromUser.getNickname() : "匿名用户";
                String postTitle = post.getTitle() != null && post.getTitle().length() > 10
                        ? post.getTitle().substring(0, 10) + "..." : post.getTitle();

                Notice notice = new Notice();
                notice.setUserId(post.getUserId());
                notice.setFromUserId(currentUserId);
                notice.setPostId(postId);
                notice.setType("like");
                notice.setContent(fromName + " 赞了你的帖子「" + postTitle + "」");
                notice.setIsRead(0);
                notice.setCreateTime(LocalDateTime.now());
                noticeService.save(notice);
            }

            result.put("liked", true);
            result.put("likeCount", newCount);
        }

        return Result.success(result);
    }

    /**
     * 删除帖子（管理员/审核员）
     */
    @PostMapping("/delete")
    public Result<String> delete(@RequestBody Map<String, Long> params) {
        Long currentUserId = UserContext.getUserId();
        User currentUser = userService.getById(currentUserId);
        if (currentUser == null || currentUser.getRole() == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
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

        postService.removeById(postId);
        return Result.success("已删除");
    }

    /**
     * 24小时火文（按点赞数倒序前10）
     */
    @GetMapping("/hot")
    public Result<List<Map<String, Object>>> hot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusHours(24);

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
               .ge(Post::getCreateTime, yesterday)
               .orderByDesc(Post::getLikeCount)
               .last("LIMIT 10");

        List<Post> posts = postService.list(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("id", post.getId());
            item.put("title", post.getTitle() != null ? post.getTitle() : "无标题");
            item.put("likeCount", post.getLikeCount() != null ? post.getLikeCount() : 0);
            item.put("rank", i + 1);
            result.add(item);
        }

        return Result.success(result);
    }
}
