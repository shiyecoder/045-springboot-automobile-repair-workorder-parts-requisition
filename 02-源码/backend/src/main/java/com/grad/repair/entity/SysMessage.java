package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * SysMessage（表 sys_message）
 */
@Data
@TableName("sys_message")
public class SysMessage {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 */
    private Long receiverId;

    /** 消息类型 */
    private String msgType;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 业务编号 */
    private String bizNo;

    /** 跳转地址 */
    private String link;

    /** 是否已读 */
    private Integer isRead;

    /** 创建时间 */
    private LocalDateTime createTime;

}