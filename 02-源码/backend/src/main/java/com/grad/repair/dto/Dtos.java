package com.grad.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 请求体集中定义。使用 record 减少模板代码，字段校验交给 jakarta.validation。
 * 业务规则（权限、状态机、金额重算）一律在后端 Service 校验，不依赖前端按钮控制。
 */
public class Dtos {

    // ---------------- 认证与账号 ----------------
    public record LoginReq(@NotBlank(message = "登录名不能为空") String username,
                           @NotBlank(message = "密码不能为空") String password) {
    }

    public record PasswordReq(@NotBlank(message = "原密码不能为空") String oldPassword,
                              @NotBlank(message = "新密码不能为空")
                              @Size(min = 8, max = 20, message = "新密码长度 8-20 位") String newPassword) {
    }

    public record ProfileReq(@NotBlank(message = "姓名不能为空") String realName,
                             String phone, String email, String avatar) {
    }

    public record UserReq(Long id, @NotBlank(message = "登录名不能为空") String username,
                          String password, @NotBlank(message = "姓名不能为空") String realName,
                          String phone, String email, String avatar, String gender,
                          Long orgId, String orgName, Long roleId, List<Long> roleIds,
                          Integer status) {
    }

    public record RoleMenuReq(List<Long> menuIds) {
    }

    // ---------------- 客户与车辆 ----------------
    public record CustomerReq(Long id, @NotBlank(message = "客户姓名不能为空") String name,
                              @NotBlank(message = "手机号不能为空") String phone,
                              String customerType, String source, String remark) {
    }

    /** 车辆档案：车牌去除首尾空格后唯一；里程不得为负；图片只接受上传后的相对路径 */
    public record VehicleReq(Long id, @NotBlank(message = "车牌号不能为空") String plateNo,
                             @NotNull(message = "客户不能为空") Long customerId,
                             @NotBlank(message = "车辆品牌不能为空") String brand,
                             @NotBlank(message = "车型不能为空") String model,
                             @NotBlank(message = "车架号不能为空") String vin,
                             String engineNo, Integer mileage, String buyDate,
                             String vehicleStatus, String photo, String remark) {
    }

    // ---------------- 预约与接车 ----------------
    public record AppointmentReq(Long id, @NotNull(message = "车辆不能为空") Long vehicleId,
                                 String appointType, @NotBlank(message = "期望时间不能为空") String expectTime,
                                 String problemType, Long advisorId, String remark) {
    }

    /** 接车登记：进厂里程不得小于上次记录，现场照片至少 1 张 */
    public record ReceptionReq(@NotNull(message = "车辆不能为空") Long vehicleId,
                               String orderType, @NotNull(message = "进厂里程不能为空") Integer mileage,
                               String fuelLevel, String scratchDesc, String itemsDesc,
                               List<String> photos, String faultDesc,
                               String expectFinish, Long advisorId, Long appointmentId) {
    }

    // ---------------- 检测与项目 ----------------
    public record InspectionReq(@NotNull(message = "工单不能为空") Long orderId,
                                @NotBlank(message = "检查项不能为空") String checkItem,
                                @NotBlank(message = "检测结果不能为空") String checkResult,
                                String faultDesc, String severity, String suggestion,
                                String photo) {
    }

    public record ItemReq(Long id, @NotNull(message = "工单不能为空") Long orderId,
                          @NotNull(message = "项目不能为空") Long projectId,
                          Integer qty, BigDecimal hours, Long techId) {
    }

    // ---------------- 报价与确认 ----------------
    public record QuoteReq(@NotNull(message = "工单不能为空") Long orderId, BigDecimal discount,
                           Integer expectDays, String remark) {
    }

    public record ConfirmReq(@NotNull(message = "报价单不能为空") Long quoteId,
                             @NotBlank(message = "确认方式不能为空") String confirmType,
                             @NotBlank(message = "确认结果不能为空") String confirmResult,
                             String remark, List<Long> agreeItemIds, List<Long> rejectItemIds) {
    }

    // ---------------- 派工与工时 ----------------
    public record DispatchReq(@NotNull(message = "工单项目不能为空") Long itemId,
                              @NotNull(message = "技师不能为空") Long techId,
                              BigDecimal planHours) {
    }

    public record WorklogReq(@NotBlank(message = "动作不能为空") String action,
                             BigDecimal hours, String remark) {
    }

