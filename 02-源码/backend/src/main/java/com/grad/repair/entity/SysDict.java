package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * SysDict（表 sys_dict）
 */
@Data
@TableName("sys_dict")
public class SysDict {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 显示值 */
    private String dictLabel;

    /** 存储值 */
    private String dictValue;

    /** 排序 */
    private Integer sort;

}