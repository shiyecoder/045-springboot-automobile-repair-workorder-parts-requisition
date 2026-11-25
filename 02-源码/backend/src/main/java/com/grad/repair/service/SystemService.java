package com.grad.repair.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.repair.common.BizException;
import com.grad.repair.common.PageResult;
import com.grad.repair.common.UserContext;
import com.grad.repair.dto.Dtos;
import com.grad.repair.entity.*;
import com.grad.repair.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统管理：用户、角色、菜单、字典、消息、日志、人员资质。
 * 角色与菜单是两套权限：菜单只决定可见性，写接口的角色校验在 SecurityConfig 中完成。
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysDictMapper dictMapper;
    private final SysMessageMapper messageMapper;
    private final SysLogMapper logMapper;
    private final AuthService authService;
    private final PasswordEncoder encoder;

    /** 用户列表：支持按登录名/姓名关键字与角色过滤 */
    public PageResult<Map<String, Object>> userPage(String keyword, Long roleId, long page, long size) {
        List<Long> userIds = null;
        if (roleId != null) {
            userIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                            .eq(SysUserRole::getRoleId, roleId))
                    .stream().map(SysUserRole::getUserId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return PageResult.of(0, page, size, List.of());
            }
        }
        var q = Wrappers.<SysUser>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
        }
        if (userIds != null) {
            q.in(SysUser::getId, userIds);
        }
        q.orderByDesc(SysUser::getId);
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), q);
        Map<Long, SysRole> roles = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r));
        List<Map<String, Object>> rows = p.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("phone", u.getPhone());
            m.put("avatar", u.getAvatar());
            m.put("orgName", u.getOrgName());
            m.put("status", u.getStatus());
            m.put("lastLogin", u.getLastLogin());
            SysUserRole ur = userRoleMapper.selectOne(Wrappers.<SysUserRole>lambdaQuery()
                    .eq(SysUserRole::getUserId, u.getId()).last("limit 1"));
            SysRole r = ur == null ? null : roles.get(ur.getRoleId());
            m.put("roleId", r == null ? null : r.getId());
            m.put("roleName", r == null ? null : r.getRoleName());
            return m;
        }).collect(Collectors.toList());
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 新增或编辑用户；密码为空表示不修改 */
    @Transactional
    public void saveUser(Dtos.UserReq req) {
        SysUser exist = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, req.username().trim()));
        if (exist != null && !exist.getId().equals(req.id())) {
            throw new BizException(400, "登录名已存在");
        }
        SysUser u = new SysUser();
        u.setId(req.id());
        u.setUsername(req.username().trim());
        u.setRealName(req.realName());
        u.setPhone(req.phone());
        u.setOrgId(req.orgId());
        u.setOrgName(req.orgName());
        u.setStatus(req.status() == null ? 1 : req.status());
        if (StringUtils.hasText(req.avatar())) {
            u.setAvatar(req.avatar());
        }
        if (req.id() == null) {
            u.setPassword(encoder.encode(StringUtils.hasText(req.password()) ? req.password() : "123456"));
            u.setLoginFailCount(0);
            u.setCreateTime(LocalDateTime.now());
            userMapper.insert(u);
        } else {
            if (StringUtils.hasText(req.password())) {
                u.setPassword(encoder.encode(req.password()));
            }
            userMapper.updateById(u);
        }
        if (req.roleId() != null) {
            userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, u.getId()));
            SysUserRole ur = new SysUserRole();
            ur.setUserId(u.getId());
            ur.setRoleId(req.roleId());
            userRoleMapper.insert(ur);
        }
        authService.writeLog(UserContext.get().username(), req.id() == null ? "新增用户" : "编辑用户",
                "sys_user:" + u.getId(), req.username(), null);
    }

    @Transactional
    public void resetPassword(Long id) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPassword(encoder.encode("123456"));
        u.setLoginFailCount(0);
        userMapper.updateById(u);
        authService.writeLog(UserContext.get().username(), "重置密码", "sys_user:" + id, "重置为默认密码", null);
    }

    public List<Map<String, Object>> roleList() {
        return roleMapper.selectList(null).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("roleCode", r.getRoleCode());
            m.put("roleName", r.getRoleName());
            m.put("dataScope", r.getDataScope());
            m.put("remark", r.getRemark());
            m.put("userCount", userRoleMapper.selectCount(Wrappers.<SysUserRole>lambdaQuery()
                    .eq(SysUserRole::getRoleId, r.getId())));
            m.put("menuCount", roleMenuMapper.selectCount(Wrappers.<SysRoleMenu>lambdaQuery()
                    .eq(SysRoleMenu::getRoleId, r.getId())));
            return m;
        }).collect(Collectors.toList());
    }

    /** 菜单树的平铺结构，前端「分配菜单」使用树形勾选 */
    public List<Map<String, Object>> menuList() {
        List<SysMenu> all = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getSort));
        return all.stream().map(m -> {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", m.getId());
            o.put("parentId", m.getParentId());
            o.put("name", m.getMenuName());
            o.put("path", m.getPath());
            o.put("type", m.getMenuType());
            o.put("perm", m.getPerm());
            o.put("sort", m.getSort());
            return o;
        }).collect(Collectors.toList());
    }

    public List<Long> roleMenus(Long roleId) {
        return roleMenuMapper.selectList(Wrappers.<SysRoleMenu>lambdaQuery()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Transactional
    public void saveRoleMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        for (Long mid : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(mid);
            roleMenuMapper.insert(rm);
        }
        authService.writeLog(UserContext.get().username(), "分配菜单", "sys_role:" + roleId,
                "菜单数 " + menuIds.size(), null);
    }

    public List<SysDict> dict(String type) {
        return dictMapper.selectList(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getDictType, type).orderByAsc(SysDict::getSort));
    }

    public PageResult<SysLog> logPage(long page, long size) {
        Page<SysLog> p = logMapper.selectPage(new Page<>(page, size),
                Wrappers.<SysLog>lambdaQuery().orderByDesc(SysLog::getId));
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    /** 我的消息：只返回本人或全员消息，按已读状态与时间倒序 */
    public PageResult<SysMessage> messagePage(Integer isRead, long page, long size) {
        var q = Wrappers.<SysMessage>lambdaQuery()
                .and(w -> w.eq(SysMessage::getReceiverId, UserContext.userId()).or().isNull(SysMessage::getReceiverId));
        if (isRead != null) {
            q.eq(SysMessage::getIsRead, isRead);
        }
        q.orderByAsc(SysMessage::getIsRead).orderByDesc(SysMessage::getCreateTime);
        Page<SysMessage> p = messageMapper.selectPage(new Page<>(page, size), q);
        return PageResult.of(p.getTotal(), page, size, p.getRecords());
    }

    public void readMessage(Long id) {
        SysMessage m = new SysMessage();
        m.setId(id);
        m.setIsRead(1);
        messageMapper.updateById(m);
    }

    /** 站内消息推送（业务流转时调用） */
    public void push(Long receiverId, String type, String title, String content, String bizNo, String link) {
        SysMessage m = new SysMessage();
        m.setReceiverId(receiverId);
        m.setMsgType(type);
        m.setTitle(title);
        m.setContent(content);
        m.setBizNo(bizNo);
        m.setLink(link);
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);
    }
}
