package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArDelivery（表 ar_delivery）
 */
@Data
@TableName("ar_delivery")
public class ArDelivery {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** 交车里程 */
    private Integer mileage;

    /** 客户验收结果 */
    private String acceptResult;

    /** 旧件是否领取 */
    private String oldPartTaken;

    /** 交车凭证相对路径 */
    private String voucher;

    /** 交车时间 */
    private LocalDateTime deliveryTime;

    /** 经办人 */
    private Long operatorId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

}