package com.example.campusmarketserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campusmarketserver.entity.ClubMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClubMemberMapper extends BaseMapper<ClubMember> {
}
