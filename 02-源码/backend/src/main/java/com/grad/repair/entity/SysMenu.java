package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * SysMenu（表 sys_menu）
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父菜单 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 前端路由 */
    private String path;

    /** 图标 */
    private String icon;

    /** catalog/menu/button */
    private String menuType;

    /** 权限标识 */
    private String perm;

    /** 排序 */
    private Integer sort;

    /** 是否显示 */
    private Integer visible;

}