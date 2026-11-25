package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArQuoteItem（表 ar_quote_item）
 */
@Data
@TableName("ar_quote_item")
public class ArQuoteItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报价单 */
    private Long quoteId;

    /** LABOR/PART */
    private String itemType;

    /** 对应工单项目或配件 */
    private Long refId;

    /** 名称 */
    private String itemName;

    /** 数量 */
    private BigDecimal qty;

    /** 单位 */
    private String unit;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 小计 */
    private BigDecimal amount;

    /** WAIT/AGREE/REJECT */
    private String confirmStatus;

    /** 排序 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createTime;

}