package com.grad.repair.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grad.repair.common.BizException;
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
 * 创新点：故障项目与配件需求预检。
 *
 * <p>定位是「可解释的业务辅助工具」，不替用户做不可逆决定，也不依赖外部大模型：
 * 只用 MySQL 历史工单、配件共现与当前库存，按可配置权重算出建议领用清单与等级，
 * 但最终领料仍需技师确认。</p>
 *
 * <p>六项因素（权重合计必须 100%）：
 * 车型 15 / 故障标签 20 / 诊断项目 20 / 历史工时 15 / 常用配件 20 / 当前库存 10。
 * 每次计算保存输入快照、规则版本、各项得分、总分与解释文本，可完整追溯；
 * 业务负责人可人工调整最终结果，但必须填写原因，且原始计算结果不得覆盖。</p>
 */
@Service
@RequiredArgsConstructor
public class PrecheckService {

    private final ArRuleMapper ruleMapper;
    private final ArScoreMapper scoreMapper;
    private final ArOrderMapper orderMapper;
    private final ArVehicleMapper vehicleMapper;
    private final ArOrderItemMapper itemMapper;
    private final ArInspectionMapper inspectionMapper;
    private final ArPartMapper partMapper;
    private final ArDemandMapper demandMapper;
    private final ArPartFlowMapper flowMapper;
    private final TraceService trace;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 故障标签 → 常见配件编码，用于「常用配件」因素与需求清单。 */
    private static final Map<String, List<String>> FAULT_PARTS = new LinkedHashMap<>();
    static {
        FAULT_PARTS.put("怠速抖动", List.of("PT-1001", "PT-1002", "PT-1008"));
        FAULT_PARTS.put("积碳", List.of("PT-1008", "PT-1006"));
        FAULT_PARTS.put("刹车异响", List.of("PT-1005", "PT-1007"));
        FAULT_PARTS.put("保养到期", List.of("PT-1001", "PT-1002", "PT-1004"));
        FAULT_PARTS.put("空调不冷", List.of("PT-1017", "PT-1004"));
        FAULT_PARTS.put("底盘异响", List.of("PT-1012"));
        FAULT_PARTS.put("车身划伤", List.of("PT-1014", "PT-1015", "PT-1016"));
        FAULT_PARTS.put("电瓶亏电", List.of("PT-1011"));
    }

    /** 项目 → 常用配件编码（诊断项目因素）。 */
    private static final Map<String, List<String>> PROJECT_PARTS = new LinkedHashMap<>();
    static {
        PROJECT_PARTS.put("更换机油机滤", List.of("PT-1001", "PT-1002"));
        PROJECT_PARTS.put("更换空调滤芯", List.of("PT-1004"));
        PROJECT_PARTS.put("清洗节气门", List.of("PT-1008"));
        PROJECT_PARTS.put("更换前刹车片", List.of("PT-1005"));
        PROJECT_PARTS.put("更换制动液", List.of("PT-1007"));
        PROJECT_PARTS.put("更换火花塞（4 支）", List.of("PT-1006"));
        PROJECT_PARTS.put("更换空气滤芯", List.of("PT-1003"));
        PROJECT_PARTS.put("更换雨刮片", List.of("PT-1009"));
        PROJECT_PARTS.put("更换电瓶", List.of("PT-1011"));
        PROJECT_PARTS.put("更换减震器", List.of("PT-1012"));
        PROJECT_PARTS.put("更换正时皮带", List.of("PT-1013"));
        PROJECT_PARTS.put("钣金修复（单幅）", List.of("PT-1016"));
        PROJECT_PARTS.put("喷漆（单幅）", List.of("PT-1014", "PT-1015"));
        PROJECT_PARTS.put("空调系统检漏加氟", List.of("PT-1017"));
    }

    // ================================================== 规则

    public List<ArRule> rules() {
        return ruleMapper.selectList(Wrappers.<ArRule>lambdaQuery().orderByDesc(ArRule::getId));
    }

    public ArRule activeRule() {
        ArRule r = ruleMapper.selectOne(Wrappers.<ArRule>lambdaQuery()
                .eq(ArRule::getStatus, 1).orderByDesc(ArRule::getId).last("limit 1"));
        if (r == null) {
            throw new BizException(500, "尚未配置预检规则，请先在规则配置页保存一版");
        }
        return r;
    }

