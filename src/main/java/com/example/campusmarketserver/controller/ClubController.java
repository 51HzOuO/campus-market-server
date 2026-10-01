package com.example.campusmarketserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.*;
import com.example.campusmarketserver.service.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 社团、活动和成员关系接口。 */
@RestController
@RequestMapping("/club")
public class ClubController {
    private final ClubService clubService;
    private final ClubActivityService activityService;
    private final ClubMemberService memberService;
    private final ClubActivityMemberService activityMemberService;

    public ClubController(ClubService clubService, ClubActivityService activityService,
                          ClubMemberService memberService, ClubActivityMemberService activityMemberService) {
        this.clubService = clubService; this.activityService = activityService;
        this.memberService = memberService; this.activityMemberService = activityMemberService;
    }

    @GetMapping("/list")
    public Result<List<Club>> list() {
        return Result.success(clubService.list(new LambdaQueryWrapper<Club>()
                .eq(Club::getStatus, 1).orderByDesc(Club::getMemberCount).orderByDesc(Club::getCreateTime)));
    }

    @GetMapping("/detail/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Club club = clubService.getById(id);
        if (club == null || !Integer.valueOf(1).equals(club.getStatus())) return Result.error(404, "社团不存在");
        Map<String, Object> data = new HashMap<>(); data.put("club", club);
        List<ClubActivity> activities = activityService.list(new LambdaQueryWrapper<ClubActivity>()
                .eq(ClubActivity::getClubId, id).eq(ClubActivity::getStatus, 1)
                .orderByDesc(ClubActivity::getStartTime));
        Long userId = UserContext.getUserId();
        markJoined(activities, userId);
        data.put("activities", activities);
        data.put("joined", userId != null && isMember(id, userId));
        return Result.success(data);
    }

    @PostMapping("/create")
    public Result<Club> create(@RequestBody Club request) {
        Long userId = UserContext.getUserId();
        if (userId == null) return Result.error(401, "未登录");
        if (request == null || !StringUtils.hasText(request.getName()) || !StringUtils.hasText(request.getDescription())) {
            return Result.error(400, "社团名称和简介不能为空");
        }
        Club club = new Club(); club.setCreatorId(userId); club.setName(request.getName().trim());
        club.setDescription(request.getDescription().trim()); club.setLogo(request.getLogo()); club.setContact(request.getContact());
        club.setMemberCount(1); club.setStatus(0); club.setCreateTime(LocalDateTime.now()); club.setUpdateTime(LocalDateTime.now());
        clubService.save(club);
        ClubMember member = new ClubMember(); member.setClubId(club.getId()); member.setUserId(userId); member.setRole(1); member.setStatus(1); member.setCreateTime(LocalDateTime.now()); member.setUpdateTime(LocalDateTime.now()); memberService.save(member);
        return Result.success(club);
    }

    @GetMapping("/my")
    public Result<List<Club>> my() {
        Long userId = UserContext.getUserId(); if (userId == null) return Result.error(401, "未登录");
        List<ClubMember> memberships = memberService.list(new LambdaQueryWrapper<ClubMember>().eq(ClubMember::getUserId, userId).eq(ClubMember::getStatus, 1));
        if (memberships.isEmpty()) return Result.success(java.util.Collections.emptyList());
        return Result.success(clubService.listByIds(memberships.stream().map(ClubMember::getClubId).toList()));
    }

    @PostMapping("/join")
    public Result<String> join(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId(), clubId = longValue(body == null ? null : body.get("clubId"));
        if (userId == null) return Result.error(401, "未登录"); if (clubId == null) return Result.error(400, "社团ID不能为空");
        Club club = clubService.getById(clubId); if (club == null || !Integer.valueOf(1).equals(club.getStatus())) return Result.error(404, "社团不存在");
        ClubMember member = memberService.getOne(new LambdaQueryWrapper<ClubMember>().eq(ClubMember::getClubId, clubId).eq(ClubMember::getUserId, userId), false);
        if (member != null && Integer.valueOf(1).equals(member.getStatus())) return Result.error(400, "已经加入该社团");
        if (member == null) { member = new ClubMember(); member.setClubId(clubId); member.setUserId(userId); member.setRole(0); member.setCreateTime(LocalDateTime.now()); }
        member.setStatus(1); member.setUpdateTime(LocalDateTime.now()); memberService.saveOrUpdate(member);
        club.setMemberCount((club.getMemberCount() == null ? 0 : club.getMemberCount()) + 1); clubService.updateById(club);
        return Result.success("加入成功");
    }

    @PostMapping("/leave")
    public Result<String> leave(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId(), clubId = longValue(body == null ? null : body.get("clubId"));
        if (userId == null) return Result.error(401, "未登录"); if (clubId == null) return Result.error(400, "社团ID不能为空");
        ClubMember member = memberService.getOne(new LambdaQueryWrapper<ClubMember>().eq(ClubMember::getClubId, clubId).eq(ClubMember::getUserId, userId).eq(ClubMember::getStatus, 1), false);
        if (member == null) return Result.error(400, "你不是社团成员"); if (Integer.valueOf(1).equals(member.getRole())) return Result.error(400, "负责人不能退出社团");
        member.setStatus(0); member.setUpdateTime(LocalDateTime.now()); memberService.updateById(member);
        Club club = clubService.getById(clubId); if (club != null) { club.setMemberCount(Math.max(0, (club.getMemberCount() == null ? 1 : club.getMemberCount()) - 1)); clubService.updateById(club); }
        return Result.success("已退出");
    }

