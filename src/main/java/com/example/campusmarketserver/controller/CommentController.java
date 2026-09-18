package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.Comment;
import com.example.campusmarketserver.entity.Like;
import com.example.campusmarketserver.entity.Notice;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.CommentService;
import com.example.campusmarketserver.service.LikeService;
import com.example.campusmarketserver.service.NoticeService;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.ActivityUtil;
import com.example.campusmarketserver.util.AvatarUtil;
import com.example.campusmarketserver.vo.CommentVO;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;
    private final NoticeService noticeService;

    public CommentController(CommentService commentService, PostService postService, UserService userService, NoticeService noticeService) {
        this.commentService = commentService;
        this.postService = postService;
        this.userService = userService;
        this.noticeService = noticeService;
    }

    /**
     * 获取帖子的评论列表
     */
    @GetMapping("/list")
    public Result<List<CommentVO>> list(@RequestParam Long postId) {
        // 查询评论
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
               .orderByAsc(Comment::getCreateTime);
        List<Comment> comments = commentService.list(wrapper);

        if (comments.isEmpty()) {
            return Result.success(List.of());
        }

        // 批量查询用户信息
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为 VO
        List<CommentVO> voList = comments.stream()
                .map(comment -> {
                    CommentVO vo = new CommentVO();
                    vo.setId(comment.getId());
                    vo.setPostId(comment.getPostId());
                    vo.setUserId(comment.getUserId());
                    vo.setContent(comment.getContent());
                    vo.setCreateTimeFromLocalDateTime(comment.getCreateTime());

                    User user = userMap.get(comment.getUserId());
                    if (user != null) {
                        vo.setNickname(user.getNickname());
                        vo.setAvatar(AvatarUtil.fullUrl(user.getAvatar()));
                        int score = user.getActivityScore() != null ? user.getActivityScore() : 0;
                        Map<String, Object> levelInfo = ActivityUtil.getLevel(score);
                        vo.setActivityLevel((String) levelInfo.get("level"));
                        vo.setActivityColor((String) levelInfo.get("color"));
                    } else {
                        vo.setNickname("匿名用户");
                        vo.setActivityLevel("新手上路");
                        vo.setActivityColor("#999999");
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 发表评论
     */
    @PostMapping("/add")
    public Result<Comment> add(@RequestBody Comment comment) {
        if (comment.getPostId() == null) {
            return Result.error(400, "帖子ID不能为空");
        }
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return Result.error(400, "评论内容不能为空");
        }

        // 从拦截器获取当前用户 ID
        Long currentUserId = UserContext.getUserId();
        if (comment.getUserId() == null) {
            comment.setUserId(currentUserId);
        }

        comment.setCreateTime(LocalDateTime.now());
        commentService.save(comment);

        // 帖子评论数 +1
        Post post = postService.getById(comment.getPostId());
        if (post != null) {
            int newCount = (post.getCommentCount() != null ? post.getCommentCount() : 0) + 1;
            post.setCommentCount(newCount);
            postService.updateById(post);
        }

        // 评论人活跃度 +5
        userService.addActivityScore(comment.getUserId(), 5);

        // 给帖子作者发通知（不能通知自己）
        if (post != null && !comment.getUserId().equals(post.getUserId())) {
            User fromUser = userService.getById(comment.getUserId());
            String fromName = fromUser != null ? fromUser.getNickname() : "匿名用户";
            String postTitle = post.getTitle() != null && post.getTitle().length() > 10
                    ? post.getTitle().substring(0, 10) + "..." : post.getTitle();
            String commentPreview = comment.getContent().length() > 20
                    ? comment.getContent().substring(0, 20) + "..." : comment.getContent();

            Notice notice = new Notice();
            notice.setUserId(post.getUserId());
            notice.setFromUserId(comment.getUserId());
            notice.setPostId(post.getId());
            notice.setType("comment");
            notice.setContent(fromName + " 评论了你的帖子「" + postTitle + "」：" + commentPreview);
            notice.setIsRead(0);
            notice.setCreateTime(LocalDateTime.now());
            noticeService.save(notice);
        }

        return Result.success(comment);
    }
}
