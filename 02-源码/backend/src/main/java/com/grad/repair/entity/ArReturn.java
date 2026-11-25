package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArReturn（表 ar_return）
 */
@Data
@TableName("ar_return")
public class ArReturn {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 返修单号 */
    private String returnNo;

    /** 原工单 */
    private Long orderId;

    /** 关联质保 */
    private Long warrantyId;

    /** 客户 */
    private Long customerId;

    /** 车辆 */
    private Long vehicleId;

    /** 原维修项目 */
    private String itemName;

    /** 问题描述 */
    private String problemDesc;

    /** STORE/PART/CUSTOMER/WAIT */
    private String dutyResult;

    /** WAIT/REPAIRING/DONE/CANCELED */
    private String status;

    /** 返修次数 */
    private Integer reworkTimes;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}