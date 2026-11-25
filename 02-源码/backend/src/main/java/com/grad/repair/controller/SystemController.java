package com.grad.repair.controller;

import com.grad.repair.common.PageResult;
import com.grad.repair.common.R;
import com.grad.repair.dto.Dtos;
import com.grad.repair.entity.SysDict;
import com.grad.repair.entity.SysLog;
import com.grad.repair.entity.SysMessage;
import com.grad.repair.service.SystemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统管理接口，统一 /api/system/** 前缀，仅感控/后勤管理员可调用（见 SecurityConfig）。
 * 菜单只控制可见性，此处再以角色兜底，防止直接调用写接口越权。
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/user/page")
    public R<PageResult<Map<String, Object>>> userPage(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long roleId,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "10") long size) {
        return R.ok(systemService.userPage(keyword, roleId, page, size));
    }

    @PostMapping("/user")
    public R<Void> saveUser(@Valid @RequestBody Dtos.UserReq req) {
        systemService.saveUser(req);
        return R.ok();
    }

    @PutMapping("/user/{id}/reset")
    public R<Void> reset(@PathVariable Long id) {
        systemService.resetPassword(id);
        return R.ok();
    }

    @GetMapping("/role/list")
    public R<List<Map<String, Object>>> roles() {
        return R.ok(systemService.roleList());
    }

    @GetMapping("/menu/list")
    public R<List<Map<String, Object>>> menus() {
        return R.ok(systemService.menuList());
    }

    @GetMapping("/role/{id}/menu")
    public R<List<Long>> roleMenus(@PathVariable Long id) {
        return R.ok(systemService.roleMenus(id));
    }

    @PutMapping("/role/{id}/menu")
    public R<Void> saveRoleMenus(@PathVariable Long id, @RequestBody Dtos.RoleMenuReq req) {
        systemService.saveRoleMenus(id, req.menuIds());
        return R.ok();
    }

    @GetMapping("/dict/{type}")
    public R<List<SysDict>> dict(@PathVariable String type) {
        return R.ok(systemService.dict(type));
    }

    @GetMapping("/log/page")
    public R<PageResult<SysLog>> logs(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        return R.ok(systemService.logPage(page, size));
    }

    @GetMapping("/message/page")
    public R<PageResult<SysMessage>> messages(@RequestParam(required = false) Integer isRead,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return R.ok(systemService.messagePage(isRead, page, size));
    }

    @PutMapping("/message/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        systemService.readMessage(id);
        return R.ok();
    }

}
