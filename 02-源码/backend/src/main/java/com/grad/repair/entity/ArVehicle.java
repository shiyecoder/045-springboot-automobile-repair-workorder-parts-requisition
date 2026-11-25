package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ArVehicle（表 ar_vehicle）
 */
@Data
@TableName("ar_vehicle")
public class ArVehicle {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车牌号 */
    private String plateNo;

    /** 所属客户 */
    private Long customerId;

    /** 车辆品牌 */
    private String brand;

    /** 车型 */
    private String model;

    /** 车架号 */
    private String vin;

    /** 发动机号 */
    private String engineNo;

    /** 当前里程 km */
    private Integer mileage;

    /** 购车日期 */
    private LocalDate buyDate;

    /** 车辆状态 NORMAL/IN_REPAIR/WAIT_SETTLE */
    private String vehicleStatus;

    /** 车辆照片 */
    private String photo;

    /** 备注 */
    private String remark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 乐观锁版本号 */
    private Integer version;

}