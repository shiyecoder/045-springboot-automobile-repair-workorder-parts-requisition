package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArDispatch（表 ar_dispatch）
 */
@Data
@TableName("ar_dispatch")
public class ArDispatch {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 工单项目 */
    private Long itemId;

    /** 技师 */
    private Long techId;

    /** 工种 */
    private String workType;

    /** 计划工时 */
    private BigDecimal planHours;

    /** 实际工时 */
    private BigDecimal actualHours;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 暂停时间 */
    private LocalDateTime pauseTime;

    /** 完成时间 */
    private LocalDateTime endTime;

    /** WAIT/WORKING/PAUSED/DONE */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

}