package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * SysRoleMenu（表 sys_role_menu）
 */
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色 */
    private Long roleId;

    /** 菜单 */
    private Long menuId;

}