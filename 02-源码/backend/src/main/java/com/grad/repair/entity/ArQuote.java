package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArQuote（表 ar_quote）
 */
@Data
@TableName("ar_quote")
public class ArQuote {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报价单号 */
    private String quoteNo;

    /** 工单 */
    private Long orderId;

    /** 版本号 */
    private Integer versionNo;

    /** 工时费合计 */
    private BigDecimal totalLabor;

    /** 配件费合计 */
    private BigDecimal totalPart;

    /** 优惠 */
    private BigDecimal discount;

    /** 预计总额 */
    private BigDecimal totalAmount;

    /** 预计工期（天） */
    private Integer expectDays;

    /** WAIT_CONFIRM/CONFIRMED/REJECTED/EXPIRED */
    private String status;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

}