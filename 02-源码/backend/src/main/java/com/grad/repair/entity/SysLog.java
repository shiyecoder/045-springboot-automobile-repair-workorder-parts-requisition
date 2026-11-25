package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysLog（表 sys_log）
 */
@Data
@TableName("sys_log")
public class SysLog {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 */
    private String username;

    /** 操作 */
    private String action;

    /** 操作对象 */
    private String target;

    /** 明细 */
    private String detail;

    /** 来源 IP */
    private String ip;

    /** 时间 */
    private LocalDateTime createTime;

}