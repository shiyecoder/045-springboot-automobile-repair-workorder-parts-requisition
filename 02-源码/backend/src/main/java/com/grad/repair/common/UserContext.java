package com.grad.repair.common;

/**
 * 当前登录用户上下文。由 JWT 过滤器写入，Controller / Service 只读。
 * 数据范围按需求文档「权限与消息规则」三级控制：
 * admin 全域、store 本门店、self 本人。
 */
public class UserContext {

    public record CurrentUser(Long userId, String username, String realName, String roleCode,
                              Long orgId, String orgName, String dataScope) {
    }

    /** 角色编码常量，避免各处写死字符串（与 sys_role.role_code 一致） */
    public static final String ROLE_ADMIN = "ROLE_MANAGER";     // 门店经理：全域数据与审批
    public static final String ROLE_SERVICE = "ROLE_SERVICE";   // 服务顾问：接车、报价、确认、交车
    public static final String ROLE_TECH = "ROLE_TECH";         // 维修技师：仅本人任务
    public static final String ROLE_STORE = "ROLE_STORE";       // 仓库人员：库存与领退
    public static final String ROLE_QC = "ROLE_QC";             // 质检：检验与返工退回
    public static final String ROLE_OWNER = "ROLE_OWNER";       // 车主客户：本人车辆

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.userId();
    }

    public static String username() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.username();
    }

    public static String realName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.realName();
    }

    public static String roleCode() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.roleCode();
    }

    public static Long orgId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.orgId();
    }

    public static String orgName() {
        CurrentUser u = HOLDER.get();
        return u == null ? "" : u.orgName();
    }

    public static String dataScope() {
        CurrentUser u = HOLDER.get();
        return u == null ? "self" : u.dataScope();
    }

    /** 系统管理员：全域数据 */
    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(roleCode());
    }

    /** 门店经理：全店数据、审批异常、系统配置 */
    public static boolean isManager() {
        return ROLE_ADMIN.equals(roleCode());
    }

    /** 服务顾问：接车、报价与客户确认、交车 */
    public static boolean isService() {
        return ROLE_SERVICE.equals(roleCode()) || isManager();
    }

    /** 维修技师：只能看到本人任务，不能改报价与客户确认结果 */
    public static boolean isTech() {
        return ROLE_TECH.equals(roleCode());
    }

    /** 仓库人员：库存与领退，无权修改维修报价 */
    public static boolean isStore() {
        return ROLE_STORE.equals(roleCode()) || isManager();
    }

    /** 质检：检验、返工退回 */
    public static boolean isQc() {
        return ROLE_QC.equals(roleCode()) || isManager();
    }

    /** 车主客户 */
    public static boolean isOwner() {
        return ROLE_OWNER.equals(roleCode());
    }

    /** 是否为门店内部员工（用于数据范围过滤：车主只能看本人） */
    public static boolean isStaff() {
        return isManager() || ROLE_SERVICE.equals(roleCode()) || isTech()
                || ROLE_STORE.equals(roleCode()) || ROLE_QC.equals(roleCode());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
