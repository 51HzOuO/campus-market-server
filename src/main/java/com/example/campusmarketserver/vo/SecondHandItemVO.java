package com.example.campusmarketserver.vo;

import com.example.campusmarketserver.entity.SecondHandItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 面向小程序的二手商品展示对象，包含发布人的公开资料。 */
@Data
public class SecondHandItemVO {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String images;
    private BigDecimal price;
    private String category;
    private String location;
    private String contact;
    private Integer status;
    private Integer auditStatus;
    private String auditRemark;
    private Long buyerId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private String sellerNickname;
    private String sellerAvatar;
    private String buyerNickname;

    public static SecondHandItemVO fromItem(SecondHandItem item) {
        SecondHandItemVO vo = new SecondHandItemVO();
        vo.setId(item.getId());
        vo.setUserId(item.getUserId());
        vo.setTitle(item.getTitle());
        vo.setDescription(item.getDescription());
        vo.setImages(item.getImages());
        vo.setPrice(item.getPrice());
        vo.setCategory(item.getCategory());
        vo.setLocation(item.getLocation());
        vo.setContact(item.getContact());
        vo.setStatus(item.getStatus());
        vo.setAuditStatus(item.getAuditStatus());
        vo.setAuditRemark(item.getAuditRemark());
        vo.setBuyerId(item.getBuyerId());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }
}
