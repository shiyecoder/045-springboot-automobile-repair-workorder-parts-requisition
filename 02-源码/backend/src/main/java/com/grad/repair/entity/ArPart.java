package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArPart（表 ar_part）
 */
@Data
@TableName("ar_part")
public class ArPart {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配件编码 */
    private String partCode;

    /** 配件名称 */
    private String partName;

    /** 适用车型 */
    private String applyModels;

    /** 单位 */
    private String unit;

    /** 采购价 */
    private BigDecimal purchasePrice;

    /** 销售价 */
    private BigDecimal salePrice;

    /** 当前库存 */
    private BigDecimal stockQty;

    /** 库存下限 */
    private BigDecimal minQty;

    /** 库存上限 */
    private BigDecimal maxQty;

    /** 配件图片 */
    private String image;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}