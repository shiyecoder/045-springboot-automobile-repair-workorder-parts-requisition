package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArFee（表 ar_fee）
 */
@Data
@TableName("ar_fee")
public class ArFee {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** LABOR/PART/EXTRA/DISCOUNT */
    private String feeType;

    /** 费用说明 */
    private String feeDesc;

    /** 金额 */
    private BigDecimal amount;

    /** 创建时间 */
    private LocalDateTime createTime;

}