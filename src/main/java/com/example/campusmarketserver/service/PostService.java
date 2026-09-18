package com.example.campusmarketserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.vo.PostVO;

public interface PostService extends IService<Post> {

    /**
     * 分页查询帖子列表（带用户活跃度信息）
     *
     * @param page   页码
     * @param size   每页数量
     * @param module 模块筛选（可选）
     * @return 分页结果
     */
    IPage<PostVO> listPosts(int page, int size, Integer module);
}
