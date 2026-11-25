package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArDemand（表 ar_demand）
 */
@Data
@TableName("ar_demand")
public class ArDemand {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 配件 */
    private Long partId;

    /** 需求数量 */
    private BigDecimal demandQty;

    /** 计算时库存 */
    private BigDecimal stockQty;

    /** 缺口数量 */
    private BigDecimal gapQty;

    /** PRECHECK/MANUAL */
    private String source;

    /** 预检计算快照 */
    private Long scoreId;

    /** WAIT/RESERVED/CLAIMED/CANCELED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

}