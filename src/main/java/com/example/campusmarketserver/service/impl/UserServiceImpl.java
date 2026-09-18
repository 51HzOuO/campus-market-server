package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.mapper.UserMapper;
import com.example.campusmarketserver.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public void addActivityScore(Long userId, int score) {
        if (userId == null) return;

        User user = getById(userId);
        if (user == null) return;

        int currentScore = user.getActivityScore() != null ? user.getActivityScore() : 0;
        int newScore = currentScore + score;

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
               .set(User::getActivityScore, newScore);
        update(wrapper);
    }

    @Override
    public User getByToken(String token) {
        if (token == null || token.isEmpty()) return null;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getToken, token);
        return getOne(wrapper);
    }

    @Override
    public User getByOpenid(String openid) {
        if (openid == null || openid.isEmpty()) return null;
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getOpenid, openid);
        return getOne(wrapper);
    }
}
