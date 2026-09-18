package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.mapper.PostMapper;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.util.ActivityUtil;
import com.example.campusmarketserver.util.AvatarUtil;
import com.example.campusmarketserver.vo.PostVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private final UserService userService;

    public PostServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public IPage<PostVO> listPosts(int page, int size, Integer module) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        // 只查正常状态的帖子
        wrapper.eq(Post::getStatus, 1);

        // 按模块筛选
        if (module != null) {
            wrapper.eq(Post::getModule, module);
        }

        // 排序：置顶优先（is_top DESC），同是置顶的到期时间近的在前（top_expire_time ASC），
        // 其余按创建时间倒序（create_time DESC）
        wrapper.orderByDesc(Post::getIsTop)
               .orderByAsc(Post::getTopExpireTime)
               .orderByDesc(Post::getCreateTime);

        // 分页查询帖子
        IPage<Post> postPage = page(new Page<>(page, size), wrapper);

        // 收集所有用户ID，批量查询用户信息
        List<Long> userIds = postPage.getRecords().stream()
                .map(Post::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 批量获取用户信息
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 转换为 PostVO，填充用户信息和活跃度
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

        return voPage;
    }
}
