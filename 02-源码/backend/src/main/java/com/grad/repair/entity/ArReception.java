package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArReception（表 ar_reception）
 */
@Data
@TableName("ar_reception")
public class ArReception {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 进厂里程 */
    private Integer mileage;

    /** 油量 */
    private String fuelLevel;

    /** 外观划痕 */
    private String scratchDesc;

    /** 随车物品 */
    private String itemsDesc;

    /** 现场照片，逗号分隔相对路径 */
    private String photos;

    /** 接车时间 */
    private LocalDateTime receiveTime;

    /** 服务顾问 */
    private Long advisorId;

    /** 创建时间 */
    private LocalDateTime createTime;

}