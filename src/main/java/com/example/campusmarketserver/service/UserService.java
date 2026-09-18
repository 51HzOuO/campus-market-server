package com.example.campusmarketserver.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.example.campusmarketserver.entity.User;

public interface UserService extends IService<User> {

    /**
     * 增加用户活跃度分数
     */
    void addActivityScore(Long userId, int score);

    /**
     * 根据 token 查找用户
     */
    User getByToken(String token);

    /**
     * 根据 openid 查找用户
     */
    User getByOpenid(String openid);
}