    @GetMapping("/activity/list")
    public Result<List<ClubActivity>> activities(@RequestParam Long clubId) {
        List<ClubActivity> activities = activityService.list(new LambdaQueryWrapper<ClubActivity>()
                .eq(ClubActivity::getClubId, clubId).eq(ClubActivity::getStatus, 1)
                .orderByAsc(ClubActivity::getStartTime));
        markJoined(activities, UserContext.getUserId());
        return Result.success(activities);
    }

    @GetMapping("/activity/detail/{id}")
    public Result<ClubActivity> activityDetail(@PathVariable Long id) {
        ClubActivity activity = activityService.getById(id); if (activity == null || !Integer.valueOf(1).equals(activity.getStatus())) return Result.error(404, "活动不存在"); return Result.success(activity);
    }

    @PostMapping("/activity/create")
    public Result<ClubActivity> createActivity(@RequestBody ClubActivity request) {
        Long userId = UserContext.getUserId(); if (userId == null) return Result.error(401, "未登录");
        if (request == null || request.getClubId() == null || !StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent()) || !StringUtils.hasText(request.getLocation()) || request.getStartTime() == null) return Result.error(400, "活动信息不完整");
        ClubMember member = memberService.getOne(new LambdaQueryWrapper<ClubMember>().eq(ClubMember::getClubId, request.getClubId()).eq(ClubMember::getUserId, userId).eq(ClubMember::getStatus, 1), false);
        if (member == null || !Integer.valueOf(1).equals(member.getRole())) return Result.error(403, "只有社团负责人可以发布活动");
        ClubActivity activity = new ClubActivity(); activity.setClubId(request.getClubId()); activity.setCreatorId(userId); activity.setTitle(request.getTitle().trim()); activity.setContent(request.getContent().trim()); activity.setStartTime(request.getStartTime()); activity.setEndTime(request.getEndTime()); activity.setLocation(request.getLocation().trim()); activity.setMaxParticipants(request.getMaxParticipants()); activity.setParticipantCount(0); activity.setStatus(0); activity.setCreateTime(LocalDateTime.now()); activity.setUpdateTime(LocalDateTime.now()); activityService.save(activity); return Result.success(activity);
    }

    @PostMapping("/activity/join")
    public Result<String> joinActivity(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId(), activityId = longValue(body == null ? null : body.get("activityId")); if (userId == null) return Result.error(401, "未登录"); if (activityId == null) return Result.error(400, "活动ID不能为空");
        ClubActivity activity = activityService.getById(activityId); if (activity == null || !Integer.valueOf(1).equals(activity.getStatus())) return Result.error(404, "活动不存在");
        if (activity.getMaxParticipants() != null && activity.getParticipantCount() != null && activity.getParticipantCount() >= activity.getMaxParticipants()) return Result.error(400, "活动报名已满");
        ClubActivityMember member = activityMemberService.getOne(new LambdaQueryWrapper<ClubActivityMember>().eq(ClubActivityMember::getActivityId, activityId).eq(ClubActivityMember::getUserId, userId), false);
        if (member != null && Integer.valueOf(1).equals(member.getStatus())) return Result.error(400, "已经报名");
        if (member == null) { member = new ClubActivityMember(); member.setActivityId(activityId); member.setUserId(userId); member.setCreateTime(LocalDateTime.now()); }
        member.setStatus(1); member.setUpdateTime(LocalDateTime.now()); activityMemberService.saveOrUpdate(member); activity.setParticipantCount((activity.getParticipantCount() == null ? 0 : activity.getParticipantCount()) + 1); activityService.updateById(activity); return Result.success("报名成功");
    }

    @PostMapping("/activity/leave")
    public Result<String> leaveActivity(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId(), activityId = longValue(body == null ? null : body.get("activityId")); if (userId == null) return Result.error(401, "未登录"); if (activityId == null) return Result.error(400, "活动ID不能为空");
        ClubActivityMember member = activityMemberService.getOne(new LambdaQueryWrapper<ClubActivityMember>().eq(ClubActivityMember::getActivityId, activityId).eq(ClubActivityMember::getUserId, userId).eq(ClubActivityMember::getStatus, 1), false); if (member == null) return Result.error(400, "你尚未报名"); member.setStatus(0); member.setUpdateTime(LocalDateTime.now()); activityMemberService.updateById(member); ClubActivity a = activityService.getById(activityId); if (a != null) { a.setParticipantCount(Math.max(0, (a.getParticipantCount() == null ? 1 : a.getParticipantCount()) - 1)); activityService.updateById(a); } return Result.success("已取消报名");
    }

    private boolean isMember(Long clubId, Long userId) { return memberService.count(new LambdaQueryWrapper<ClubMember>().eq(ClubMember::getClubId, clubId).eq(ClubMember::getUserId, userId).eq(ClubMember::getStatus, 1)) > 0; }

    private void markJoined(List<ClubActivity> activities, Long userId) {
        if (activities == null || activities.isEmpty()) return;
        if (userId == null) {
            activities.forEach(activity -> activity.setJoined(false));
            return;
        }
        List<Long> activityIds = activities.stream().map(ClubActivity::getId).toList();
        java.util.Set<Long> joinedIds = activityMemberService.list(new LambdaQueryWrapper<ClubActivityMember>()
                        .in(ClubActivityMember::getActivityId, activityIds)
                        .eq(ClubActivityMember::getUserId, userId)
                        .eq(ClubActivityMember::getStatus, 1))
                .stream().map(ClubActivityMember::getActivityId).collect(java.util.stream.Collectors.toSet());
        activities.forEach(activity -> activity.setJoined(joinedIds.contains(activity.getId())));
    }
    private static Long longValue(Object value) { try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (Exception e) { return null; } }
}
