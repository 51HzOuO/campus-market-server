package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.entity.ClubActivity;
import com.example.campusmarketserver.mapper.ClubActivityMapper;
import com.example.campusmarketserver.service.ClubActivityService;
import org.springframework.stereotype.Service;

@Service
public class ClubActivityServiceImpl extends ServiceImpl<ClubActivityMapper, ClubActivity> implements ClubActivityService {
}