    // ---------------- 配件 ----------------
    public record PartReq(Long id, @NotBlank(message = "配件编码不能为空") String partCode,
                          @NotBlank(message = "配件名称不能为空") String partName,
                          String applyModels, String unit, BigDecimal purchasePrice,
                          BigDecimal salePrice, BigDecimal minQty, BigDecimal maxQty,
                          String image, Integer status) {
    }

    public record BatchReq(@NotNull(message = "配件不能为空") Long partId,
                           @NotBlank(message = "批次号不能为空") String batchNo,
                           String warehouse, BigDecimal inQty, BigDecimal purchasePrice,
                           String supplier, String expireDate) {
    }

    /** 领退：数量必须为正数，库存不足时后端拒绝；旧件处理方式入库 */
    public record PartFlowReq(@NotNull(message = "工单不能为空") Long orderId,
                              @NotNull(message = "配件不能为空") Long partId,
                              Long batchId, @NotNull(message = "数量不能为空") BigDecimal qty,
                              @NotBlank(message = "类型不能为空") String flowType,
                              Long techId, String oldPartHandle, String remark) {
    }

    public record DemandReq(@NotNull(message = "工单不能为空") Long orderId,
                            @NotNull(message = "配件不能为空") Long partId,
                            @NotNull(message = "数量不能为空") BigDecimal qty, String source) {
    }

    // ---------------- 质检与返工 ----------------
    public record QcReq(@NotNull(message = "工单不能为空") Long orderId,
                        @NotBlank(message = "检验结果不能为空") String qcResult,
                        String qcDesc, List<Long> failItemIds) {
    }

    public record ReworkReq(@NotNull(message = "工单不能为空") Long orderId, Long itemId,
                            @NotBlank(message = "返工原因不能为空")
                            @Size(min = 5, message = "原因不少于 5 个字") String reason) {
    }

    // ---------------- 结算与交车 ----------------
    public record PaymentReq(@NotNull(message = "工单不能为空") Long orderId,
                             @NotBlank(message = "收款方式不能为空") String payType,
                             @NotNull(message = "收款金额不能为空") BigDecimal amount,
                             String remark) {
    }

    public record DeliveryReq(@NotNull(message = "工单不能为空") Long orderId,
                              @NotNull(message = "交车里程不能为空") Integer mileage,
                              String acceptResult, String oldPartTaken, String voucher,
                              String remark) {
    }

    // ---------------- 返修与评价 ----------------
    public record ReturnReq(@NotNull(message = "原工单不能为空") Long orderId, Long warrantyId,
                            @NotBlank(message = "问题描述不能为空")
                            @Size(min = 10, message = "问题描述不少于 10 个字") String problemDesc) {
    }

    public record DutyReq(@NotBlank(message = "责任判定不能为空") String dutyResult,
                          @NotBlank(message = "判定说明不能为空")
                          @Size(min = 5, message = "说明不少于 5 个字") String reason) {
    }

    public record ReviewReq(@NotNull(message = "工单不能为空") Long orderId,
                            @NotNull(message = "评分不能为空") Integer score, String content) {
    }

    public record ReplyReq(@NotBlank(message = "回复内容不能为空") String reply) {
    }

    // ---------------- 状态流转与创新点 ----------------
    /** 通用状态流转：目标状态 + 原因（退回/作废/延期时原因不少于 5 字） */
    public record TransitionReq(@NotBlank(message = "目标状态不能为空") String targetStatus,
                               String reason) {
    }

    /** 预检规则：六项权重合计必须等于 100 */
    public record RuleReq(@NotNull(message = "车型权重不能为空") BigDecimal wModel,
                          @NotNull(message = "故障标签权重不能为空") BigDecimal wFault,
                          @NotNull(message = "诊断项目权重不能为空") BigDecimal wProject,
                          @NotNull(message = "历史工时权重不能为空") BigDecimal wHours,
                          @NotNull(message = "常用配件权重不能为空") BigDecimal wPart,
                          @NotNull(message = "当前库存权重不能为空") BigDecimal wStock,
                          BigDecimal thresholdHigh, BigDecimal thresholdLow, String remark) {
    }

    /** 人工修正：原始计算结果不得覆盖，修正原因不少于 5 字 */
    public record ManualReq(@NotBlank(message = "修正结果不能为空") String manualResult,
                            @NotBlank(message = "修正原因不能为空")
                            @Size(min = 5, message = "修正原因不少于 5 个字") String manualReason) {
    }
}
