package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArAttachment（表 ar_attachment）
 */
@Data
@TableName("ar_attachment")
public class ArAttachment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型 vehicle/reception/inspection/voucher/avatar/part */
    private String bizType;

    /** 业务主键 */
    private Long bizId;

    /** 原始文件名 */
    private String fileName;

    /** 相对路径 */
    private String filePath;

    /** 大小（字节） */
    private Long fileSize;

    /** 上传人 */
    private String uploadBy;

    /** 创建时间 */
    private LocalDateTime createTime;

}