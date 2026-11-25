package com.grad.repair.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grad.repair.entity.*;
import com.grad.repair.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * 维修经营分析看板。
 *
 * <p>需求文档要求「统计值不在前端二次聚合全表」，因此所有指标与图表数据都在后端算好；
 * 首屏 6 个核心指标 + 6 个图表，按 1440×900 一屏设计。</p>
 *
 * <p>核心指标：进厂台次、平均维修时长、报价确认率、一次检验通过率、配件消耗、技师工时；
 * 图表：进厂趋势、工单状态分布、项目收入构成、技师工时排行、配件消耗排行、返修原因分布。</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ArOrderMapper orderMapper;
    private final ArOrderItemMapper itemMapper;
    private final ArPartFlowMapper flowMapper;
    private final ArPartMapper partMapper;
    private final ArQuoteMapper quoteMapper;
    private final ArQcMapper qcMapper;
    private final ArReturnMapper returnMapper;
    private final ArScoreMapper scoreMapper;
    private final ArDispatchMapper dispatchMapper;
    private final SysUserMapper userMapper;

    private List<ArOrder> ordersIn(LocalDateTime from, LocalDateTime to) {
        return orderMapper.selectList(Wrappers.<ArOrder>lambdaQuery()
                .ge(ArOrder::getReceiveTime, from).le(ArOrder::getReceiveTime, to));
    }

    public Map<String, Object> overview(String range) {
        LocalDate today = LocalDate.now();
        LocalDateTime from;
        LocalDateTime to = LocalDateTime.now();
        switch (range == null ? "30d" : range) {
            case "today" -> from = today.atStartOfDay();
            case "month" -> from = today.withDayOfMonth(1).atStartOfDay();
            case "quarter" -> from = today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1)
                    .withDayOfMonth(1).atStartOfDay();
            case "year" -> from = today.withDayOfYear(1).atStartOfDay();
            default -> from = today.minusDays(29).atStartOfDay();
        }
        List<ArOrder> scoped = ordersIn(from, to);
        List<ArOrder> all = orderMapper.selectList(null);

        long todayIn = ordersIn(today.atStartOfDay(), to).size();
        long inRepair = all.stream().filter(o -> List.of("REPAIR_PART", "REWORK", "QC").contains(o.getStatus())).count();
        long waitConfirm = all.stream().filter(o -> "QUOTE_CONFIRM".equals(o.getStatus())).count();
        long waitQc = all.stream().filter(o -> "QC".equals(o.getStatus())).count();
        long stockWarn = partMapper.selectCount(Wrappers.<ArPart>lambdaQuery().apply("stock_qty <= min_qty"));
        BigDecimal monthAmount = all.stream()
                .filter(o -> o.getReceiveTime() != null
                        && o.getReceiveTime().isAfter(today.withDayOfMonth(1).atStartOfDay()))
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 平均维修时长：已完工工单的接车→完工小时数
        List<ArOrder> finished = scoped.stream()
                .filter(o -> o.getFinishTime() != null && o.getReceiveTime() != null).toList();
        double avgHours = finished.isEmpty() ? 0 : finished.stream()
                .mapToDouble(o -> java.time.Duration.between(o.getReceiveTime(), o.getFinishTime()).toMinutes() / 60.0)
                .average().orElse(0);

        // 报价确认率：已确认/已拒绝版本 ÷ 全部报价版本
        List<ArQuote> quotes = quoteMapper.selectList(Wrappers.<ArQuote>lambdaQuery()
                .ge(ArQuote::getCreateTime, from));
        long confirmed = quotes.stream().filter(q -> !"WAIT_CONFIRM".equals(q.getStatus())).count();

        // 一次检验通过率：首次质检即通过的工单占比
        List<ArQc> qcs = qcMapper.selectList(Wrappers.<ArQc>lambdaQuery().ge(ArQc::getQcTime, from)
                .orderByAsc(ArQc::getId));
        Map<Long, List<ArQc>> byOrder = new LinkedHashMap<>();
        qcs.forEach(q -> byOrder.computeIfAbsent(q.getOrderId(), k -> new ArrayList<>()).add(q));
        long passFirst = byOrder.values().stream()
                .filter(l -> "PASS".equals(l.get(0).getQcResult())).count();

        // 配件消耗：期内已发料领用金额（数量×销售价）
        List<ArPartFlow> flows = flowMapper.selectList(Wrappers.<ArPartFlow>lambdaQuery()
                .ge(ArPartFlow::getFlowTime, from).eq(ArPartFlow::getStatus, "ISSUED"));
        BigDecimal partAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> partRank = new LinkedHashMap<>();
        for (ArPartFlow f : flows) {
            ArPart p = partMapper.selectById(f.getPartId());
            if (p == null) {
                continue;
            }
            BigDecimal amt = (p.getSalePrice() == null ? BigDecimal.ZERO : p.getSalePrice())
                    .multiply(f.getQty() == null ? BigDecimal.ONE : f.getQty());
            partAmount = partAmount.add(amt);
            partRank.merge(p.getPartName(), f.getQty() == null ? BigDecimal.ONE : f.getQty(), BigDecimal::add);
        }

        // 技师工时排行
        List<ArDispatch> dispatches = dispatchMapper.selectList(Wrappers.<ArDispatch>lambdaQuery()
                .isNotNull(ArDispatch::getActualHours));
        Map<Long, BigDecimal> techHours = new LinkedHashMap<>();
        for (ArDispatch d : dispatches) {
            techHours.merge(d.getTechId(), d.getActualHours() == null ? BigDecimal.ZERO : d.getActualHours(),
                    BigDecimal::add);
        }
        List<Map<String, Object>> techRank = techHours.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue())).limit(6).map(e -> {
                    SysUser u = userMapper.selectById(e.getKey());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", u == null ? "—" : u.getRealName());
                    m.put("hours", e.getValue().setScale(1, RoundingMode.HALF_UP));
                    return m;
                }).toList();

        // 进厂趋势：按天
        Map<String, Long> trend = new LinkedHashMap<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            trend.put(d.toString().substring(5), ordersIn(d.atStartOfDay(), d.plusDays(1).atStartOfDay()).size() * 1L);
        }

        // 状态分布
        Map<String, Long> statusDist = new LinkedHashMap<>();
        OrderService.STATUS_NAME.forEach((k, v) -> statusDist.put(v,
                all.stream().filter(o -> k.equals(o.getStatus())).count()));

        // 项目收入构成：按工种汇总工时费
        Map<String, BigDecimal> income = new LinkedHashMap<>();
        List<ArOrderItem> items = itemMapper.selectList(Wrappers.<ArOrderItem>lambdaQuery()
                .eq(ArOrderItem::getConfirmed, 1));
        for (ArOrderItem it : items) {
            String t = switch (it.getWorkType() == null ? "MECHANIC" : it.getWorkType()) {
                case "ELECTRIC" -> "电控";
                case "SHEET_METAL" -> "钣金";
                case "PAINT" -> "喷漆";
                default -> "机修";
            };
            income.merge(t, it.getLaborFee() == null ? BigDecimal.ZERO : it.getLaborFee(), BigDecimal::add);
        }

        // 返修原因分布：返工记录按原因前缀归类
        Map<String, Long> reworkReason = new LinkedHashMap<>();
        for (ArReworkLog r : returnMapperList()) {
            String key = r.getReason() == null ? "其他" :
                    (r.getReason().contains("装配") ? "装配不当"
                            : r.getReason().contains("配件") ? "配件质量"
                            : r.getReason().contains("遗漏") ? "诊断遗漏" : "其他");
            reworkReason.merge(key, 1L, Long::sum);
        }

        // 预检结果分布（创新模块）
        List<ArScore> scores = scoreMapper.selectList(null);
        Map<String, Long> precheck = new LinkedHashMap<>();
        precheck.put("推荐领用", scores.stream().filter(s -> "RECOMMEND".equals(s.getResultLevel())).count());
        precheck.put("谨慎处理", scores.stream().filter(s -> "CAUTION".equals(s.getResultLevel())).count());
        precheck.put("不推荐", scores.stream().filter(s -> "REJECT".equals(s.getResultLevel())).count());

        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("todayIn", todayIn);
        kpi.put("inRepair", inRepair);
        kpi.put("waitConfirm", waitConfirm);
        kpi.put("waitQc", waitQc);
        kpi.put("stockWarn", stockWarn);
        kpi.put("monthAmount", monthAmount.setScale(2, RoundingMode.HALF_UP));
        kpi.put("avgHours", Math.round(avgHours * 10) / 10.0);
        kpi.put("confirmRate", quotes.isEmpty() ? 0
                : Math.round(confirmed * 1000.0 / quotes.size()) / 10.0);
        kpi.put("firstPassRate", byOrder.isEmpty() ? 0
                : Math.round(passFirst * 1000.0 / byOrder.size()) / 10.0);
        kpi.put("partAmount", partAmount.setScale(2, RoundingMode.HALF_UP));
        kpi.put("techHours", techHours.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP));
        kpi.put("returnRate", all.isEmpty() ? 0
                : Math.round(returnMapper.selectCount(null) * 1000.0 / all.size()) / 10.0);
        m.put("kpi", kpi);
        m.put("trend", trend);
        m.put("statusDist", statusDist);
        m.put("income", income);
        m.put("techRank", techRank);
        m.put("partRank", partRank.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue())).limit(6)
                .map(e -> Map.of("name", e.getKey(), "qty", e.getValue())).toList());
        m.put("reworkReason", reworkReason);
        m.put("precheck", precheck);
        m.put("range", range == null ? "30d" : range);
        return m;
    }

    /** 返修原因取返工记录（质检退回）。 */
    private List<ArReworkLog> returnMapperList() {
        return reworkLogMapper.selectList(Wrappers.<ArReworkLog>lambdaQuery().orderByDesc(ArReworkLog::getId));
    }

    private final ArReworkLogMapper reworkLogMapper;

    /** 首页待办：按当前角色返回需要处理的事项。 */
    public List<Map<String, Object>> todo() {
        List<Map<String, Object>> out = new ArrayList<>();
        List<ArOrder> list = orderMapper.selectList(Wrappers.<ArOrder>lambdaQuery()
                .in(ArOrder::getStatus, List.of("QUOTE_CONFIRM", "QC", "REWORK", "SETTLE"))
                .orderByDesc(ArOrder::getUpdateTime).last("limit 8"));
        for (ArOrder o : list) {
            ArVehicle v = null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orderNo", o.getOrderNo());
            m.put("id", o.getId());
            m.put("status", o.getStatus());
            m.put("statusName", OrderService.STATUS_NAME.getOrDefault(o.getStatus(), o.getStatus()));
            m.put("updateTime", o.getUpdateTime());
            m.put("expectFinish", o.getExpectFinish());
            m.put("item", switch (o.getStatus()) {
                case "QUOTE_CONFIRM" -> "等待客户逐项确认报价";
                case "QC" -> "完工待质检";
                case "REWORK" -> "质检不合格，等待返工复检";
                case "SETTLE" -> "质检通过，等待结算交车";
                default -> "处理中";
            });
            m.put("priority", List.of("REWORK", "QUOTE_CONFIRM").contains(o.getStatus()) ? "HIGH" : "NORMAL");
            out.add(m);
        }
        return out;
    }

    /** 近 6 个月产值趋势，供报表页使用。 */
    public List<Map<String, Object>> monthly() {
        List<Map<String, Object>> out = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            BigDecimal sum = orderMapper.selectList(Wrappers.<ArOrder>lambdaQuery()
                            .ge(ArOrder::getReceiveTime, ym.atDay(1).atStartOfDay())
                            .lt(ArOrder::getReceiveTime, ym.plusMonths(1).atDay(1).atStartOfDay()))
                    .stream().map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", ym.toString());
            m.put("amount", sum.setScale(2, RoundingMode.HALF_UP));
            out.add(m);
        }
        return out;
    }
}
