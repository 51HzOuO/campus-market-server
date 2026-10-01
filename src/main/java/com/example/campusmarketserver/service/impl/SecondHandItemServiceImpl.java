package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.entity.SecondHandItem;
import com.example.campusmarketserver.mapper.SecondHandItemMapper;
import com.example.campusmarketserver.service.SecondHandItemService;
import org.springframework.stereotype.Service;

@Service
public class SecondHandItemServiceImpl extends ServiceImpl<SecondHandItemMapper, SecondHandItem>
        implements SecondHandItemService {
}
