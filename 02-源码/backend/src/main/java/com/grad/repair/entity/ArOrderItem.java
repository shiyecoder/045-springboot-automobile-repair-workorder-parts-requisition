package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArOrderItem（表 ar_order_item）
 */
@Data
@TableName("ar_order_item")
public class ArOrderItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 项目库 */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 工种 */
    private String workType;

    /** 工时 */
    private BigDecimal hours;

    /** 工时费 */
    private BigDecimal laborFee;

    /** 技师 */
    private Long techId;

    /** WAIT/WORKING/DONE/REJECTED/REWORK */
    private String itemStatus;

    /** 客户是否确认 1/0 */
    private Integer confirmed;

    /** 所属报价版本 */
    private Long quoteId;

    /** 创建时间 */
    private LocalDateTime createTime;

}