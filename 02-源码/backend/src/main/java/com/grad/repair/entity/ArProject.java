package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArProject（表 ar_project）
 */
@Data
@TableName("ar_project")
public class ArProject {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 工种 MECHANIC/ELECTRIC/SHEET_METAL/PAINT */
    private String workType;

    /** 标准工时 */
    private BigDecimal stdHours;

    /** 工时费 */
    private BigDecimal laborFee;

    /** 适用车型 */
    private String applyModels;

    /** 常用配件 */
    private String commonParts;

    /** 1 启用 0 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

}