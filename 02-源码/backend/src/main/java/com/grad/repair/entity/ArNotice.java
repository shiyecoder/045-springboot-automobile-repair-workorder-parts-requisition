package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArNotice（表 ar_notice）
 */
@Data
@TableName("ar_notice")
public class ArNotice {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 发布人 */
    private String publisher;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 1 发布 0 下线 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

}