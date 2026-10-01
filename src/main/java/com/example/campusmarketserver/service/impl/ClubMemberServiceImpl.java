package com.example.campusmarketserver.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.example.campusmarketserver.entity.ClubMember;
import com.example.campusmarketserver.mapper.ClubMemberMapper;
import com.example.campusmarketserver.service.ClubMemberService;
import org.springframework.stereotype.Service;

@Service
public class ClubMemberServiceImpl extends ServiceImpl<ClubMemberMapper, ClubMember> implements ClubMemberService {
}