    /** 保存新版本：权重合计必须等于 100%，旧版本转为历史，只影响新计算。 */
    @Transactional
    public ArRule saveRule(Dtos.RuleReq req) {
        BigDecimal sum = nz(req.wModel()).add(nz(req.wFault())).add(nz(req.wProject()))
                .add(nz(req.wHours())).add(nz(req.wPart())).add(nz(req.wStock()));
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            throw new BizException(400, "六项权重合计必须等于 100%，当前为 " + sum + "%");
        }
        long cnt = ruleMapper.selectCount(null);
        ArRule r = new ArRule();
        r.setRuleVersion("v" + (cnt + 1));
        r.setWModel(req.wModel());
        r.setWFault(req.wFault());
        r.setWProject(req.wProject());
        r.setWHours(req.wHours());
        r.setWPart(req.wPart());
        r.setWStock(req.wStock());
        r.setThresholdHigh(req.thresholdHigh() == null ? new BigDecimal("80") : req.thresholdHigh());
        r.setThresholdLow(req.thresholdLow() == null ? new BigDecimal("60") : req.thresholdLow());
        r.setStatus(1);
        r.setEffectTime(LocalDateTime.now());
        r.setCreateBy(UserContext.username());
        r.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(r);
        // 旧版本置为历史
        ruleMapper.update(null, Wrappers.<ArRule>lambdaUpdate()
                .set(ArRule::getStatus, 0).lt(ArRule::getId, r.getId()));
        trace.log(UserContext.username(), "更新预检规则", "ar_rule:" + r.getId(),
                "生成规则版本 " + r.getRuleVersion() + "，权重合计 100%");
        return r;
    }

    // ================================================== 计算

    /**
     * 执行预检：读取工单的车辆、检测结果、项目与库存，按当前规则算分并落快照。
     * 相同输入 + 相同规则版本必然得到相同结果（无随机、无外部依赖）。
     */
    @Transactional
    public Map<String, Object> evaluate(Long orderId) {
        ArOrder o = orderMapper.selectById(orderId);
        if (o == null) {
            throw new BizException(404, "工单不存在");
        }
        ArRule rule = activeRule();
        ArVehicle v = vehicleMapper.selectById(o.getVehicleId());
        List<ArInspection> ins = inspectionMapper.selectList(Wrappers.<ArInspection>lambdaQuery()
                .eq(ArInspection::getOrderId, orderId));
        List<ArOrderItem> items = itemMapper.selectList(Wrappers.<ArOrderItem>lambdaQuery()
                .eq(ArOrderItem::getOrderId, orderId));

        // ---- 1 车型：同品牌同车型历史工单越多，模板越可信
        long sameModel = v == null ? 0 : orderMapper.selectCount(Wrappers.<ArOrder>lambdaQuery()
                .eq(ArOrder::getVehicleId, v.getId()));
        long modelAll = v == null ? 1L : vehicleMapper.selectCount(Wrappers.<ArVehicle>lambdaQuery()
                .eq(ArVehicle::getModel, v.getModel()));
        BigDecimal scoreModel = rate(sameModel, Math.max(1, modelAll)).multiply(rule.getWModel());

        // ---- 2 故障标签：检测结论里出现建议维修/观察的条目占比
        long faultHit = ins.stream().filter(i -> !"NORMAL".equals(i.getCheckResult())).count();
        BigDecimal scoreFault = rate(faultHit, Math.max(1, ins.size())).multiply(rule.getWFault());

        // ---- 3 诊断项目：已命中项目库且有关联配件模板的占比
        long projHit = items.stream().filter(i -> PROJECT_PARTS.containsKey(i.getProjectName())).count();
        BigDecimal scoreProject = rate(projHit, Math.max(1, items.size())).multiply(rule.getWProject());

        // ---- 4 历史工时：同类项目历史均值与本次计划工时的偏离度（越接近越高）
        List<ArOrderItem> hist = itemMapper.selectList(Wrappers.<ArOrderItem>lambdaQuery()
                .in(ArOrderItem::getProjectName, items.stream().map(ArOrderItem::getProjectName).toList()));
        BigDecimal planHours = items.stream().map(ArOrderItem::getHours).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgHours = hist.isEmpty() ? planHours
                : hist.stream().map(ArOrderItem::getHours).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(hist.size()), 2, RoundingMode.HALF_UP);
        BigDecimal dev = planHours.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ONE
                : avgHours.subtract(planHours).abs().divide(planHours, 4, RoundingMode.HALF_UP);
        BigDecimal ratioHours = BigDecimal.ONE.subtract(dev.min(BigDecimal.ONE));
        BigDecimal scoreHours = ratioHours.multiply(rule.getWHours());

        // ---- 5 常用配件：故障标签与项目映射出的配件集合的覆盖率
        Set<String> likely = new LinkedHashSet<>();
        if (v != null) {
            // 车型相关：大众/奥迪系常用机油机滤
            if (List.of("大众", "奥迪", "斯柯达").contains(v.getBrand())) {
                likely.add("PT-1001");
                likely.add("PT-1002");
            }
        }
        for (ArInspection i : ins) {
            for (String tag : FAULT_PARTS.keySet()) {
                if ((i.getFaultDesc() != null && i.getFaultDesc().contains(tag))
                        || (i.getSuggestion() != null && i.getSuggestion().contains(tag))) {
                    likely.addAll(FAULT_PARTS.get(tag));
                }
            }
        }
        for (ArOrderItem i : items) {
            likely.addAll(PROJECT_PARTS.getOrDefault(i.getProjectName(), List.of()));
        }
        List<ArPart> parts = likely.isEmpty() ? List.of()
                : partMapper.selectList(Wrappers.<ArPart>lambdaQuery().in(ArPart::getPartCode, likely));
        long partHit = parts.size();
        BigDecimal scorePart = rate(partHit, Math.max(1, likely.size())).multiply(rule.getWPart());

        // ---- 6 当前库存：可满足数量占比，缺口超过 80% 触发一票否决
        long enough = parts.stream().filter(p -> p.getStockQty() != null
                && p.getMinQty() != null && p.getStockQty().compareTo(p.getMinQty()) >= 0).count();
        BigDecimal ratioStock = rate(enough, Math.max(1, parts.size()));
        boolean veto = !parts.isEmpty() && ratioStock.compareTo(new BigDecimal("0.2")) < 0;
        BigDecimal scoreStock = ratioStock.multiply(rule.getWStock());

        BigDecimal total = scoreModel.add(scoreFault).add(scoreProject)
                .add(scoreHours).add(scorePart).add(scoreStock).setScale(2, RoundingMode.HALF_UP);
        String level;
        if (veto) {
            level = "REJECT";
        } else if (total.compareTo(rule.getThresholdHigh()) >= 0) {
            level = "RECOMMEND";
        } else if (total.compareTo(rule.getThresholdLow()) >= 0) {
            level = "CAUTION";
        } else {
            level = "REJECT";
        }

        // ---- 解释文本：推荐结果之外还要给出主要因素、未满足条件与可改进项
        StringBuilder explain = new StringBuilder();
        explain.append("车型匹配度 ").append(pct(scoreModel, rule.getWModel()))
                .append("、故障标签命中 ").append(faultHit).append("/").append(ins.size())
                .append(" 项、诊断项目可映射配件 ").append(projHit).append("/").append(items.size())
                .append(" 项；预计工时 ").append(planHours).append("h，与同类工单均值 ")
                .append(avgHours).append("h 相比").append(dev.compareTo(new BigDecimal("0.15")) <= 0 ? "接近" : "偏差较大")
                .append("；推荐配件 ").append(parts.size()).append(" 项，其中 ").append(enough).append(" 项库存充足。");
        List<String> miss = new ArrayList<>();
        List<String> improve = new ArrayList<>();
        for (ArPart p : parts) {
            if (p.getStockQty() == null || p.getMinQty() == null) {
                continue;
            }
            if (p.getStockQty().compareTo(BigDecimal.ZERO) <= 0) {
                miss.add(p.getPartName() + " 已缺货");
            } else if (p.getStockQty().compareTo(p.getMinQty()) < 0) {
                miss.add(p.getPartName() + " 低于库存下限（现有 " + p.getStockQty() + p.getUnit() + "）");
                improve.add("建议将 " + p.getPartName() + " 补货至 " + p.getMaxQty() + p.getUnit());
            }
        }
        if (ins.stream().noneMatch(i -> "REPAIR".equals(i.getCheckResult()))) {
            miss.add("检测结果中没有「建议维修」条目，需先补录诊断结论");
        }
        if (items.isEmpty()) {
            miss.add("工单尚未添加维修项目");
        }

        Integer ver = Math.toIntExact(scoreMapper.selectCount(null)) + 1;
        ArScore s = new ArScore();
        s.setScoreNo("SC-" + LocalDate.now().format(DAY) + "-" + String.format("%04d", ver));
        s.setOrderId(orderId);
        s.setRuleId(rule.getId());
        s.setRuleVersion(rule.getRuleVersion());
        s.setInputSnapshot(snapshot(v, ins, items, parts, planHours, avgHours));
        s.setScoreModel(scoreModel.setScale(2, RoundingMode.HALF_UP));
        s.setScoreFault(scoreFault.setScale(2, RoundingMode.HALF_UP));
        s.setScoreProject(scoreProject.setScale(2, RoundingMode.HALF_UP));
        s.setScoreHours(scoreHours.setScale(2, RoundingMode.HALF_UP));
        s.setScorePart(scorePart.setScale(2, RoundingMode.HALF_UP));
        s.setScoreStock(scoreStock.setScale(2, RoundingMode.HALF_UP));
        s.setTotalScore(total);
        s.setResultLevel(level);
        s.setExplainText(cut(explain.toString(), 590));
        s.setMissText(cut(miss.isEmpty() ? "无未满足条件" : String.join("；", miss), 390));
        s.setImproveText(cut(improve.isEmpty() ? "无需改进" : String.join("；", improve), 390));
        s.setCreateTime(LocalDateTime.now());
        scoreMapper.insert(s);

        // 按预检结果生成「待确认」的配件需求，技师确认后才真正领料
        for (ArPart p : parts) {
            ArDemand d = new ArDemand();
            d.setOrderId(orderId);
            d.setPartId(p.getId());
            d.setDemandQty(BigDecimal.ONE);
            d.setStockQty(p.getStockQty());
            d.setGapQty(p.getStockQty() == null ? BigDecimal.ONE
                    : BigDecimal.ONE.subtract(p.getStockQty()).max(BigDecimal.ZERO));
            d.setSource("PRECHECK");
            d.setScoreId(s.getId());
            d.setStatus("WAIT");
            d.setCreateTime(LocalDateTime.now());
            demandMapper.insert(d);
        }
        trace.log(UserContext.username(), "配件需求预检", o.getOrderNo(),
                "规则 " + rule.getRuleVersion() + " 计算得分 " + total + "（" + level + "）");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("score", s);
        out.put("factors", List.of(
                factor("车型", scoreModel, rule.getWModel()),
                factor("故障标签", scoreFault, rule.getWFault()),
                factor("诊断项目", scoreProject, rule.getWProject()),
                factor("历史工时", scoreHours, rule.getWHours()),
                factor("常用配件", scorePart, rule.getWPart()),
                factor("当前库存", scoreStock, rule.getWStock())));
        out.put("demands", demandMapper.selectList(Wrappers.<ArDemand>lambdaQuery()
                .eq(ArDemand::getOrderId, orderId).orderByDesc(ArDemand::getId)));
        out.put("parts", parts);
        return out;
    }

    private Map<String, Object> factor(String name, BigDecimal got, BigDecimal weight) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("weight", weight);
        m.put("score", got.setScale(1, RoundingMode.HALF_UP));
        m.put("percent", weight.compareTo(BigDecimal.ZERO) == 0 ? 0
                : got.divide(weight, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue());
        return m;
    }

    private String snapshot(ArVehicle v, List<ArInspection> ins, List<ArOrderItem> items,
                            List<ArPart> parts, BigDecimal plan, BigDecimal avg) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"model\":").append(v == null ? "\"\"" : "\"" + v.getBrand() + " " + v.getModel() + "\"");
        sb.append(",\"faults\":[");
        for (int i = 0; i < ins.size(); i++) {
            if (!"NORMAL".equals(ins.get(i).getCheckResult())) {
                sb.append(i > 0 ? "," : "").append("\"").append(ins.get(i).getCheckItem()).append("\"");
            }
        }
        sb.append("],\"projects\":[");
        sb.append(String.join(",", items.stream().map(i -> "\"" + i.getProjectName() + "\"").toList()));
        sb.append("],\"planHours\":").append(plan).append(",\"avgHours\":").append(avg);
        sb.append(",\"parts\":[");
        sb.append(String.join(",", parts.stream().map(p -> "\"" + p.getPartCode() + "\"").toList()));
        sb.append("],\"stock\":[");
        sb.append(String.join(",", parts.stream()
                .map(p -> "\"" + p.getPartName() + ":" + p.getStockQty() + "\"").toList()));
        sb.append("]}");
        return cut(sb.toString(), 790);
    }

    /** 人工修正：只改结果等级与说明，原始分数与快照保留。 */
    @Transactional
    public void manual(Long scoreId, Dtos.ManualReq req) {
        ArScore s = scoreMapper.selectById(scoreId);
        if (s == null) {
            throw new BizException(404, "预检记录不存在");
        }
        ArScore u = new ArScore();
        u.setId(scoreId);
        u.setManualResult(req.manualResult());
        u.setManualReason(req.manualReason());
        u.setManualBy(UserContext.username());
        u.setManualTime(LocalDateTime.now());
        scoreMapper.updateById(u);
        ArOrder o = orderMapper.selectById(s.getOrderId());
        trace.log(UserContext.username(), "人工修正预检结果", o == null ? "ar_score:" + scoreId : o.getOrderNo(),
                "系统结果 " + s.getResultLevel() + " → 人工 " + req.manualResult() + "｜" + req.manualReason());
    }

    /** 模块效果：推荐采纳率、人工调整率、缺件拦截次数，用于验证创新模块是否有效。 */
    public Map<String, Object> stats() {
        List<ArScore> all = scoreMapper.selectList(null);
        long total = all.size();
        long manual = all.stream().filter(s -> StringUtils.hasText(s.getManualResult())).count();
        long recommend = all.stream().filter(s -> "RECOMMEND".equals(s.getResultLevel())).count();
        long reject = all.stream().filter(s -> "REJECT".equals(s.getResultLevel())).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("recommend", recommend);
        m.put("caution", all.stream().filter(s -> "CAUTION".equals(s.getResultLevel())).count());
        m.put("reject", reject);
        m.put("manual", manual);
        m.put("adoptRate", total == 0 ? 0 : Math.round((total - manual) * 1000.0 / total) / 10.0);
        m.put("manualRate", total == 0 ? 0 : Math.round(manual * 1000.0 / total) / 10.0);
        m.put("blocked", demandMapper.selectCount(Wrappers.<ArDemand>lambdaQuery()
                .gt(ArDemand::getGapQty, BigDecimal.ZERO)));
        m.put("claimed", flowMapper.selectCount(Wrappers.<ArPartFlow>lambdaQuery()
                .eq(ArPartFlow::getStatus, "ISSUED")));
        return m;
    }

    /** 最新一次预检的配件需求明细，供「领用与预检」页刷新后回显。 */
    public List<ArDemand> demandsOf(Long orderId) {
        ArScore last = scoreMapper.selectOne(Wrappers.<ArScore>lambdaQuery()
                .eq(ArScore::getOrderId, orderId).orderByDesc(ArScore::getId).last("limit 1"));
        if (last == null) {
            return List.of();
        }
        return demandMapper.selectList(Wrappers.<ArDemand>lambdaQuery()
                .eq(ArDemand::getScoreId, last.getId()).orderByAsc(ArDemand::getId));
    }

    public List<ArScore> history(Long orderId) {
        return scoreMapper.selectList(Wrappers.<ArScore>lambdaQuery()
                .eq(ArScore::getOrderId, orderId).orderByDesc(ArScore::getId));
    }

    // ---- 小工具
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 命中率：封顶 1.0，避免同车型工单数大于车辆数时得分超过权重。 */
    private static BigDecimal rate(long hit, long all) {
        BigDecimal r = new BigDecimal(Math.max(0, hit)).divide(new BigDecimal(Math.max(1, all)),
                4, RoundingMode.HALF_UP);
        return r.min(BigDecimal.ONE);
    }

    private static int pct(BigDecimal got, BigDecimal weight) {
        return weight == null || weight.compareTo(BigDecimal.ZERO) == 0 ? 0
                : got.divide(weight, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)).intValue();
    }

    private static String cut(String s, int n) {
        return s == null || s.length() <= n ? s : s.substring(0, n);
    }
}
