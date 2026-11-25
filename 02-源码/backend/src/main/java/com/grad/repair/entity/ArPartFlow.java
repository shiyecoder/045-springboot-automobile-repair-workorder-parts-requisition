package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArPartFlow（表 ar_part_flow）
 */
@Data
@TableName("ar_part_flow")
public class ArPartFlow {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 领用单号 */
    private String flowNo;

    /** 关联工单（必填） */
    private Long orderId;

    /** 配件 */
    private Long partId;

    /** 批次 */
    private Long batchId;

    /** 数量（正数） */
    private BigDecimal qty;

    /** CLAIM/EXTRA/RETURN */
    private String flowType;

    /** 领用技师 */
    private Long techId;

    /** 旧件处理方式 */
    private String oldPartHandle;

    /** 仓库操作人 */
    private Long operatorId;

    /** 业务时间 */
    private LocalDateTime flowTime;

    /** WAIT/ISSUED/REJECTED/RETURNED */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

}