package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArReworkLog（表 ar_rework_log）
 */
@Data
@TableName("ar_rework_log")
public class ArReworkLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 质检记录 */
    private Long qcId;

    /** 工单项目 */
    private Long itemId;

    /** 责任技师 */
    private Long techId;

    /** 返工原因 */
    private String reason;

    /** 第几次返工 */
    private Integer times;

    /** 复检结果 */
    private String recheckResult;

    /** 复检时间 */
    private LocalDateTime recheckTime;

    /** 创建时间 */
    private LocalDateTime createTime;

}