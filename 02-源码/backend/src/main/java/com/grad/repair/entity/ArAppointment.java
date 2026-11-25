package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArAppointment（表 ar_appointment）
 */
@Data
@TableName("ar_appointment")
public class ArAppointment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约号 */
    private String appointNo;

    /** 客户 */
    private Long customerId;

    /** 车辆 */
    private Long vehicleId;

    /** 预约类型 MAINTAIN/REPAIR/CHECK/SHEET_METAL */
    private String appointType;

    /** 期望时间 */
    private LocalDateTime expectTime;

    /** 问题类型 */
    private String problemType;

    /** WAIT_RECEIVE/RECEIVED/CANCELED/MISSED */
    private String status;

    /** 服务顾问 */
    private Long advisorId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}