package com.grad.repair.controller;

import com.grad.repair.common.R;
import com.grad.repair.dto.Dtos;
import com.grad.repair.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 认证与个人中心接口。登录与刷新用户信息均返回菜单树。 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody Dtos.LoginReq req, HttpServletRequest request) {
        return R.ok(authService.login(req, request.getRemoteAddr()));
    }

    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(authService.me());
    }

    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody Dtos.ProfileReq req) {
        authService.updateProfile(req);
        return R.ok();
    }

    @PutMapping("/password")
    public R<Void> updatePassword(@Valid @RequestBody Dtos.PasswordReq req) {
        authService.updatePassword(req);
        return R.ok();
    }
}
