package com.grad.repair.controller;

import com.grad.repair.common.PageResult;
import com.grad.repair.common.R;
import com.grad.repair.dto.Dtos;
import com.grad.repair.entity.ArCustomer;
import com.grad.repair.entity.ArPart;
import com.grad.repair.entity.ArProject;
import com.grad.repair.service.MetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 档案与库存接口：客户、车辆、预约、项目库、配件、批次、领退、员工与下拉选项。
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    // ---------------- 客户 ----------------
    @GetMapping("/customer/page")
    public R<PageResult<ArCustomer>> customerPage(@RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size) {
        return R.ok(metaService.customerPage(keyword, type, page, size));
    }

    @PostMapping("/customer")
    public R<Void> saveCustomer(@Valid @RequestBody Dtos.CustomerReq req) {
        metaService.saveCustomer(req);
        return R.ok();
    }

    // ---------------- 车辆 ----------------
    @GetMapping("/vehicle/page")
    public R<PageResult<Map<String, Object>>> vehiclePage(@RequestParam(required = false) String plate,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "1") long page,
                                                          @RequestParam(defaultValue = "10") long size) {
        return R.ok(metaService.vehiclePage(plate, keyword, status, page, size));
    }

    @GetMapping("/vehicle/{id}")
    public R<Map<String, Object>> vehicleDetail(@PathVariable Long id) {
        return R.ok(metaService.vehicleDetail(id));
    }

    @PostMapping("/vehicle")
    public R<Void> saveVehicle(@Valid @RequestBody Dtos.VehicleReq req) {
        metaService.saveVehicle(req);
        return R.ok();
    }

    @DeleteMapping("/vehicle/{id}")
    public R<Void> deleteVehicle(@PathVariable Long id) {
        metaService.deleteVehicle(id);
        return R.ok();
    }

    // ---------------- 预约 ----------------
    @GetMapping("/appointment/page")
    public R<PageResult<Map<String, Object>>> appointmentPage(@RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "10") long size) {
        return R.ok(metaService.appointmentPage(status, keyword, page, size));
    }

    @PostMapping("/appointment")
    public R<Void> saveAppointment(@Valid @RequestBody Dtos.AppointmentReq req) {
        metaService.saveAppointment(req);
        return R.ok();
    }

    @PostMapping("/appointment/{id}/cancel")
    public R<Void> cancelAppointment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        metaService.cancelAppointment(id, body.get("reason"));
        return R.ok();
    }

    // ---------------- 项目库 ----------------
    @GetMapping("/project/page")
    public R<PageResult<ArProject>> projectPage(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String workType,
                                                @RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size) {
        return R.ok(metaService.projectPage(keyword, workType, page, size));
    }

    @GetMapping("/project/list")
    public R<List<ArProject>> projects() {
        return R.ok(metaService.projects());
    }

    // ---------------- 配件与批次 ----------------
    @GetMapping("/part/page")
    public R<PageResult<ArPart>> partPage(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String stockState,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return R.ok(metaService.partPage(keyword, stockState, page, size));
    }

    @PostMapping("/part")
    public R<Void> savePart(@Valid @RequestBody Dtos.PartReq req) {
        metaService.savePart(req);
        return R.ok();
    }

    @GetMapping("/batch/page")
    public R<PageResult<Map<String, Object>>> batchPage(@RequestParam(required = false) Long partId,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size) {
        return R.ok(metaService.batchPage(partId, keyword, page, size));
    }

    @PostMapping("/batch/inbound")
    public R<Void> inbound(@Valid @RequestBody Dtos.BatchReq req) {
        metaService.inbound(req);
        return R.ok();
    }

    // ---------------- 领退 ----------------
    @GetMapping("/flow/page")
    public R<PageResult<Map<String, Object>>> flowPage(@RequestParam(required = false) Long orderId,
                                                       @RequestParam(required = false) String flowType,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return R.ok(metaService.flowPage(orderId, flowType, page, size));
    }

    @PostMapping("/flow")
    public R<Void> partFlow(@Valid @RequestBody Dtos.PartFlowReq req) {
        metaService.partFlow(req);
        return R.ok();
    }

    @GetMapping("/stock/warnings")
    public R<List<Map<String, Object>>> stockWarnings() {
        return R.ok(metaService.stockWarnings());
    }

    // ---------------- 质保台账 / 返修记录 ----------------
    @GetMapping("/warranty/page")
    public R<PageResult<Map<String, Object>>> warrantyPage(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "10") long size,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) String keyword) {
        return R.ok(metaService.warrantyPage(page, size, status, keyword));
    }

    @GetMapping("/return/page")
    public R<PageResult<Map<String, Object>>> returnPage(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "10") long size,
                                                         @RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String duty) {
        return R.ok(metaService.returnPage(page, size, status, duty));
    }

    // ---------------- 员工与选项 ----------------
    @GetMapping("/staff/list")
    public R<List<Map<String, Object>>> staff() {
        return R.ok(metaService.staffList());
    }

    @GetMapping("/options")
    public R<Map<String, Object>> options() {
        return R.ok(metaService.options());
    }
}
