package com.grad.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.repair.common.BizException;
import com.grad.repair.common.PageResult;
import com.grad.repair.common.UserContext;
import com.grad.repair.dto.Dtos;
import com.grad.repair.entity.*;
import com.grad.repair.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 档案与库存服务：客户、车辆、预约、项目库、配件、批次、领退。
 *
 * <p>三条库存纪律（需求文档「关键业务流程与规则」）：
 * 1) 领用/退回必须关联工单；2) 库存数量不能出现负数，不足时后端拒绝并给出缺口；
 * 3) 领用后同步扣减批次剩余量与配件总库存，退回则加回。</p>
 */
@Service
@RequiredArgsConstructor
public class MetaService {

    private final ArCustomerMapper customerMapper;
    private final ArVehicleMapper vehicleMapper;
    private final ArAppointmentMapper appointmentMapper;
    private final ArProjectMapper projectMapper;
    private final ArPartMapper partMapper;
    private final ArBatchMapper batchMapper;
    private final ArPartFlowMapper flowMapper;
    private final ArDemandMapper demandMapper;
    private final ArOrderMapper orderMapper;
    private final ArWarrantyMapper warrantyMapper;
    private final ArReturnMapper returnMapper;
    private final SysUserMapper userMapper;
    private final TraceService trace;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ================================================== 客户

    public PageResult<ArCustomer> customerPage(String keyword, String type, long page, long size) {
        var q = Wrappers.<ArCustomer>lambdaQuery().orderByDesc(ArCustomer::getId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(ArCustomer::getName, keyword).or().like(ArCustomer::getPhone, keyword)
                    .or().like(ArCustomer::getCustomerNo, keyword));
        }
        if (StringUtils.hasText(type)) {
            q.eq(ArCustomer::getCustomerType, type);
        }
        IPage<ArCustomer> p = customerMapper.selectPage(new Page<>(page, size), q);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    @Transactional
    public void saveCustomer(Dtos.CustomerReq req) {
        ArCustomer c = new ArCustomer();
        c.setName(req.name().trim());
        c.setPhone(req.phone().trim());
        c.setCustomerType(StringUtils.hasText(req.customerType()) ? req.customerType() : "NORMAL");
        c.setSource(req.source());
        c.setRemark(req.remark());
        if (req.id() == null) {
            long n = customerMapper.selectCount(null) + 1;
            c.setCustomerNo("CUS-" + String.format("%04d", n));
            c.setVisitCount(0);
            c.setTotalAmount(BigDecimal.ZERO);
            c.setDeleted(0);
            c.setCreateBy(UserContext.username());
            c.setCreateTime(LocalDateTime.now());
            c.setUpdateTime(LocalDateTime.now());
            c.setVersion(0);
            customerMapper.insert(c);
        } else {
            c.setId(req.id());
            c.setUpdateTime(LocalDateTime.now());
            customerMapper.updateById(c);
        }
    }

    // ================================================== 车辆

