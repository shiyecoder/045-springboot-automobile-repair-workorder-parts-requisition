package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ArBatch（表 ar_batch）
 */
@Data
@TableName("ar_batch")
public class ArBatch {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配件 */
    private Long partId;

    /** 批次号 */
    private String batchNo;

    /** 仓库 */
    private String warehouse;

    /** 入库时间 */
    private LocalDateTime inTime;

    /** 入库数量 */
    private BigDecimal inQty;

    /** 剩余数量 */
    private BigDecimal leftQty;

    /** 采购价 */
    private BigDecimal purchasePrice;

    /** 供应商 */
    private String supplier;

    /** 有效期 */
    private LocalDate expireDate;

    /** IN_STOCK/LOW/EXPIRE/OUT */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

}