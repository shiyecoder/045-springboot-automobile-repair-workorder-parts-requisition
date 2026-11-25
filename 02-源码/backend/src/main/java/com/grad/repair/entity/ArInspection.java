package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArInspection（表 ar_inspection）
 */
@Data
@TableName("ar_inspection")
public class ArInspection {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 检查项 */
    private String checkItem;

    /** NORMAL/OBSERVE/REPAIR */
    private String checkResult;

    /** 故障描述 */
    private String faultDesc;

    /** HIGH/MID/LOW */
    private String severity;

    /** 建议 */
    private String suggestion;

    /** 技师 */
    private Long techId;

    /** 检测时间 */
    private LocalDateTime checkTime;

    /** 创建时间 */
    private LocalDateTime createTime;

}