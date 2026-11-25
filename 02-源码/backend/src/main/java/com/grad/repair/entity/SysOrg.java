package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysOrg（表 sys_org）
 */
@Data
@TableName("sys_org")
public class SysOrg {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门店名称 */
    private String orgName;

    /** 门店编码 */
    private String orgCode;

    /** 城市 */
    private String city;

    /** 地址 */
    private String address;

    /** 联系电话 */
    private String phone;

    /** 营业开始 */
    private String openTime;

    /** 营业结束 */
    private String closeTime;

    /** 交通指引 */
    private String traffic;

    /** 服务须知 */
    private String notice;

    /** 门店图片 */
    private String cover;

    /** 负责人 */
    private String manager;

    /** 1 营业 0 停业 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

}