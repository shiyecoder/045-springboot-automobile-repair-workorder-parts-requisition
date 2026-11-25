package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ArConfirm（表 ar_confirm）
 */
@Data
@TableName("ar_confirm")
public class ArConfirm {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报价单 */
    private Long quoteId;

    /** 工单 */
    private Long orderId;

    /** 报价版本 */
    private Integer versionNo;

    /** PHONE/SMS/ONSITE/APP */
    private String confirmType;

    /** AGREE/PART/REJECT */
    private String confirmResult;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 确认人（客户或代登记人） */
    private String confirmBy;

    /** 说明 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

}