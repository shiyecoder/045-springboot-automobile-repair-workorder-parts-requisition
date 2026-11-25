package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArQc（表 ar_qc）
 */
@Data
@TableName("ar_qc")
public class ArQc {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 工单项目 */
    private Long itemId;

    /** PASS/FAIL */
    private String qcResult;

    /** 检验说明 */
    private String qcDesc;

    /** 质检人员 */
    private Long inspectorId;

    /** 检验时间 */
    private LocalDateTime qcTime;

    /** DONE/REWORK */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

}