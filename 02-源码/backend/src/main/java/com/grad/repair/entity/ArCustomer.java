package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArCustomer（表 ar_customer）
 */
@Data
@TableName("ar_customer")
public class ArCustomer {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户编号 */
    private String customerNo;

    /** 客户姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 客户类型 MEMBER/NORMAL */
    private String customerType;

    /** 客户来源 */
    private String source;

    /** 进厂次数 */
    private Integer visitCount;

    /** 累计消费 */
    private BigDecimal totalAmount;

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