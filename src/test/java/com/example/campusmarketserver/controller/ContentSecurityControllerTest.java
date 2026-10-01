package com.example.campusmarketserver.controller;

import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.ErrandOrder;
import com.example.campusmarketserver.entity.Post;
import com.example.campusmarketserver.entity.SecondHandItem;
import com.example.campusmarketserver.service.ErrandOrderService;
import com.example.campusmarketserver.service.LikeService;
import com.example.campusmarketserver.service.NoticeService;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.SecondHandItemService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.vo.ErrandOrderVO;
import com.example.campusmarketserver.vo.SecondHandItemVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentSecurityControllerTest {

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void postCreateAlwaysUsesAuthenticatedUserAsAuthor() {
        PostService posts = mock(PostService.class);
        UserService users = mock(UserService.class);
        PostController controller = new PostController(posts, users, mock(LikeService.class), mock(NoticeService.class));
        UserContext.setUserId(7L);

        Post request = new Post();
        request.setUserId(99L);
        request.setTitle("title");
        request.setContent("content");

        assertThat(controller.create(request).getCode()).isEqualTo(200);
        assertThat(request.getUserId()).isEqualTo(7L);
        verify(posts).save(request);
        verify(users).addActivityScore(7L, 10);
    }

    @Test
    void postCreateRejectsMissingAuthenticatedUser() {
        PostService posts = mock(PostService.class);
        PostController controller = new PostController(posts, mock(UserService.class), mock(LikeService.class), mock(NoticeService.class));
        Post request = new Post();
        request.setUserId(99L);

        assertThat(controller.create(request).getCode()).isEqualTo(401);
        verifyNoInteractions(posts);
    }

    @Test
    void anonymousCannotReadPendingErrandAndApprovedContactIsMasked() {
        ErrandOrderService errands = mock(ErrandOrderService.class);
        ErrandOrderController controller = new ErrandOrderController(errands);
        ErrandOrder pending = errand(1L, 7L, ErrandOrder.AUDIT_PENDING);
        when(errands.getById(1L)).thenReturn(pending);

        assertThat(controller.detail(1L).getCode()).isEqualTo(404);
        verify(errands, never()).toViews(any());

        ErrandOrder approved = errand(2L, 7L, ErrandOrder.AUDIT_APPROVED);
        ErrandOrderVO view = new ErrandOrderVO();
        view.setContactPhone(approved.getContactPhone());
        when(errands.getById(2L)).thenReturn(approved);
        when(errands.toViews(any())).thenReturn(List.of(view));

        assertThat(controller.detail(2L).getCode()).isEqualTo(200);
        assertThat(controller.detail(2L).getData().getContactPhone()).isNull();
    }

    @Test
    void errandParticipantMayReadPendingOrderAndContact() {
        ErrandOrderService errands = mock(ErrandOrderService.class);
        ErrandOrderController controller = new ErrandOrderController(errands);
        ErrandOrder pending = errand(1L, 7L, ErrandOrder.AUDIT_PENDING);
        ErrandOrderVO view = new ErrandOrderVO();
        view.setContactPhone(pending.getContactPhone());
        when(errands.getById(1L)).thenReturn(pending);
        when(errands.toViews(any())).thenReturn(List.of(view));
        UserContext.setUserId(7L);

        assertThat(controller.detail(1L).getCode()).isEqualTo(200);
        assertThat(controller.detail(1L).getData().getContactPhone()).isEqualTo("13800000000");
    }

    @Test
    void anonymousCannotReadPendingSecondHandItem() {
        SecondHandItemService items = mock(SecondHandItemService.class);
        SecondHandItemController controller = new SecondHandItemController(items, mock(UserService.class));
        SecondHandItem pending = item(1L, 7L, 0);
        when(items.getById(1L)).thenReturn(pending);

        assertThat(controller.detail(1L).getCode()).isEqualTo(404);
        verify(items).getById(1L);
    }

    @Test
    void itemOwnerMayReadPendingSecondHandItem() {
        SecondHandItemService items = mock(SecondHandItemService.class);
        UserService users = mock(UserService.class);
        SecondHandItemController controller = new SecondHandItemController(items, users);
        SecondHandItem pending = item(1L, 7L, 0);
        when(items.getById(1L)).thenReturn(pending);
        when(users.listByIds(any())).thenReturn(Collections.emptyList());
        UserContext.setUserId(7L);

        assertThat(controller.detail(1L).getCode()).isEqualTo(200);
    }

    private static ErrandOrder errand(Long id, Long publisherId, int auditStatus) {
        ErrandOrder order = new ErrandOrder();
        order.setId(id);
        order.setPublisherId(publisherId);
        order.setAuditStatus(auditStatus);
        order.setContactPhone("13800000000");
        return order;
    }

    private static SecondHandItem item(Long id, Long userId, int auditStatus) {
        SecondHandItem item = new SecondHandItem();
        item.setId(id);
        item.setUserId(userId);
        item.setAuditStatus(auditStatus);
        return item;
    }
}
