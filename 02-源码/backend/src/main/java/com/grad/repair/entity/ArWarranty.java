package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ArWarranty（表 ar_warranty）
 */
@Data
@TableName("ar_warranty")
public class ArWarranty {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 质保编号 */
    private String warrantyNo;

    /** 原工单 */
    private Long orderId;

    /** 原工单项目 */
    private Long itemId;

    /** 车辆 */
    private Long vehicleId;

    /** 维修项目 */
    private String projectName;

    /** 质保天数 */
    private Integer warrantyDays;

    /** 质保里程 */
    private Integer warrantyKm;

    /** 起算日期 */
    private LocalDate startDate;

    /** 到期日期 */
    private LocalDate endDate;

    /** VALID/EXPIRE/USED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

}