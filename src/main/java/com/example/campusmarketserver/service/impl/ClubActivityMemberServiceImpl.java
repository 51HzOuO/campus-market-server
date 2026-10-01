package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.entity.ClubActivityMember;
import com.example.campusmarketserver.mapper.ClubActivityMemberMapper;
import com.example.campusmarketserver.service.ClubActivityMemberService;
import org.springframework.stereotype.Service;

@Service
public class ClubActivityMemberServiceImpl extends ServiceImpl<ClubActivityMemberMapper, ClubActivityMember> implements ClubActivityMemberService {
}
