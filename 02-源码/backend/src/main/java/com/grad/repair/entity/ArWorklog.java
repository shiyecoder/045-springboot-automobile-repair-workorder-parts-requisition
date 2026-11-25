package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArWorklog（表 ar_worklog）
 */
@Data
@TableName("ar_worklog")
public class ArWorklog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 派工单 */
    private Long dispatchId;

    /** 工单 */
    private Long orderId;

    /** 技师 */
    private Long techId;

    /** START/PAUSE/RESUME/FINISH */
    private String action;

    /** 操作时间 */
    private LocalDateTime logTime;

    /** 本次工时 */
    private BigDecimal hours;

    /** 维修说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

}