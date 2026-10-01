package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.entity.Club;
import com.example.campusmarketserver.mapper.ClubMapper;
import com.example.campusmarketserver.service.ClubService;
import org.springframework.stereotype.Service;

@Service
public class ClubServiceImpl extends ServiceImpl<ClubMapper, Club> implements ClubService {
}