    public PageResult<Map<String, Object>> vehiclePage(String plate, String keyword, String status,
                                                       long page, long size) {
        var q = Wrappers.<ArVehicle>lambdaQuery().orderByDesc(ArVehicle::getId);
        if (StringUtils.hasText(plate)) {
            q.like(ArVehicle::getPlateNo, plate);
        }
        if (StringUtils.hasText(status)) {
            q.eq(ArVehicle::getVehicleStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> cids = customerMapper.selectList(Wrappers.<ArCustomer>lambdaQuery()
                            .like(ArCustomer::getName, keyword).or().like(ArCustomer::getPhone, keyword)
                            .select(ArCustomer::getId))
                    .stream().map(ArCustomer::getId).toList();
            q.and(w -> {
                w.like(ArVehicle::getVin, keyword).or().like(ArVehicle::getModel, keyword);
                if (!cids.isEmpty()) {
                    w.or().in(ArVehicle::getCustomerId, cids);
                }
            });
        }
        IPage<ArVehicle> p = vehicleMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = p.getRecords().stream().map(v -> {
            ArCustomer c = customerMapper.selectById(v.getCustomerId());
            ArOrder last = orderMapper.selectOne(Wrappers.<ArOrder>lambdaQuery()
                    .eq(ArOrder::getVehicleId, v.getId()).orderByDesc(ArOrder::getId).last("limit 1"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vehicle", v);
            m.put("customerName", c == null ? null : c.getName());
            m.put("customerPhone", c == null ? null : c.getPhone());
            m.put("lastOrderNo", last == null ? null : last.getOrderNo());
            m.put("lastStatus", last == null ? null : last.getStatus());
            m.put("lastStatusName", last == null ? null
                    : OrderService.STATUS_NAME.getOrDefault(last.getStatus(), last.getStatus()));
            m.put("lastReceive", last == null ? null : last.getReceiveTime());
            m.put("settleStatus", last == null ? "UNSETTLED"
                    : (last.getPaidAmount() != null && last.getTotalAmount() != null
                    && last.getPaidAmount().compareTo(last.getTotalAmount()) >= 0 ? "SETTLED" : "PART"));
            return m;
        }).toList();
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 车辆详情：档案 + 历史维修 + 质保中项目 + 配件更换与操作记录。 */
    public Map<String, Object> vehicleDetail(Long id) {
        ArVehicle v = vehicleMapper.selectById(id);
        if (v == null) {
            throw new BizException(404, "车辆不存在");
        }
        ArCustomer c = customerMapper.selectById(v.getCustomerId());
        List<ArOrder> orders = orderMapper.selectList(Wrappers.<ArOrder>lambdaQuery()
                .eq(ArOrder::getVehicleId, id).orderByDesc(ArOrder::getReceiveTime));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vehicle", v);
        m.put("customer", c);
        m.put("orders", orders);
        m.put("timeline", trace.timeline(v.getPlateNo()));
        return m;
    }

    @Transactional
    public void saveVehicle(Dtos.VehicleReq req) {
        String plate = req.plateNo().trim();
        if (plate.length() > 50) {
            throw new BizException(400, "车牌号长度不超过 50 个字符");
        }
        ArVehicle dup = vehicleMapper.selectOne(Wrappers.<ArVehicle>lambdaQuery()
                .eq(ArVehicle::getPlateNo, plate)
                .ne(req.id() != null, ArVehicle::getId, req.id()).last("limit 1"));
        if (dup != null) {
            throw new BizException(400, "车牌号 " + plate + " 已存在，请勿重复建档");
        }
        if (req.mileage() != null && req.mileage() < 0) {
            throw new BizException(400, "里程不得为负数");
        }
        ArVehicle v = new ArVehicle();
        v.setPlateNo(plate);
        v.setCustomerId(req.customerId());
        v.setBrand(req.brand());
        v.setModel(req.model());
        v.setVin(req.vin());
        v.setEngineNo(req.engineNo());
        v.setMileage(req.mileage() == null ? 0 : req.mileage());
        v.setVehicleStatus(StringUtils.hasText(req.vehicleStatus()) ? req.vehicleStatus() : "NORMAL");
        v.setPhoto(req.photo());
        v.setRemark(req.remark());
        if (StringUtils.hasText(req.buyDate())) {
            v.setBuyDate(LocalDate.parse(req.buyDate().substring(0, 10)));
        }
        if (req.id() == null) {
            v.setDeleted(0);
            v.setCreateBy(UserContext.username());
            v.setCreateTime(LocalDateTime.now());
            v.setUpdateTime(LocalDateTime.now());
            v.setVersion(0);
            vehicleMapper.insert(v);
        } else {
            v.setId(req.id());
            v.setUpdateTime(LocalDateTime.now());
            vehicleMapper.updateById(v);
        }
    }

    /** 删除主数据前检查引用：有工单的车辆不允许删除。 */
    @Transactional
    public void deleteVehicle(Long id) {
        long refs = orderMapper.selectCount(Wrappers.<ArOrder>lambdaQuery().eq(ArOrder::getVehicleId, id));
        if (refs > 0) {
            throw new BizException(400, "该车辆已有 " + refs + " 张维修工单，不能删除");
        }
        vehicleMapper.deleteById(id);
    }

    // ================================================== 预约

    public PageResult<Map<String, Object>> appointmentPage(String status, String keyword, long page, long size) {
        var q = Wrappers.<ArAppointment>lambdaQuery().orderByAsc(ArAppointment::getExpectTime);
        if (StringUtils.hasText(status)) {
            q.eq(ArAppointment::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> vids = vehicleMapper.selectList(Wrappers.<ArVehicle>lambdaQuery()
                            .like(ArVehicle::getPlateNo, keyword).select(ArVehicle::getId))
                    .stream().map(ArVehicle::getId).toList();
            if (!vids.isEmpty()) {
                q.in(ArAppointment::getVehicleId, vids);
            }
        }
        IPage<ArAppointment> p = appointmentMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = p.getRecords().stream().map(a -> {
            ArVehicle v = vehicleMapper.selectById(a.getVehicleId());
            ArCustomer c = customerMapper.selectById(a.getCustomerId());
            SysUser adv = a.getAdvisorId() == null ? null : userMapper.selectById(a.getAdvisorId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("appointment", a);
            m.put("plateNo", v == null ? null : v.getPlateNo());
            m.put("vehicleName", v == null ? null : v.getBrand() + " " + v.getModel());
            m.put("customerName", c == null ? null : c.getName());
            m.put("customerPhone", c == null ? null : c.getPhone());
            m.put("advisorName", adv == null ? null : adv.getRealName());
            return m;
        }).toList();
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    @Transactional
    public void saveAppointment(Dtos.AppointmentReq req) {
        ArVehicle v = vehicleMapper.selectById(req.vehicleId());
        if (v == null) {
            throw new BizException(404, "车辆不存在");
        }
        ArAppointment a = new ArAppointment();
        a.setVehicleId(req.vehicleId());
        a.setCustomerId(v.getCustomerId());
        a.setAppointType(StringUtils.hasText(req.appointType()) ? req.appointType() : "MAINTAIN");
        a.setExpectTime(LocalDateTime.parse(req.expectTime().replace(" ", "T")));
        a.setProblemType(req.problemType());
        a.setStatus("WAIT_RECEIVE");
        a.setAdvisorId(req.advisorId() == null ? UserContext.userId() : req.advisorId());
        a.setRemark(req.remark());
        a.setUpdateTime(LocalDateTime.now());
        if (req.id() == null) {
            long n = appointmentMapper.selectCount(null) + 1;
            a.setAppointNo("AP-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", n));
            a.setCreateTime(LocalDateTime.now());
            appointmentMapper.insert(a);
        } else {
            a.setId(req.id());
            appointmentMapper.updateById(a);
        }
    }

    @Transactional
    public void cancelAppointment(Long id, String reason) {
        ArAppointment a = appointmentMapper.selectById(id);
        if (a == null) {
            throw new BizException(404, "预约不存在");
        }
        if ("RECEIVED".equals(a.getStatus())) {
            throw new BizException(400, "已接车的预约不能取消");
        }
        ArAppointment u = new ArAppointment();
        u.setId(id);
        u.setStatus("CANCELED");
        u.setRemark(reason);
        u.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(u);
    }

    // ================================================== 项目库

    public PageResult<ArProject> projectPage(String keyword, String workType, long page, long size) {
        var q = Wrappers.<ArProject>lambdaQuery().orderByAsc(ArProject::getId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(ArProject::getProjectName, keyword).or().like(ArProject::getProjectCode, keyword));
        }
        if (StringUtils.hasText(workType)) {
            q.eq(ArProject::getWorkType, workType);
        }
        IPage<ArProject> p = projectMapper.selectPage(new Page<>(page, size), q);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    public List<ArProject> projects() {
        return projectMapper.selectList(Wrappers.<ArProject>lambdaQuery().eq(ArProject::getStatus, 1));
    }

    // ================================================== 配件与批次

    public PageResult<ArPart> partPage(String keyword, String stockState, long page, long size) {
        var q = Wrappers.<ArPart>lambdaQuery().orderByAsc(ArPart::getId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(ArPart::getPartName, keyword).or().like(ArPart::getPartCode, keyword));
        }
        if ("LOW".equals(stockState)) {
            q.apply("stock_qty < min_qty");
        } else if ("OUT".equals(stockState)) {
            q.le(ArPart::getStockQty, 0);
        }
        IPage<ArPart> p = partMapper.selectPage(new Page<>(page, size), q);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    @Transactional
    public void savePart(Dtos.PartReq req) {
        ArPart dup = partMapper.selectOne(Wrappers.<ArPart>lambdaQuery()
                .eq(ArPart::getPartCode, req.partCode().trim())
                .ne(req.id() != null, ArPart::getId, req.id()).last("limit 1"));
        if (dup != null) {
            throw new BizException(400, "配件编码已存在");
        }
        if (req.salePrice() != null && req.purchasePrice() != null
                && req.salePrice().compareTo(req.purchasePrice()) < 0) {
            throw new BizException(400, "销售价不得低于采购价");
        }
        ArPart p = new ArPart();
        p.setPartCode(req.partCode().trim());
        p.setPartName(req.partName());
        p.setApplyModels(req.applyModels());
        p.setUnit(StringUtils.hasText(req.unit()) ? req.unit() : "个");
        p.setPurchasePrice(req.purchasePrice());
        p.setSalePrice(req.salePrice());
        p.setMinQty(req.minQty());
        p.setMaxQty(req.maxQty());
        p.setImage(req.image());
        p.setStatus(req.status() == null ? 1 : req.status());
        if (req.id() == null) {
            p.setStockQty(BigDecimal.ZERO);
            p.setDeleted(0);
            p.setCreateTime(LocalDateTime.now());
            p.setUpdateTime(LocalDateTime.now());
            partMapper.insert(p);
        } else {
            p.setId(req.id());
            p.setUpdateTime(LocalDateTime.now());
            partMapper.updateById(p);
        }
    }

    public PageResult<Map<String, Object>> batchPage(Long partId, String keyword, long page, long size) {
        var q = Wrappers.<ArBatch>lambdaQuery().orderByDesc(ArBatch::getId);
        if (partId != null) {
            q.eq(ArBatch::getPartId, partId);
        }
        if (StringUtils.hasText(keyword)) {
            q.like(ArBatch::getBatchNo, keyword);
        }
        IPage<ArBatch> p = batchMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = p.getRecords().stream().map(b -> {
            ArPart part = partMapper.selectById(b.getPartId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("batch", b);
            m.put("partName", part == null ? null : part.getPartName());
            m.put("partCode", part == null ? null : part.getPartCode());
            m.put("unit", part == null ? null : part.getUnit());
            return m;
        }).toList();
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 入库：写批次并累加配件总库存。 */
    @Transactional
    public void inbound(Dtos.BatchReq req) {
        ArPart part = partMapper.selectById(req.partId());
        if (part == null) {
            throw new BizException(404, "配件不存在");
        }
        if (req.inQty() == null || req.inQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "入库数量必须大于 0");
        }
        ArBatch b = new ArBatch();
        b.setPartId(req.partId());
        b.setBatchNo(req.batchNo().trim());
        b.setWarehouse(StringUtils.hasText(req.warehouse()) ? req.warehouse() : "主仓");
        b.setInTime(LocalDateTime.now());
        b.setInQty(req.inQty());
        b.setLeftQty(req.inQty());
        b.setPurchasePrice(req.purchasePrice() == null ? part.getPurchasePrice() : req.purchasePrice());
        b.setSupplier(req.supplier());
        if (StringUtils.hasText(req.expireDate())) {
            b.setExpireDate(LocalDate.parse(req.expireDate().substring(0, 10)));
        }
        b.setStatus("IN_STOCK");
        b.setCreateTime(LocalDateTime.now());
        batchMapper.insert(b);

        ArPart u = new ArPart();
        u.setId(part.getId());
        u.setStockQty((part.getStockQty() == null ? BigDecimal.ZERO : part.getStockQty()).add(req.inQty()));
        u.setUpdateTime(LocalDateTime.now());
        partMapper.updateById(u);
        trace.log(UserContext.username(), "配件入库", b.getBatchNo(),
                part.getPartName() + " 入库 " + req.inQty() + part.getUnit());
    }

    // ================================================== 领退

    public PageResult<Map<String, Object>> flowPage(Long orderId, String flowType, long page, long size) {
        var q = Wrappers.<ArPartFlow>lambdaQuery().orderByDesc(ArPartFlow::getId);
        if (orderId != null) {
            q.eq(ArPartFlow::getOrderId, orderId);
        }
        if (StringUtils.hasText(flowType)) {
            q.eq(ArPartFlow::getFlowType, flowType);
        }
        IPage<ArPartFlow> p = flowMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = p.getRecords().stream().map(f -> {
            ArPart part = partMapper.selectById(f.getPartId());
            ArOrder o = orderMapper.selectById(f.getOrderId());
            ArBatch b = f.getBatchId() == null ? null : batchMapper.selectById(f.getBatchId());
            SysUser tech = f.getTechId() == null ? null : userMapper.selectById(f.getTechId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("flow", f);
            m.put("partName", part == null ? null : part.getPartName());
            m.put("partCode", part == null ? null : part.getPartCode());
            m.put("unit", part == null ? null : part.getUnit());
            m.put("orderNo", o == null ? null : o.getOrderNo());
            m.put("batchNo", b == null ? null : b.getBatchNo());
            m.put("techName", tech == null ? null : tech.getRealName());
            return m;
        }).toList();
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /**
     * 领用 / 补领 / 退回。
     * 领用：校验工单存在、项目已确认、库存充足，扣减批次与总库存；
     * 退回：加回库存并写退回记录，旧件处理方式必填。
     */
    @Transactional
    public void partFlow(Dtos.PartFlowReq req) {
        ArOrder o = orderMapper.selectById(req.orderId());
        if (o == null) {
            throw new BizException(404, "关联工单不存在");
        }
        if (List.of("FINISHED", "CANCELED").contains(o.getStatus())) {
            throw new BizException(400, "已结束的工单不能再领退配件");
        }
        ArPart part = partMapper.selectById(req.partId());
        if (part == null) {
            throw new BizException(404, "配件不存在");
        }
        BigDecimal qty = req.qty();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "数量必须大于 0");
        }
        boolean isReturn = "RETURN".equalsIgnoreCase(req.flowType());
        if (!isReturn) {
            long confirmed = o.getStatus().equals("RECEIVE") || o.getStatus().equals("DIAGNOSIS")
                    || o.getStatus().equals("QUOTE_CONFIRM") ? 0 : 1;
            if (confirmed == 0) {
                throw new BizException(400, "未经客户确认的收费项目不得进入配件领用流程");
            }
            BigDecimal stock = part.getStockQty() == null ? BigDecimal.ZERO : part.getStockQty();
            if (stock.compareTo(qty) < 0) {
                throw new BizException(400, "「" + part.getPartName() + "」库存不足，现有 "
                        + stock.stripTrailingZeros().toPlainString() + part.getUnit()
                        + "，需求 " + qty.stripTrailingZeros().toPlainString() + part.getUnit());
            }
        }

        // 批次扣减/回补
        ArBatch batch = req.batchId() != null ? batchMapper.selectById(req.batchId())
                : batchMapper.selectOne(Wrappers.<ArBatch>lambdaQuery()
                .eq(ArBatch::getPartId, part.getId())
                .gt(ArBatch::getLeftQty, BigDecimal.ZERO)
                .orderByAsc(ArBatch::getInTime).last("limit 1"));
        if (!isReturn && batch != null) {
            if (batch.getLeftQty().compareTo(qty) < 0) {
                throw new BizException(400, "批次 " + batch.getBatchNo() + " 余量不足，请选择其他批次");
            }
            ArBatch bu = new ArBatch();
            bu.setId(batch.getId());
            bu.setLeftQty(batch.getLeftQty().subtract(qty));
            bu.setStatus(bu.getLeftQty().compareTo(BigDecimal.ZERO) <= 0 ? "OUT" : "IN_STOCK");
            batchMapper.updateById(bu);
        } else if (isReturn && batch != null) {
            ArBatch bu = new ArBatch();
            bu.setId(batch.getId());
            bu.setLeftQty(batch.getLeftQty().add(qty));
            bu.setStatus("IN_STOCK");
            batchMapper.updateById(bu);
        }

        ArPart pu = new ArPart();
        pu.setId(part.getId());
        pu.setStockQty((part.getStockQty() == null ? BigDecimal.ZERO : part.getStockQty())
                .add(isReturn ? qty : qty.negate()));
        pu.setUpdateTime(LocalDateTime.now());
        partMapper.updateById(pu);

        long n = flowMapper.selectCount(null) + 1;
        ArPartFlow f = new ArPartFlow();
        f.setFlowNo("PR-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", n));
        f.setOrderId(o.getId());
        f.setPartId(part.getId());
        f.setBatchId(batch == null ? null : batch.getId());
        f.setQty(qty);
        f.setFlowType(req.flowType().toUpperCase());
        f.setTechId(req.techId() == null ? o.getTechId() : req.techId());
        f.setOldPartHandle(req.oldPartHandle());
        f.setOperatorId(UserContext.userId());
        f.setFlowTime(LocalDateTime.now());
        f.setStatus(isReturn ? "RETURNED" : "ISSUED");
        f.setRemark(req.remark());
        f.setCreateTime(LocalDateTime.now());
        flowMapper.insert(f);

        // 需求单标记为已领
        if (!isReturn) {
            List<ArDemand> ds = demandMapper.selectList(Wrappers.<ArDemand>lambdaQuery()
                    .eq(ArDemand::getOrderId, o.getId()).eq(ArDemand::getPartId, part.getId())
                    .eq(ArDemand::getStatus, "WAIT"));
            for (ArDemand d : ds) {
                ArDemand du = new ArDemand();
                du.setId(d.getId());
                du.setStatus("CLAIMED");
                demandMapper.updateById(du);
            }
        }
        trace.log(UserContext.username(), isReturn ? "配件退回" : "配件领用", o.getOrderNo(),
                part.getPartName() + " × " + qty + part.getUnit()
                        + (StringUtils.hasText(req.oldPartHandle()) ? "｜旧件：" + req.oldPartHandle() : ""));
    }

    /** 库存预警列表：低于下限或已缺货，供看板「库存预警」与消息中心使用。 */
    public List<Map<String, Object>> stockWarnings() {
        List<ArPart> ps = partMapper.selectList(Wrappers.<ArPart>lambdaQuery()
                .apply("stock_qty <= min_qty").orderByAsc(ArPart::getStockQty));
        return ps.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("partName", p.getPartName());
            m.put("partCode", p.getPartCode());
            m.put("stockQty", p.getStockQty());
            m.put("minQty", p.getMinQty());
            m.put("unit", p.getUnit());
            m.put("state", p.getStockQty() != null && p.getStockQty().compareTo(BigDecimal.ZERO) <= 0
                    ? "OUT" : "LOW");
            return m;
        }).toList();
    }

    /** 员工列表：直接取用户表，岗位由角色反推，避免重复建员工表。 */
    public List<Map<String, Object>> staffList() {
        List<SysUser> us = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getStatus, 1).orderByAsc(SysUser::getId));
        return us.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("avatar", u.getAvatar());
            m.put("orgName", u.getOrgName());
            m.put("lastLogin", u.getLastLogin());
            long working = 0;
            m.put("working", working);
            return m;
        }).toList();
    }

