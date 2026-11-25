package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArPayment（表 ar_payment）
 */
@Data
@TableName("ar_payment")
public class ArPayment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单 */
    private Long orderId;

    /** CASH/WECHAT/ALIPAY/CARD */
    private String payType;

    /** 收款金额 */
    private BigDecimal amount;

    /** 收款时间 */
    private LocalDateTime payTime;

    /** 收款人 */
    private Long operatorId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

}