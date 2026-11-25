package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArOrder（表 ar_order）
 */
@Data
@TableName("ar_order")
public class ArOrder {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单号（后端生成） */
    private String orderNo;

    /** 车辆 */
    private Long vehicleId;

    /** 客户 */
    private Long customerId;

    /** 工单类型 */
    private String orderType;

    /** 接车登记/故障诊断/报价确认/维修领料/质量检验/交车结算 + REWORK/CANCELED */
    private String status;

    /** 服务顾问 */
    private Long advisorId;

    /** 主责技师 */
    private Long techId;

    /** 接车时间 */
    private LocalDateTime receiveTime;

    /** 预计完工 */
    private LocalDateTime expectFinish;

    /** 实际完工 */
    private LocalDateTime finishTime;

    /** 故障描述（≤100 字） */
    private String faultDesc;

    /** 紧急程度 */
    private String urgency;

    /** 应收金额 */
    private BigDecimal totalAmount;

    /** 已收金额 */
    private BigDecimal paidAmount;

    /** 优惠金额 */
    private BigDecimal discount;

    /** 返工次数 */
    private Integer reworkTimes;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    private Integer version;

}