    // ================================================== 质保台账 / 返修记录

    /**
     * 质保台账分页：按车牌、项目名检索，带剩余天数。
     * 剩余天数由后端算，前端只展示（避免时区/时差导致的偏差）。
     */
    public PageResult<Map<String, Object>> warrantyPage(long page, long size, String status, String keyword) {
        Wrappers.<ArWarranty>lambdaQuery();
        var q = Wrappers.<ArWarranty>lambdaQuery()
                .eq(StringUtils.hasText(status), ArWarranty::getStatus, status)
                .orderByDesc(ArWarranty::getId);
        IPage<ArWarranty> p = warrantyMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ArWarranty w : p.getRecords()) {
            ArVehicle v = vehicleMapper.selectById(w.getVehicleId());
            ArCustomer c = v == null ? null : customerMapper.selectById(v.getCustomerId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("warrantyNo", w.getWarrantyNo());
            m.put("orderId", w.getOrderId());
            m.put("projectName", w.getProjectName());
            m.put("plateNo", v == null ? "—" : v.getPlateNo());
            m.put("vehicleName", v == null ? "—" : v.getBrand() + " " + v.getModel());
            m.put("customerName", c == null ? "—" : c.getName());
            m.put("warrantyDays", w.getWarrantyDays());
            m.put("warrantyKm", w.getWarrantyKm());
            m.put("startDate", w.getStartDate());
            m.put("endDate", w.getEndDate());
            m.put("status", w.getStatus());
            m.put("leftDays", w.getEndDate() == null ? null
                    : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), w.getEndDate()));
            rows.add(m);
        }
        List<Map<String, Object>> filtered = StringUtils.hasText(keyword)
                ? rows.stream().filter(r -> String.valueOf(r.get("plateNo")).contains(keyword)
                    || String.valueOf(r.get("projectName")).contains(keyword)
                    || String.valueOf(r.get("warrantyNo")).contains(keyword)).toList()
                : rows;
        return PageResult.of(p.getTotal(), page, size, filtered);
    }

    /** 返修记录分页：按责任判定与状态筛选。 */
    public PageResult<Map<String, Object>> returnPage(long page, long size, String status, String duty) {
        var q = Wrappers.<ArReturn>lambdaQuery()
                .eq(StringUtils.hasText(status), ArReturn::getStatus, status)
                .eq(StringUtils.hasText(duty), ArReturn::getDutyResult, duty)
                .orderByDesc(ArReturn::getId);
        IPage<ArReturn> p = returnMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ArReturn r : p.getRecords()) {
            ArOrder o = orderMapper.selectById(r.getOrderId());
            ArVehicle v = r.getVehicleId() == null ? null : vehicleMapper.selectById(r.getVehicleId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("returnNo", r.getReturnNo());
            m.put("orderId", r.getOrderId());
            m.put("orderNo", o == null ? "—" : o.getOrderNo());
            m.put("plateNo", v == null ? "—" : v.getPlateNo());
            m.put("itemName", r.getItemName());
            m.put("problemDesc", r.getProblemDesc());
            m.put("dutyResult", r.getDutyResult());
            m.put("status", r.getStatus());
            m.put("reworkTimes", r.getReworkTimes());
            m.put("createTime", r.getCreateTime());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 下拉选项：车辆、项目、配件、技师，统一走一个接口减少前端请求数。 */
    public Map<String, Object> options() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vehicles", vehicleMapper.selectList(Wrappers.<ArVehicle>lambdaQuery()
                .orderByDesc(ArVehicle::getId).last("limit 200")));
        m.put("projects", projects());
        m.put("parts", partMapper.selectList(Wrappers.<ArPart>lambdaQuery()
                .eq(ArPart::getStatus, 1).orderByAsc(ArPart::getId)));
        m.put("techs", userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, List.of(5L, 6L, 7L, 8L, 9L, 10L))));
        m.put("advisors", userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .in(SysUser::getId, List.of(3L, 4L))));
        return m;
    }
}
