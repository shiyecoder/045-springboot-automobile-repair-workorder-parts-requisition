package com.grad.repair.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.repair.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全配置：无状态 JWT，放行登录与上传目录，其余接口需认证。
 * 关键写接口额外做角色校验，避免「前端隐藏按钮但直接调接口」的越权（需求文档 权限与消息规则）。
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**", "/uploads/**", "/error").permitAll()
                // 系统管理（用户/角色/菜单/字典/日志/消息）仅门店经理
                .requestMatchers("/api/system/**").hasRole("MANAGER")
                // 预检规则：仅经理可改权重与人工修正，其余角色只读
                .requestMatchers(HttpMethod.POST, "/api/precheck/rules").hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/precheck/score/*/manual").hasRole("MANAGER")
                // 配件档案、批次入库、领退由仓库与经理维护
                .requestMatchers(HttpMethod.POST, "/api/meta/part", "/api/meta/batch/**",
                        "/api/meta/flow").hasAnyRole("MANAGER", "STORE")
                // 客户、车辆、预约由服务顾问与经理维护
                .requestMatchers(HttpMethod.POST, "/api/meta/customer", "/api/meta/vehicle",
                        "/api/meta/appointment").hasAnyRole("MANAGER", "SERVICE")
                .requestMatchers(HttpMethod.DELETE, "/api/meta/vehicle/**").hasAnyRole("MANAGER", "SERVICE")
                // 接车建单、报价、派工仅服务顾问与经理（细粒度校验仍在 Controller/Service）
                .requestMatchers(HttpMethod.POST, "/api/order/receive", "/api/order/quote",
                        "/api/order/dispatch").hasAnyRole("MANAGER", "SERVICE")
                // 质量检验与返工复检由质检执行
                .requestMatchers(HttpMethod.POST, "/api/order/qc", "/api/order/rework/**")
                        .hasAnyRole("MANAGER", "QC")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> write(res, 401, "登录状态已失效，请重新登录"))
                .accessDeniedHandler((req, res, ex) -> write(res, 403, "没有该操作的权限")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(f -> f.disable())
            .httpBasic(b -> b.disable())
            .logout(l -> l.disable());
        return http.build();
    }

    private void write(jakarta.servlet.http.HttpServletResponse res, int code, String msg) throws java.io.IOException {
        res.setStatus(200);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(R.fail(code, msg)));
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }
}
