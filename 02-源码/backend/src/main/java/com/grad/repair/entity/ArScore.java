package com.grad.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ArScore（表 ar_score）
 */
@Data
@TableName("ar_score")
public class ArScore {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 快照编号 */
    private String scoreNo;

    /** 工单 */
    private Long orderId;

    /** 规则 */
    private Long ruleId;

    /** 规则版本 */
    private String ruleVersion;

    /** 输入快照 JSON */
    private String inputSnapshot;

    /** 车型得分 */
    private BigDecimal scoreModel;

    /** 故障标签得分 */
    private BigDecimal scoreFault;

    /** 诊断项目得分 */
    private BigDecimal scoreProject;

    /** 历史工时得分 */
    private BigDecimal scoreHours;

    /** 常用配件得分 */
    private BigDecimal scorePart;

    /** 当前库存得分 */
    private BigDecimal scoreStock;

    /** 总分 */
    private BigDecimal totalScore;

    /** RECOMMEND/CAUTION/REJECT */
    private String resultLevel;

    /** 结果解释 */
    private String explainText;

    /** 未满足条件 */
    private String missText;

    /** 可改进项 */
    private String improveText;

    /** 人工修正结果 */
    private String manualResult;

    /** 修正原因（≥5 字） */
    private String manualReason;

    /** 修正人 */
    private String manualBy;

    /** 修正时间 */
    private LocalDateTime manualTime;

    /** 生成时间 */
    private LocalDateTime createTime;

}