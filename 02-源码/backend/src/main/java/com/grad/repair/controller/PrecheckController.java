package com.grad.repair.controller;

import com.grad.repair.common.R;
import com.grad.repair.common.UserContext;
import com.grad.repair.dto.Dtos;
import com.grad.repair.entity.ArDemand;
import com.grad.repair.entity.ArRule;
import com.grad.repair.entity.ArScore;
import com.grad.repair.service.DashboardService;
import com.grad.repair.service.PrecheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 创新点与看板接口。
 *
 * <p>预检模块只给出「建议 + 解释 + 留痕」，最终领料仍需人工确认；
 * 规则修改只影响新计算，历史快照保持原规则版本。</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PrecheckController {

    private final PrecheckService precheckService;
    private final DashboardService dashboardService;

    // ---------------- 预检规则 ----------------
    @GetMapping("/precheck/rules")
    public R<List<ArRule>> rules() {
        return R.ok(precheckService.rules());
    }

    @PostMapping("/precheck/rules")
    public R<ArRule> saveRule(@Valid @RequestBody Dtos.RuleReq req) {
        if (!UserContext.isManager()) {
            return R.fail(403, "只有门店经理可以修改预检规则");
        }
        return R.ok(precheckService.saveRule(req));
    }

    /** 执行预检并返回逐项得分、解释、未满足条件、可改进项与生成的配件需求。 */
    @PostMapping("/precheck/{orderId}/evaluate")
    public R<Map<String, Object>> evaluate(@PathVariable Long orderId) {
        return R.ok(precheckService.evaluate(orderId));
    }

    @PostMapping("/precheck/score/{id}/manual")
    public R<Void> manual(@PathVariable Long id, @Valid @RequestBody Dtos.ManualReq req) {
        if (!UserContext.isManager()) {
            return R.fail(403, "只有业务负责人可以人工修正预检结果");
        }
        precheckService.manual(id, req);
        return R.ok();
    }

    @GetMapping("/precheck/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(precheckService.stats());
    }

    @GetMapping("/precheck/demands/{orderId}")
    public R<List<ArDemand>> demands(@PathVariable Long orderId) {
        return R.ok(precheckService.demandsOf(orderId));
    }

    @GetMapping("/precheck/history/{orderId}")
    public R<List<ArScore>> history(@PathVariable Long orderId) {
        return R.ok(precheckService.history(orderId));
    }

    // ---------------- 看板 ----------------
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(required = false) String range) {
        return R.ok(dashboardService.overview(range));
    }

    @GetMapping("/dashboard/todo")
    public R<List<Map<String, Object>>> todo() {
        return R.ok(dashboardService.todo());
    }

    @GetMapping("/dashboard/monthly")
    public R<List<Map<String, Object>>> monthly() {
        return R.ok(dashboardService.monthly());
    }
}
