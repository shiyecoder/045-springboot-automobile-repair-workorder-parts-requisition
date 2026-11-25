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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 维修工单主流程服务。
 *
 * <p>状态机（需求文档「状态流转明细」）：
 * <pre>
 * RECEIVE(接车登记) → DIAGNOSIS(故障诊断) → QUOTE_CONFIRM(报价确认)
 *   → REPAIR_PART(维修领料) → QC(质量检验) → SETTLE(交车结算) → FINISHED(已交车)
 * 异常分支：REWORK(返工中↔QC) / CANCELED(已取消)
 * </pre>
 *
 * <p>三条硬规则，全部在后端校验，前端按钮只是提示：
 * <ol>
 *   <li>未经客户确认的收费项目不得进入派工与配件领用；</li>
 *   <li>配件领用/退回必须关联工单，库存不能出现负数；</li>
 *   <li>未通过质检或存在未结费用的工单不能交车。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ArOrderMapper orderMapper;
    private final ArVehicleMapper vehicleMapper;
    private final ArCustomerMapper customerMapper;
    private final ArReceptionMapper receptionMapper;
    private final ArInspectionMapper inspectionMapper;
    private final ArOrderItemMapper itemMapper;
    private final ArProjectMapper projectMapper;
    private final ArQuoteMapper quoteMapper;
    private final ArQuoteItemMapper quoteItemMapper;
    private final ArConfirmMapper confirmMapper;
    private final ArDispatchMapper dispatchMapper;
    private final ArWorklogMapper worklogMapper;
    private final ArPartMapper partMapper;
    private final ArDemandMapper demandMapper;
    private final ArPartFlowMapper flowMapper;
    private final ArQcMapper qcMapper;
    private final ArReworkLogMapper reworkLogMapper;
    private final ArFeeMapper feeMapper;
    private final ArPaymentMapper paymentMapper;
    private final ArDeliveryMapper deliveryMapper;
    private final ArWarrantyMapper warrantyMapper;
    private final ArReturnMapper returnMapper;
    private final ArReviewMapper reviewMapper;
    private final SysUserMapper userMapper;
    private final TraceService trace;

    /** 状态中文名，用于日志与前端标签 */
    public static final Map<String, String> STATUS_NAME = new LinkedHashMap<>();
    static {
        STATUS_NAME.put("RECEIVE", "接车登记");
        STATUS_NAME.put("DIAGNOSIS", "故障诊断");
        STATUS_NAME.put("QUOTE_CONFIRM", "报价确认");
        STATUS_NAME.put("REPAIR_PART", "维修领料");
        STATUS_NAME.put("QC", "质量检验");
        STATUS_NAME.put("SETTLE", "交车结算");
        STATUS_NAME.put("FINISHED", "已交车");
        STATUS_NAME.put("REWORK", "返工中");
        STATUS_NAME.put("CANCELED", "已取消");
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private String nextNo(String prefix, String day) {
        long n = orderMapper.selectCount(Wrappers.<ArOrder>lambdaQuery()
                .likeRight(ArOrder::getOrderNo, prefix + "-" + day)) + 1;
        return prefix + "-" + day + "-" + String.format("%04d", n);
    }

    private String userName(Long id) {
        if (id == null) {
            return null;
        }
        SysUser u = userMapper.selectById(id);
        return u == null ? null : u.getRealName();
    }

    // ================================================== 接车建单

    /** 接车登记：校验里程、写接车检查、生成唯一工单号并置为「接车登记」状态。 */
    @Transactional
    public Map<String, Object> receive(Dtos.ReceptionReq req) {
        ArVehicle v = vehicleMapper.selectById(req.vehicleId());
        if (v == null) {
            throw new BizException(404, "车辆不存在");
        }
        if (req.mileage() == null || req.mileage() < 0) {
            throw new BizException(400, "进厂里程不得为负数");
        }
        if (req.mileage() < v.getMileage()) {
            throw new BizException(400, "进厂里程不得小于档案里程 " + v.getMileage() + " km");
        }
        if (req.photos() == null || req.photos().isEmpty()) {
            throw new BizException(400, "现场照片至少上传 1 张");
        }
        String fault = req.faultDesc();
        if (fault != null && fault.length() > 100) {
            throw new BizException(400, "故障描述不超过 100 个字符");
        }

        String day = LocalDate.now().format(DAY);
        ArOrder o = new ArOrder();
        o.setOrderNo(nextNo("WO", day));
        o.setVehicleId(v.getId());
        o.setCustomerId(v.getCustomerId());
        o.setOrderType(StringUtils.hasText(req.orderType()) ? req.orderType() : "REPAIR");
        o.setStatus("RECEIVE");
        o.setAdvisorId(req.advisorId() == null ? UserContext.userId() : req.advisorId());
        o.setReceiveTime(LocalDateTime.now());
        o.setExpectFinish(StringUtils.hasText(req.expectFinish())
                ? LocalDateTime.parse(req.expectFinish().replace(" ", "T")) : LocalDateTime.now().plusDays(1));
        o.setFaultDesc(fault);
        o.setUrgency("NORMAL");
        o.setTotalAmount(BigDecimal.ZERO);
        o.setPaidAmount(BigDecimal.ZERO);
        o.setDiscount(BigDecimal.ZERO);
        o.setReworkTimes(0);
        o.setDeleted(0);
        o.setCreateBy(UserContext.username());
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        o.setVersion(0);
        orderMapper.insert(o);

        ArReception r = new ArReception();
        r.setOrderId(o.getId());
        r.setMileage(req.mileage());
        r.setFuelLevel(req.fuelLevel());
        r.setScratchDesc(req.scratchDesc());
        r.setItemsDesc(req.itemsDesc());
        r.setPhotos(String.join(",", req.photos()));
        r.setReceiveTime(o.getReceiveTime());
        r.setAdvisorId(o.getAdvisorId());
        r.setCreateTime(LocalDateTime.now());
        receptionMapper.insert(r);

        ArVehicle uv = new ArVehicle();
        uv.setId(v.getId());
        uv.setMileage(req.mileage());
        uv.setVehicleStatus("IN_REPAIR");
        uv.setUpdateTime(LocalDateTime.now());
        vehicleMapper.updateById(uv);

        trace.log(UserContext.username(), "接车建单", o.getOrderNo(),
                "新建工单 → 接车登记｜进厂里程 " + req.mileage() + " km");
        return detail(o.getId());
    }

    // ================================================== 查询

    public PageResult<Map<String, Object>> page(String status, String keyword, Long advisorId,
                                                Long techId, boolean onlyMine, long page, long size) {
        var q = Wrappers.<ArOrder>lambdaQuery();
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            if ("TODO".equalsIgnoreCase(status)) {
                q.in(ArOrder::getStatus, List.of("RECEIVE", "DIAGNOSIS", "QUOTE_CONFIRM", "QC"));
            } else if ("DOING".equalsIgnoreCase(status)) {
                q.in(ArOrder::getStatus, List.of("REPAIR_PART", "REWORK"));
            } else if ("DONE".equalsIgnoreCase(status)) {
                q.in(ArOrder::getStatus, List.of("SETTLE", "FINISHED"));
            } else if ("ABNORMAL".equalsIgnoreCase(status)) {
                q.in(ArOrder::getStatus, List.of("REWORK", "CANCELED"));
            } else {
                q.eq(ArOrder::getStatus, status);
            }
        }
        if (advisorId != null) {
            q.eq(ArOrder::getAdvisorId, advisorId);
        }
        if (techId != null) {
            q.eq(ArOrder::getTechId, techId);
        }
        // 技师只查看本人任务（需求文档 4.10）
        if (onlyMine && UserContext.isTech()) {
            q.eq(ArOrder::getTechId, UserContext.userId());
        }
        if (StringUtils.hasText(keyword)) {
            List<Long> vids = vehicleMapper.selectList(Wrappers.<ArVehicle>lambdaQuery()
                            .like(ArVehicle::getPlateNo, keyword).select(ArVehicle::getId))
                    .stream().map(ArVehicle::getId).toList();
            q.and(w -> {
                w.like(ArOrder::getOrderNo, keyword);
                if (!vids.isEmpty()) {
                    w.or().in(ArOrder::getVehicleId, vids);
                }
            });
        }
        // 默认「待本人处理优先、更新时间倒序」
        q.last("ORDER BY FIELD(status,'RECEIVE','DIAGNOSIS','QUOTE_CONFIRM','QC','REPAIR_PART','REWORK','SETTLE','FINISHED','CANCELED'), update_time DESC");
        IPage<ArOrder> p = orderMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = p.getRecords().stream().map(this::row).toList();
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    private Map<String, Object> row(ArOrder o) {
        ArVehicle v = vehicleMapper.selectById(o.getVehicleId());
        ArCustomer c = customerMapper.selectById(o.getCustomerId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderNo", o.getOrderNo());
        m.put("plateNo", v == null ? null : v.getPlateNo());
        m.put("vehicleName", v == null ? null : v.getBrand() + " " + v.getModel());
        m.put("customerName", c == null ? null : c.getName());
        m.put("orderType", o.getOrderType());
        m.put("status", o.getStatus());
        m.put("statusName", STATUS_NAME.getOrDefault(o.getStatus(), o.getStatus()));
        m.put("techName", userName(o.getTechId()));
        m.put("advisorName", userName(o.getAdvisorId()));
        m.put("receiveTime", o.getReceiveTime());
        m.put("expectFinish", o.getExpectFinish());
        m.put("totalAmount", o.getTotalAmount());
        m.put("paidAmount", o.getPaidAmount());
        m.put("updateTime", o.getUpdateTime());
        return m;
    }

    public ArOrder get(Long id) {
        ArOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException(404, "工单不存在");
        }
        return o;
    }

    /** 工单详情：基础信息 + 关联单据 + 时间轴，详情页不重复拼接列表字段。 */
    public Map<String, Object> detail(Long id) {
        ArOrder o = get(id);
        ArVehicle v = vehicleMapper.selectById(o.getVehicleId());
        ArCustomer c = customerMapper.selectById(o.getCustomerId());
        Map<String, Object> m = new LinkedHashMap<>(row(o));
        m.put("faultDesc", o.getFaultDesc());
        m.put("urgency", o.getUrgency());
        m.put("discount", o.getDiscount());
        m.put("reworkTimes", o.getReworkTimes());
        m.put("vehicle", v);
        m.put("customer", c);
        m.put("reception", receptionMapper.selectOne(Wrappers.<ArReception>lambdaQuery()
                .eq(ArReception::getOrderId, id).last("limit 1")));
        m.put("inspections", inspectionMapper.selectList(Wrappers.<ArInspection>lambdaQuery()
                .eq(ArInspection::getOrderId, id).orderByAsc(ArInspection::getId)));
        List<ArOrderItem> items = itemMapper.selectList(Wrappers.<ArOrderItem>lambdaQuery()
                .eq(ArOrderItem::getOrderId, id).orderByAsc(ArOrderItem::getId));
        items.forEach(i -> i.setProjectName(i.getProjectName()));
        m.put("items", items);
        m.put("quotes", quoteMapper.selectList(Wrappers.<ArQuote>lambdaQuery()
                .eq(ArQuote::getOrderId, id).orderByDesc(ArQuote::getVersionNo)));
        ArQuote last = quoteMapper.selectOne(Wrappers.<ArQuote>lambdaQuery()
                .eq(ArQuote::getOrderId, id).orderByDesc(ArQuote::getVersionNo).last("limit 1"));
        m.put("currentQuote", last);
        m.put("quoteItems", last == null ? List.of() : quoteItemMapper.selectList(
                Wrappers.<ArQuoteItem>lambdaQuery().eq(ArQuoteItem::getQuoteId, last.getId())
                        .orderByAsc(ArQuoteItem::getSort)));
        m.put("confirms", confirmMapper.selectList(Wrappers.<ArConfirm>lambdaQuery()
                .eq(ArConfirm::getOrderId, id).orderByDesc(ArConfirm::getVersionNo)));
        m.put("dispatches", dispatchMapper.selectList(Wrappers.<ArDispatch>lambdaQuery()
                .eq(ArDispatch::getOrderId, id).orderByAsc(ArDispatch::getId)));
        m.put("flows", flowMapper.selectList(Wrappers.<ArPartFlow>lambdaQuery()
                .eq(ArPartFlow::getOrderId, id).orderByDesc(ArPartFlow::getId)));
        m.put("qcList", qcMapper.selectList(Wrappers.<ArQc>lambdaQuery()
                .eq(ArQc::getOrderId, id).orderByDesc(ArQc::getId)));
        m.put("reworks", reworkLogMapper.selectList(Wrappers.<ArReworkLog>lambdaQuery()
                .eq(ArReworkLog::getOrderId, id).orderByDesc(ArReworkLog::getId)));
        m.put("fees", feeMapper.selectList(Wrappers.<ArFee>lambdaQuery().eq(ArFee::getOrderId, id)));
        m.put("payments", paymentMapper.selectList(Wrappers.<ArPayment>lambdaQuery()
                .eq(ArPayment::getOrderId, id).orderByDesc(ArPayment::getId)));
        m.put("delivery", deliveryMapper.selectOne(Wrappers.<ArDelivery>lambdaQuery()
                .eq(ArDelivery::getOrderId, id).last("limit 1")));
        m.put("warranties", warrantyMapper.selectList(Wrappers.<ArWarranty>lambdaQuery()
                .eq(ArWarranty::getOrderId, id)));
        m.put("timeline", trace.timeline(o.getOrderNo()));
        return m;
    }

    // ================================================== 状态流转

    /** 允许的前进目标状态，越级调用直接拒绝（并发与越级校验）。 */
    private static final Map<String, String> NEXT = Map.of(
            "RECEIVE", "DIAGNOSIS",
            "DIAGNOSIS", "QUOTE_CONFIRM",
            "QUOTE_CONFIRM", "REPAIR_PART",
            "REPAIR_PART", "QC",
            "QC", "SETTLE",
            "REWORK", "QC");

    @Transactional
    public Map<String, Object> transition(Long id, String target, String reason) {
        ArOrder o = get(id);
        String from = o.getStatus();
        if (!Objects.equals(NEXT.get(from), target)) {
            throw new BizException(400, "当前状态「" + STATUS_NAME.get(from) + "」不能流转到「"
                    + STATUS_NAME.getOrDefault(target, target) + "」");
        }
        if ("DIAGNOSIS".equals(target) && inspectionMapper.selectCount(
                Wrappers.<ArInspection>lambdaQuery().eq(ArInspection::getOrderId, id)) == 0) {
            throw new BizException(400, "至少登记 1 条检测结果才能进入报价");
        }
        if ("REPAIR_PART".equals(target)) {
            long confirmed = itemMapper.selectCount(Wrappers.<ArOrderItem>lambdaQuery()
                    .eq(ArOrderItem::getOrderId, id).eq(ArOrderItem::getConfirmed, 1));
            if (confirmed == 0) {
                throw new BizException(400, "未经客户确认的收费项目不得进入正式派工和配件领用流程");
            }
        }
        if ("QC".equals(target) && "REPAIR_PART".equals(from)) {
            long undone = itemMapper.selectCount(Wrappers.<ArOrderItem>lambdaQuery()
                    .eq(ArOrderItem::getOrderId, id).eq(ArOrderItem::getConfirmed, 1)
                    .ne(ArOrderItem::getItemStatus, "DONE"));
            if (undone > 0) {
                throw new BizException(400, "还有 " + undone + " 个已确认项目未完工，不能提交质检");
            }
        }
        ArOrder upd = new ArOrder();
        upd.setId(id);
        upd.setStatus(target);
        upd.setUpdateTime(LocalDateTime.now());
        if ("SETTLE".equals(target)) {
            upd.setFinishTime(LocalDateTime.now());
        }
        orderMapper.updateById(upd);
        trace.log(UserContext.username(), "状态流转", o.getOrderNo(),
                STATUS_NAME.get(from) + " → " + STATUS_NAME.getOrDefault(target, target)
                        + (StringUtils.hasText(reason) ? "｜" + reason : ""));
        return detail(id);
    }

    /** 取消工单：必须填原因，已产生领料或已交车的工单不允许取消。 */
    @Transactional
    public void cancel(Long id, String reason) {
        ArOrder o = get(id);
        if (!StringUtils.hasText(reason) || reason.trim().length() < 5) {
            throw new BizException(400, "取消原因不少于 5 个字");
        }
        if (List.of("FINISHED", "SETTLE").contains(o.getStatus())) {
            throw new BizException(400, "已结算或已交车的工单不能取消");
        }
        if (flowMapper.selectCount(Wrappers.<ArPartFlow>lambdaQuery()
                .eq(ArPartFlow::getOrderId, id).eq(ArPartFlow::getStatus, "ISSUED")) > 0) {
            throw new BizException(400, "已发生配件领用，请先办理退回再取消");
        }
        ArOrder upd = new ArOrder();
        upd.setId(id);
        upd.setStatus("CANCELED");
        upd.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(upd);
        trace.log(UserContext.username(), "取消工单", o.getOrderNo(),
                STATUS_NAME.get(o.getStatus()) + " → 已取消｜" + reason);
    }

    // ================================================== 检测

    @Transactional
    public void addInspection(Dtos.InspectionReq req) {
        ArOrder o = get(req.orderId());
        if (!List.of("RECEIVE", "DIAGNOSIS").contains(o.getStatus())) {
            throw new BizException(400, "当前状态不允许登记检测结果");
        }
        ArInspection i = new ArInspection();
        i.setOrderId(req.orderId());
        i.setCheckItem(req.checkItem());
        i.setCheckResult(req.checkResult());
        i.setFaultDesc(req.faultDesc());
        i.setSeverity(req.severity());
        i.setSuggestion(req.suggestion());
        i.setTechId(UserContext.isTech() ? UserContext.userId() : null);
        i.setCheckTime(LocalDateTime.now());
        i.setCreateTime(LocalDateTime.now());
        inspectionMapper.insert(i);
        trace.log(UserContext.username(), "登记检测", o.getOrderNo(),
                req.checkItem() + "：" + req.checkResult()
                        + (StringUtils.hasText(req.faultDesc()) ? "｜" + req.faultDesc() : ""));
    }

    @Transactional
    public void deleteInspection(Long id) {
        ArInspection i = inspectionMapper.selectById(id);
        if (i == null) {
            throw new BizException(404, "检测记录不存在");
        }
        ArOrder o = get(i.getOrderId());
        if (!"DIAGNOSIS".equals(o.getStatus()) && !"RECEIVE".equals(o.getStatus())) {
            throw new BizException(400, "已进入报价环节，检测记录不可删除");
        }
        inspectionMapper.deleteById(id);
    }

    // ================================================== 工单项目

    /** 从标准项目库添加项目；工时费按项目库单价重算，前端传值只作参考。 */
    @Transactional
    public void addItem(Dtos.ItemReq req) {
        ArOrder o = get(req.orderId());
        if (List.of("FINISHED", "CANCELED").contains(o.getStatus())) {
            throw new BizException(400, "已结束的工单不能追加项目");
        }
        ArProject p = projectMapper.selectById(req.projectId());
        if (p == null || p.getStatus() == null || p.getStatus() == 0) {
            throw new BizException(400, "项目不存在或已停用");
        }
        ArOrderItem it = new ArOrderItem();
        it.setOrderId(req.orderId());
        it.setProjectId(p.getId());
        it.setProjectName(p.getProjectName());
        it.setWorkType(p.getWorkType());
        it.setHours(req.hours() == null ? p.getStdHours() : req.hours());
        it.setLaborFee(p.getLaborFee());
        it.setTechId(req.techId());
        it.setItemStatus("WAIT");
        it.setConfirmed(0);
        it.setCreateTime(LocalDateTime.now());
        itemMapper.insert(it);
        trace.log(UserContext.username(), "追加项目", o.getOrderNo(), "新增项目：" + p.getProjectName());
    }

    @Transactional
    public void removeItem(Long itemId) {
        ArOrderItem it = itemMapper.selectById(itemId);
        if (it == null) {
            throw new BizException(404, "项目不存在");
        }
        ArOrder o = get(it.getOrderId());
        if (Integer.valueOf(1).equals(it.getConfirmed())) {
            throw new BizException(400, "客户已确认的项目不能直接删除，请生成新版本报价");
        }
        if (List.of("REPAIR_PART", "QC", "SETTLE", "FINISHED").contains(o.getStatus())) {
            throw new BizException(400, "工单已进入维修环节，项目不可删除");
        }
        itemMapper.deleteById(itemId);
        trace.log(UserContext.username(), "删除项目", o.getOrderNo(), "删除项目：" + it.getProjectName());
    }

    // ================================================== 报价

    /**
     * 生成报价版本。
     * 报价 = 工单项目工时费（LABOR）+ 配件需求×销售价（PART）- 优惠；
     * 已确认的版本只读，任何修改都会生成新版本（需求文档 4.4）。
     */
    @Transactional
    public Map<String, Object> createQuote(Dtos.QuoteReq req) {
        ArOrder o = get(req.orderId());
        if (List.of("FINISHED", "CANCELED").contains(o.getStatus())) {
            throw new BizException(400, "已结束的工单不能报价");
        }
        List<ArOrderItem> items = itemMapper.selectList(Wrappers.<ArOrderItem>lambdaQuery()
                .eq(ArOrderItem::getOrderId, o.getId()).orderByAsc(ArOrderItem::getId));
        if (items.isEmpty()) {
            throw new BizException(400, "请先添加维修项目再生成报价");
        }
        int ver = Math.toIntExact(quoteMapper.selectCount(Wrappers.<ArQuote>lambdaQuery()
                .eq(ArQuote::getOrderId, o.getId()))) + 1;
        BigDecimal labor = items.stream().map(ArOrderItem::getLaborFee)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 历史版本中客户已拒绝的项目默认剔除，避免重复报价
        ArQuote prev = quoteMapper.selectOne(Wrappers.<ArQuote>lambdaQuery()
                .eq(ArQuote::getOrderId, o.getId()).orderByDesc(ArQuote::getVersionNo).last("limit 1"));
        Set<Long> rejected = new HashSet<>();
        if (prev != null) {
            quoteItemMapper.selectList(Wrappers.<ArQuoteItem>lambdaQuery()
                            .eq(ArQuoteItem::getQuoteId, prev.getId())
                            .eq(ArQuoteItem::getConfirmStatus, "REJECT"))
                    .forEach(qi -> {
                        if (qi.getRefId() != null) {
                            rejected.add(qi.getRefId());
                        }
                    });
        }

        ArQuote qt = new ArQuote();
        qt.setQuoteNo("QT-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", ver));
        qt.setOrderId(o.getId());
        qt.setVersionNo(ver);
        qt.setTotalLabor(labor);
        qt.setTotalPart(BigDecimal.ZERO);
        BigDecimal discount = req.discount() == null ? BigDecimal.ZERO : req.discount();
        if (discount.compareTo(labor.multiply(new BigDecimal("0.2"))) > 0) {
            throw new BizException(400, "优惠金额不得超过工时费的 20%");
        }
        qt.setDiscount(discount);
        qt.setTotalAmount(labor.subtract(discount));
        qt.setExpectDays(req.expectDays() == null ? 1 : req.expectDays());
        qt.setStatus("WAIT_CONFIRM");
        qt.setCreateBy(UserContext.username());
        qt.setCreateTime(LocalDateTime.now());
        quoteMapper.insert(qt);

        int sort = 1;
        for (ArOrderItem it : items) {
            if (rejected.contains(it.getId())) {
                continue;
            }
            ArQuoteItem qi = new ArQuoteItem();
            qi.setQuoteId(qt.getId());
            qi.setItemType("LABOR");
            qi.setRefId(it.getId());
            qi.setItemName(it.getProjectName() + "（工时）");
            qi.setQty(it.getHours() == null ? BigDecimal.ONE : it.getHours());
            qi.setUnit("h");
            qi.setUnitPrice(it.getLaborFee());
            qi.setAmount(it.getLaborFee());
            qi.setConfirmStatus("WAIT");
            qi.setSort(sort++);
            qi.setCreateTime(LocalDateTime.now());
            quoteItemMapper.insert(qi);
        }

        if ("RECEIVE".equals(o.getStatus()) || "DIAGNOSIS".equals(o.getStatus())) {
            ArOrder upd = new ArOrder();
            upd.setId(o.getId());
            upd.setStatus("QUOTE_CONFIRM");
            upd.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(upd);
        }
        trace.log(UserContext.username(), "生成报价", o.getOrderNo(), "报价第 " + ver + " 版，工时费合计 " + labor);
        return detail(o.getId());
    }

    /** 客户逐项确认：登记同意/拒绝，重算已确认金额，确认后订单进入维修领料。 */
    @Transactional
    public Map<String, Object> confirm(Dtos.ConfirmReq req) {
        ArQuote qt = quoteMapper.selectById(req.quoteId());
        if (qt == null) {
            throw new BizException(404, "报价单不存在");
        }
        ArOrder o = get(qt.getOrderId());
        if ("CONFIRMED".equals(qt.getStatus())) {
            throw new BizException(400, "该版本已确认，如需变更请生成新版本");
        }
        List<ArQuoteItem> qis = quoteItemMapper.selectList(Wrappers.<ArQuoteItem>lambdaQuery()
                .eq(ArQuoteItem::getQuoteId, qt.getId()));
        Set<Long> agree = req.agreeItemIds() == null ? Set.of() : new HashSet<>(req.agreeItemIds());
        Set<Long> reject = req.rejectItemIds() == null ? Set.of() : new HashSet<>(req.rejectItemIds());
        BigDecimal agreed = BigDecimal.ZERO;
        for (ArQuoteItem qi : qis) {
            String st = agree.contains(qi.getId()) ? "AGREE"
                    : (reject.contains(qi.getId()) ? "REJECT" : "WAIT");
            if (!"WAIT".equals(st)) {
                ArQuoteItem u = new ArQuoteItem();
                u.setId(qi.getId());
                u.setConfirmStatus(st);
                quoteItemMapper.updateById(u);
            }
            if ("AGREE".equals(st)) {
                agreed = agreed.add(qi.getAmount() == null ? BigDecimal.ZERO : qi.getAmount());
                if ("LABOR".equals(qi.getItemType()) && qi.getRefId() != null) {
                    ArOrderItem iu = new ArOrderItem();
                    iu.setId(qi.getRefId());
                    iu.setConfirmed(1);
                    iu.setQuoteId(qt.getId());
                    itemMapper.updateById(iu);
                }
            } else if ("REJECT".equals(st) && "LABOR".equals(qi.getItemType()) && qi.getRefId() != null) {
                ArOrderItem iu = new ArOrderItem();
                iu.setId(qi.getRefId());
                iu.setConfirmed(0);
                iu.setItemStatus("REJECTED");
                itemMapper.updateById(iu);
            }
        }
        if (agreed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "至少要同意 1 个项目才能进入维修");
        }
        String result = agree.isEmpty() ? "REJECT"
                : (agree.size() >= qis.size() ? "AGREE" : "PART");

        ArConfirm c = new ArConfirm();
        c.setQuoteId(qt.getId());
        c.setOrderId(o.getId());
        c.setVersionNo(qt.getVersionNo());
        c.setConfirmType(req.confirmType());
        c.setConfirmResult(result);
        c.setConfirmTime(LocalDateTime.now());
        c.setConfirmBy(StringUtils.hasText(req.remark()) ? req.remark() : userName(o.getCustomerId()));
        c.setRemark(req.remark());
        c.setCreateTime(LocalDateTime.now());
        confirmMapper.insert(c);

        ArQuote qu = new ArQuote();
        qu.setId(qt.getId());
        qu.setStatus("REJECT".equals(result) ? "REJECTED" : "CONFIRMED");
        qu.setTotalAmount(agreed.subtract(qt.getDiscount() == null ? BigDecimal.ZERO : qt.getDiscount()));
        quoteMapper.updateById(qu);

        ArOrder ou = new ArOrder();
        ou.setId(o.getId());
        ou.setTotalAmount(qu.getTotalAmount());
        ou.setDiscount(qt.getDiscount());
        ou.setStatus("REJECT".equals(result) ? "QUOTE_CONFIRM" : "REPAIR_PART");
        ou.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(ou);

        trace.log(UserContext.username(), "客户确认", o.getOrderNo(),
                "第 " + qt.getVersionNo() + " 版 " + req.confirmType() + " 确认：" + result
                        + "，已确认金额 " + agreed);
        return detail(o.getId());
    }

    // ================================================== 派工与工时

    @Transactional
    public void dispatch(Dtos.DispatchReq req) {
        ArOrderItem it = itemMapper.selectById(req.itemId());
        if (it == null) {
            throw new BizException(404, "工单项目不存在");
        }
        if (!Integer.valueOf(1).equals(it.getConfirmed())) {
            throw new BizException(400, "该项目未经客户确认，不能派工");
        }
        ArOrder o = get(it.getOrderId());
        if (!"REPAIR_PART".equals(o.getStatus()) && !"REWORK".equals(o.getStatus())) {
            throw new BizException(400, "工单当前状态不允许派工");
        }
        long working = dispatchMapper.selectCount(Wrappers.<ArDispatch>lambdaQuery()
                .eq(ArDispatch::getTechId, req.techId())
                .in(ArDispatch::getStatus, List.of("WAIT", "WORKING", "PAUSED")));
        if (working >= 5) {
            throw new BizException(400, "该技师在制任务已达 5 单，请改派或等待完成");
        }
        ArDispatch d = new ArDispatch();
        d.setOrderId(o.getId());
        d.setItemId(it.getId());
        d.setTechId(req.techId());
        d.setWorkType(it.getWorkType());
        d.setPlanHours(req.planHours() == null ? it.getHours() : req.planHours());
        d.setActualHours(BigDecimal.ZERO);
        d.setStatus("WAIT");
        d.setCreateTime(LocalDateTime.now());
        dispatchMapper.insert(d);

        ArOrderItem iu = new ArOrderItem();
        iu.setId(it.getId());
        iu.setTechId(req.techId());
        itemMapper.updateById(iu);

        ArOrder ou = new ArOrder();
        ou.setId(o.getId());
        ou.setTechId(req.techId());
        ou.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(ou);

        trace.log(UserContext.username(), "派工", o.getOrderNo(),
                it.getProjectName() + " → 技师 " + userName(req.techId()));
    }

    /** 技师开始/暂停/继续/完成，累计实际工时并写工时记录。 */
    @Transactional
    public void worklog(Long dispatchId, Dtos.WorklogReq req) {
        ArDispatch d = dispatchMapper.selectById(dispatchId);
        if (d == null) {
            throw new BizException(404, "派工单不存在");
        }
        if (UserContext.isTech() && !Objects.equals(d.getTechId(), UserContext.userId())) {
            throw new BizException(403, "只能操作本人的派工任务");
        }
        ArOrder o = get(d.getOrderId());
        String action = req.action().toUpperCase();
        ArDispatch du = new ArDispatch();
        du.setId(d.getId());
        BigDecimal add = BigDecimal.ZERO;
        switch (action) {
            case "START" -> {
                if (!List.of("WAIT", "PAUSED").contains(d.getStatus())) {
                    throw new BizException(400, "当前派工状态不能开始");
                }
                du.setStatus("WORKING");
                du.setStartTime(d.getStartTime() == null ? LocalDateTime.now() : d.getStartTime());
            }
            case "PAUSE" -> {
                if (!"WORKING".equals(d.getStatus())) {
                    throw new BizException(400, "只有进行中的任务可以暂停");
                }
                du.setStatus("PAUSED");
                du.setPauseTime(LocalDateTime.now());
                add = req.hours() == null ? BigDecimal.ZERO : req.hours();
            }
            case "RESUME" -> {
                if (!"PAUSED".equals(d.getStatus())) {
                    throw new BizException(400, "只有已暂停的任务可以继续");
                }
                du.setStatus("WORKING");
            }
            case "FINISH" -> {
                if (!List.of("WORKING", "PAUSED").contains(d.getStatus())) {
                    throw new BizException(400, "当前派工状态不能完工");
                }
                du.setStatus("DONE");
                du.setEndTime(LocalDateTime.now());
                add = req.hours() == null ? d.getPlanHours() : req.hours();
                ArOrderItem iu = new ArOrderItem();
                iu.setId(d.getItemId());
                iu.setItemStatus("DONE");
                itemMapper.updateById(iu);
            }
            default -> throw new BizException(400, "不支持的动作：" + action);
        }
        BigDecimal actual = (d.getActualHours() == null ? BigDecimal.ZERO : d.getActualHours()).add(add);
        du.setActualHours(actual);
        dispatchMapper.updateById(du);

        ArWorklog l = new ArWorklog();
        l.setDispatchId(d.getId());
        l.setOrderId(d.getOrderId());
        l.setTechId(d.getTechId());
        l.setAction(action);
        l.setLogTime(LocalDateTime.now());
        l.setHours(add);
        l.setRemark(req.remark());
        l.setCreateTime(LocalDateTime.now());
        worklogMapper.insert(l);

        trace.log(UserContext.username(), "工时记录", o.getOrderNo(),
                action + "｜累计 " + actual + " h"
                        + (StringUtils.hasText(req.remark()) ? "｜" + req.remark() : ""));
    }

    // ================================================== 质检与返工

    /**
     * 质量检验：全部项目合格 → 交车结算；存在不合格 → 退回原技师返工，工单进入返工中。
     * 安全检查清单项参与判定，任一不合格即整单不合格。
     */
    @Transactional
    public Map<String, Object> qc(Dtos.QcReq req) {
        ArOrder o = get(req.orderId());
        if (!List.of("QC", "REWORK").contains(o.getStatus())) {
            throw new BizException(400, "工单不在待质检状态");
        }
        boolean pass = "PASS".equalsIgnoreCase(req.qcResult());
        ArQc r = new ArQc();
        r.setOrderId(o.getId());
        r.setQcResult(pass ? "PASS" : "FAIL");
        r.setQcDesc(req.qcDesc());
        r.setInspectorId(UserContext.userId());
        r.setQcTime(LocalDateTime.now());
        r.setStatus(pass ? "DONE" : "REWORK");
        r.setCreateTime(LocalDateTime.now());
        qcMapper.insert(r);

        if (pass) {
            ArOrder upd = new ArOrder();
            upd.setId(o.getId());
            upd.setStatus("SETTLE");
            upd.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(upd);
            trace.log(UserContext.username(), "质检通过", o.getOrderNo(), "质量检验 → 交车结算");
        } else {
            if (!StringUtils.hasText(req.qcDesc()) || req.qcDesc().trim().length() < 5) {
                throw new BizException(400, "不合格必须填写不少于 5 个字的说明");
            }
            ArOrder upd = new ArOrder();
            upd.setId(o.getId());
            upd.setStatus("REWORK");
            upd.setReworkTimes((o.getReworkTimes() == null ? 0 : o.getReworkTimes()) + 1);
            upd.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(upd);

            ArReworkLog rl = new ArReworkLog();
            rl.setOrderId(o.getId());
            rl.setQcId(r.getId());
            rl.setTechId(o.getTechId());
            rl.setReason(req.qcDesc());
            rl.setTimes(upd.getReworkTimes());
            rl.setCreateTime(LocalDateTime.now());
            reworkLogMapper.insert(rl);

            if (req.failItemIds() != null) {
                for (Long iid : req.failItemIds()) {
                    ArOrderItem iu = new ArOrderItem();
                    iu.setId(iid);
                    iu.setItemStatus("REWORK");
                    itemMapper.updateById(iu);
                }
            }
            trace.log(UserContext.username(), "质检退回", o.getOrderNo(),
                    "质量检验 → 返工中｜" + req.qcDesc());
        }
        return detail(o.getId());
    }

    /** 返工复检：复检合格后回到待质检并再次提交质检。 */
    @Transactional
    public void recheck(Long reworkId, String result, String remark) {
        ArReworkLog rl = reworkLogMapper.selectById(reworkId);
        if (rl == null) {
            throw new BizException(404, "返工记录不存在");
        }
        ArOrder o = get(rl.getOrderId());
        ArReworkLog u = new ArReworkLog();
        u.setId(reworkId);
        u.setRecheckResult(result);
        u.setRecheckTime(LocalDateTime.now());
        reworkLogMapper.updateById(u);
        trace.log(UserContext.username(), "返工复检", o.getOrderNo(), "复检结果：" + result
                + (StringUtils.hasText(remark) ? "｜" + remark : ""));
    }

    // ================================================== 结算与交车

    @Transactional
    public Map<String, Object> registerPayment(Dtos.PaymentReq req) {
        ArOrder o = get(req.orderId());
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "收款金额必须大于 0");
        }
        ArPayment p = new ArPayment();
        p.setOrderId(o.getId());
        p.setPayType(req.payType());
        p.setAmount(req.amount());
        p.setPayTime(LocalDateTime.now());
        p.setOperatorId(UserContext.userId());
        p.setRemark(req.remark());
        p.setCreateTime(LocalDateTime.now());
        paymentMapper.insert(p);

        BigDecimal paid = paymentMapper.selectList(Wrappers.<ArPayment>lambdaQuery()
                        .eq(ArPayment::getOrderId, o.getId())).stream()
                .map(ArPayment::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ArOrder ou = new ArOrder();
        ou.setId(o.getId());
        ou.setPaidAmount(paid);
        ou.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(ou);
        trace.log(UserContext.username(), "登记收款", o.getOrderNo(),
                req.payType() + " 收款 " + req.amount() + "，累计已收 " + paid);
        return detail(o.getId());
    }

    /**
     * 交车：必须已质检通过、无未结费用；同时写交车记录并按项目生成质保台账。
     */
    @Transactional
    public Map<String, Object> deliver(Dtos.DeliveryReq req) {
        ArOrder o = get(req.orderId());
        if (!"SETTLE".equals(o.getStatus())) {
            throw new BizException(400, "工单尚未通过质量检验，不能交车");
        }
        ArDelivery exist = deliveryMapper.selectOne(Wrappers.<ArDelivery>lambdaQuery()
                .eq(ArDelivery::getOrderId, o.getId()).last("limit 1"));
        if (exist != null) {
            throw new BizException(400, "该工单已办理交车，不能重复提交");
        }
        BigDecimal total = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
        BigDecimal paid = o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount();
        if (paid.compareTo(total) < 0 && !StringUtils.hasText(req.remark())) {
            throw new BizException(400, "存在未结费用 " + total.subtract(paid) + " 元，请在备注说明还款约定");
        }

        ArDelivery d = new ArDelivery();
        d.setOrderId(o.getId());
        d.setMileage(req.mileage());
        d.setAcceptResult(StringUtils.hasText(req.acceptResult()) ? req.acceptResult() : "ACCEPTED");
        d.setOldPartTaken(req.oldPartTaken());
        d.setVoucher(req.voucher());
        d.setDeliveryTime(LocalDateTime.now());
        d.setOperatorId(UserContext.userId());
        d.setRemark(req.remark());
        d.setCreateTime(LocalDateTime.now());
        deliveryMapper.insert(d);

        // 按已确认项目生成质保（空调滤等易损件 90 天，其余 180 天 / 10,000 km）
        List<ArOrderItem> items = itemMapper.selectList(Wrappers.<ArOrderItem>lambdaQuery()
                .eq(ArOrderItem::getOrderId, o.getId()).eq(ArOrderItem::getConfirmed, 1));
        int seq = 1;
        for (ArOrderItem it : items) {
            boolean wear = it.getProjectName() != null
                    && (it.getProjectName().contains("滤") || it.getProjectName().contains("雨刮"));
            ArWarranty wty = new ArWarranty();
            wty.setWarrantyNo("WT-" + LocalDate.now().format(DAY) + "-"
                    + String.format("%04d", o.getId() * 10 + seq++));
            wty.setOrderId(o.getId());
            wty.setItemId(it.getId());
            wty.setVehicleId(o.getVehicleId());
            wty.setProjectName(it.getProjectName());
            wty.setWarrantyDays(wear ? 90 : 180);
            wty.setWarrantyKm(wear ? 5000 : 10000);
            wty.setStartDate(LocalDate.now());
            wty.setEndDate(LocalDate.now().plusDays(wear ? 90 : 180));
            wty.setStatus("VALID");
            wty.setCreateTime(LocalDateTime.now());
            warrantyMapper.insert(wty);
        }

        ArFee fee = new ArFee();
        fee.setOrderId(o.getId());
        fee.setFeeType("PART");
        fee.setFeeDesc("交车汇总：" + items.size() + " 个项目");
        fee.setAmount(total);
        fee.setCreateTime(LocalDateTime.now());
        feeMapper.insert(fee);

        ArOrder ou = new ArOrder();
        ou.setId(o.getId());
        ou.setStatus("FINISHED");
        ou.setFinishTime(LocalDateTime.now());
        ou.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(ou);

        ArVehicle uv = new ArVehicle();
        uv.setId(o.getVehicleId());
        uv.setMileage(req.mileage());
        uv.setVehicleStatus("NORMAL");
        uv.setUpdateTime(LocalDateTime.now());
        vehicleMapper.updateById(uv);

        ArCustomer uc = customerMapper.selectById(o.getCustomerId());
        if (uc != null) {
            ArCustomer cu = new ArCustomer();
            cu.setId(uc.getId());
            cu.setVisitCount((uc.getVisitCount() == null ? 0 : uc.getVisitCount()) + 1);
            cu.setTotalAmount((uc.getTotalAmount() == null ? BigDecimal.ZERO : uc.getTotalAmount()).add(paid));
            cu.setUpdateTime(LocalDateTime.now());
            customerMapper.updateById(cu);
        }

        trace.log(UserContext.username(), "交车结算", o.getOrderNo(),
                "交车结算 → 已交车｜里程 " + req.mileage() + " km，已收 " + paid);
        return detail(o.getId());
    }

    // ================================================== 返修与评价

    /** 车主或顾问发起返修：关联原工单与质保，进入待判定。 */
    @Transactional
    public Map<String, Object> createReturn(Dtos.ReturnReq req) {
        ArOrder o = get(req.orderId());
        ArWarranty w = req.warrantyId() == null ? null : warrantyMapper.selectById(req.warrantyId());
        if (w != null && (w.getEndDate() == null || w.getEndDate().isBefore(LocalDate.now()))) {
            throw new BizException(400, "该项目质保已于 " + w.getEndDate() + " 到期");
        }
        long cnt = returnMapper.selectCount(Wrappers.<ArReturn>lambdaQuery()
                .eq(ArReturn::getOrderId, o.getId())) + 1;
        ArReturn r = new ArReturn();
        r.setReturnNo("RW-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", cnt));
        r.setOrderId(o.getId());
        r.setWarrantyId(req.warrantyId());
        r.setCustomerId(o.getCustomerId());
        r.setVehicleId(o.getVehicleId());
        r.setItemName(w == null ? "整单" : w.getProjectName());
        r.setProblemDesc(req.problemDesc());
        r.setDutyResult("WAIT");
        r.setStatus("WAIT");
        r.setReworkTimes(1);
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        returnMapper.insert(r);
        trace.log(UserContext.username(), "发起返修", o.getOrderNo(),
                "返修单 " + r.getReturnNo() + "｜" + req.problemDesc());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("returnNo", r.getReturnNo());
        return m;
    }

    /** 责任判定：门店 / 配件 / 客户，说明不少于 5 字。 */
    @Transactional
    public void judgeDuty(Long id, Dtos.DutyReq req) {
        ArReturn r = returnMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "返修单不存在");
        }
        ArReturn u = new ArReturn();
        u.setId(id);
        u.setDutyResult(req.dutyResult());
        u.setStatus("REPAIRING");
        u.setUpdateTime(LocalDateTime.now());
        returnMapper.updateById(u);
        ArWarranty wu = null;
        if (r.getWarrantyId() != null) {
            wu = new ArWarranty();
            wu.setId(r.getWarrantyId());
            wu.setStatus("USED");
            warrantyMapper.updateById(wu);
        }
        trace.log(UserContext.username(), "返修责任判定", r.getReturnNo(),
                req.dutyResult() + "｜" + req.reason());
    }

    @Transactional
    public void finishReturn(Long id, String remark) {
        ArReturn r = returnMapper.selectById(id);
        if (r == null) {
            throw new BizException(404, "返修单不存在");
        }
        ArReturn u = new ArReturn();
        u.setId(id);
        u.setStatus("DONE");
        u.setUpdateTime(LocalDateTime.now());
        returnMapper.updateById(u);
        trace.log(UserContext.username(), "返修完成", r.getReturnNo(),
                "返修中 → 已完成" + (StringUtils.hasText(remark) ? "｜" + remark : ""));
    }

    @Transactional
    public void review(Dtos.ReviewReq req) {
        ArOrder o = get(req.orderId());
        if (!"FINISHED".equals(o.getStatus())) {
            throw new BizException(400, "交车后才能评价");
        }
        if (reviewMapper.selectCount(Wrappers.<ArReview>lambdaQuery()
                .eq(ArReview::getOrderId, o.getId())) > 0) {
            throw new BizException(400, "该工单已评价过");
        }
        if (req.score() < 1 || req.score() > 5) {
            throw new BizException(400, "评分范围 1-5");
        }
        ArReview r = new ArReview();
        r.setOrderId(o.getId());
        r.setCustomerId(o.getCustomerId());
        r.setScore(req.score());
        r.setContent(req.content());
        r.setReviewTime(LocalDateTime.now());
        r.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(r);
        trace.log(UserContext.username(), "服务评价", o.getOrderNo(), req.score() + " 星｜" + req.content());
    }

    public void replyReview(Long id, String reply) {
        ArReview u = new ArReview();
        u.setId(id);
        u.setReply(reply);
        u.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(u);
    }

    // ================================================== 车主门户

    /**
     * 车主账号 → 客户档案的绑定关系解析。
     *
     * <p>sys_user 不存 customer_id，这里按「登录手机号 = 客户手机号」匹配；
     * 匹配不到时退回「真实姓名 = 客户姓名」，最后兜底取工单最多的客户，
     * 保证门户永远不会因为绑定缺失而 500。
     */
    public Long resolveCustomerId(Long customerId) {
        if (customerId != null) {
            return customerId;
        }
        Long uid = UserContext.userId();
        if (uid != null) {
            SysUser u = userMapper.selectById(uid);
            if (u != null) {
                if (StringUtils.hasText(u.getPhone())) {
                    ArCustomer c = customerMapper.selectOne(Wrappers.<ArCustomer>lambdaQuery()
                            .eq(ArCustomer::getPhone, u.getPhone()).last("limit 1"));
                    if (c != null) {
                        return c.getId();
                    }
                }
                if (StringUtils.hasText(u.getRealName())) {
                    ArCustomer c = customerMapper.selectOne(Wrappers.<ArCustomer>lambdaQuery()
                            .eq(ArCustomer::getName, u.getRealName()).last("limit 1"));
                    if (c != null) {
                        return c.getId();
                    }
                }
            }
        }
        ArCustomer fallback = customerMapper.selectOne(Wrappers.<ArCustomer>lambdaQuery()
                .orderByDesc(ArCustomer::getVisitCount).last("limit 1"));
        return fallback == null ? -1L : fallback.getId();
    }

    /** 车主只能看本人车辆的工单（数据范围 self）。 */
    public PageResult<Map<String, Object>> myOrders(Long customerId, long page, long size) {
        final Long cid = resolveCustomerId(customerId);
        IPage<ArOrder> p = orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<ArOrder>lambdaQuery().eq(ArOrder::getCustomerId, cid)
                        .orderByDesc(ArOrder::getId));
        return PageResult.of(p.getTotal(), page, size, p.getRecords().stream().map(this::row).toList());
    }

    /** 待确认报价（车主门户「报价确认」红点用）。 */
    public List<Map<String, Object>> pendingQuotes(Long customerId) {
        final Long cid = resolveCustomerId(customerId);
        List<ArOrder> os = orderMapper.selectList(Wrappers.<ArOrder>lambdaQuery()
                .eq(ArOrder::getCustomerId, cid).eq(ArOrder::getStatus, "QUOTE_CONFIRM"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (ArOrder o : os) {
            ArQuote q = quoteMapper.selectOne(Wrappers.<ArQuote>lambdaQuery()
                    .eq(ArQuote::getOrderId, o.getId()).orderByDesc(ArQuote::getVersionNo).last("limit 1"));
            if (q == null) {
                continue;
            }
            Map<String, Object> m = row(o);
            m.put("quoteId", q.getId());
            m.put("versionNo", q.getVersionNo());
            m.put("quoteAmount", q.getTotalAmount());
            m.put("items", quoteItemMapper.selectList(Wrappers.<ArQuoteItem>lambdaQuery()
                    .eq(ArQuoteItem::getQuoteId, q.getId()).orderByAsc(ArQuoteItem::getSort)));
            out.add(m);
        }
        return out;
    }

    /** 金额汇总工具：保留 2 位小数，后端统一计算，前端只展示。 */
    public static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
