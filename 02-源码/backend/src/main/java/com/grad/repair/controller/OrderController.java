package com.grad.repair.controller;

import com.grad.repair.common.PageResult;
import com.grad.repair.common.R;
import com.grad.repair.common.UserContext;
import com.grad.repair.dto.Dtos;
import com.grad.repair.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 维修工单接口：接车 → 诊断 → 报价确认 → 派工领料 → 质检 → 结算交车，含返修与评价。
 * 所有写接口在后端校验权限与状态，前端按钮只做提示。
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long advisorId,
                                                   @RequestParam(required = false) Long techId,
                                                   @RequestParam(defaultValue = "true") boolean onlyMine,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size) {
        return R.ok(orderService.page(status, keyword, advisorId, techId, onlyMine, page, size));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(orderService.detail(id));
    }

    /** 接车登记并建单（服务顾问/经理）。 */
    @PostMapping("/receive")
    public R<Map<String, Object>> receive(@Valid @RequestBody Dtos.ReceptionReq req) {
        if (!UserContext.isService()) {
            return R.fail(403, "只有服务顾问可以办理接车建单");
        }
        return R.ok(orderService.receive(req));
    }

    @PostMapping("/{id}/transition")
    public R<Map<String, Object>> transition(@PathVariable Long id,
                                             @Valid @RequestBody Dtos.TransitionReq req) {
        return R.ok(orderService.transition(id, req.targetStatus(), req.reason()));
    }

    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @RequestBody Dtos.TransitionReq req) {
        orderService.cancel(id, req.reason());
        return R.ok();
    }

    // ---------------- 诊断 ----------------
    @PostMapping("/inspection")
    public R<Void> addInspection(@Valid @RequestBody Dtos.InspectionReq req) {
        orderService.addInspection(req);
        return R.ok();
    }

    @DeleteMapping("/inspection/{id}")
    public R<Void> deleteInspection(@PathVariable Long id) {
        orderService.deleteInspection(id);
        return R.ok();
    }

    // ---------------- 项目 ----------------
    @PostMapping("/item")
    public R<Void> addItem(@Valid @RequestBody Dtos.ItemReq req) {
        orderService.addItem(req);
        return R.ok();
    }

    @DeleteMapping("/item/{id}")
    public R<Void> removeItem(@PathVariable Long id) {
        orderService.removeItem(id);
        return R.ok();
    }

    // ---------------- 报价与确认 ----------------
    @PostMapping("/quote")
    public R<Map<String, Object>> createQuote(@Valid @RequestBody Dtos.QuoteReq req) {
        if (!UserContext.isService()) {
            return R.fail(403, "只有服务顾问可以生成报价");
        }
        return R.ok(orderService.createQuote(req));
    }

    /** 客户逐项确认：仓库人员无权修改确认结果。 */
    @PostMapping("/confirm")
    public R<Map<String, Object>> confirm(@Valid @RequestBody Dtos.ConfirmReq req) {
        if (UserContext.isStore()) {
            return R.fail(403, "仓库人员无权修改客户确认结果");
        }
        return R.ok(orderService.confirm(req));
    }

    // ---------------- 派工与工时 ----------------
    @PostMapping("/dispatch")
    public R<Void> dispatch(@Valid @RequestBody Dtos.DispatchReq req) {
        if (!UserContext.isService()) {
            return R.fail(403, "只有服务顾问可以派工");
        }
        orderService.dispatch(req);
        return R.ok();
    }

    @PostMapping("/dispatch/{id}/worklog")
    public R<Void> worklog(@PathVariable Long id, @Valid @RequestBody Dtos.WorklogReq req) {
        orderService.worklog(id, req);
        return R.ok();
    }

    // ---------------- 质检与返工 ----------------
    @PostMapping("/qc")
    public R<Map<String, Object>> qc(@Valid @RequestBody Dtos.QcReq req) {
        if (!UserContext.isQc()) {
            return R.fail(403, "只有质检人员可以执行质量检验");
        }
        return R.ok(orderService.qc(req));
    }

    @PostMapping("/rework/{id}/recheck")
    public R<Void> recheck(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.recheck(id, body.getOrDefault("result", "PASS"), body.get("remark"));
        return R.ok();
    }

    // ---------------- 结算与交车 ----------------
    @PostMapping("/payment")
    public R<Map<String, Object>> payment(@Valid @RequestBody Dtos.PaymentReq req) {
        return R.ok(orderService.registerPayment(req));
    }

    @PostMapping("/delivery")
    public R<Map<String, Object>> delivery(@Valid @RequestBody Dtos.DeliveryReq req) {
        return R.ok(orderService.deliver(req));
    }

    // ---------------- 返修与评价 ----------------
    @PostMapping("/return")
    public R<Map<String, Object>> createReturn(@Valid @RequestBody Dtos.ReturnReq req) {
        return R.ok(orderService.createReturn(req));
    }

    @PostMapping("/return/{id}/duty")
    public R<Void> duty(@PathVariable Long id, @Valid @RequestBody Dtos.DutyReq req) {
        if (!UserContext.isManager() && !UserContext.isQc()) {
            return R.fail(403, "只有质检或门店经理可以判定返修责任");
        }
        orderService.judgeDuty(id, req);
        return R.ok();
    }

    @PostMapping("/return/{id}/finish")
    public R<Void> finishReturn(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        orderService.finishReturn(id, body == null ? null : body.get("remark"));
        return R.ok();
    }

    @PostMapping("/review")
    public R<Void> review(@Valid @RequestBody Dtos.ReviewReq req) {
        orderService.review(req);
        return R.ok();
    }

    @PostMapping("/review/{id}/reply")
    public R<Void> reply(@PathVariable Long id, @Valid @RequestBody Dtos.ReplyReq req) {
        orderService.replyReview(id, req.reply());
        return R.ok();
    }

    // ---------------- 车主门户 ----------------
    @GetMapping("/portal/my")
    public R<PageResult<Map<String, Object>>> myOrders(@RequestParam(required = false) Long customerId,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return R.ok(orderService.myOrders(customerId, page, size));
    }

    @GetMapping("/portal/quotes")
    public R<List<Map<String, Object>>> pendingQuotes(@RequestParam(required = false) Long customerId) {
        return R.ok(orderService.pendingQuotes(customerId));
    }
}
