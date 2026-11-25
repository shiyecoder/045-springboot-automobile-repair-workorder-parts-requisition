package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArRule（表 ar_rule）
 */
@Data
@TableName("ar_rule")
public class ArRule {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 版本号 */
    private String ruleVersion;

    /** 车型权重 */
    private BigDecimal wModel;

    /** 故障标签权重 */
    private BigDecimal wFault;

    /** 诊断项目权重 */
    private BigDecimal wProject;

    /** 历史工时权重 */
    private BigDecimal wHours;

    /** 常用配件权重 */
    private BigDecimal wPart;

    /** 当前库存权重 */
    private BigDecimal wStock;

    /** 推荐阈值 */
    private BigDecimal thresholdHigh;

    /** 不推荐阈值 */
    private BigDecimal thresholdLow;

    /** 1 生效 0 历史 */
    private Integer status;

    /** 生效时间 */
    private LocalDateTime effectTime;

    /** 修改人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

}