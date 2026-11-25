#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按库表规格批量生成 MyBatis-Plus 的 entity 与 mapper 源码，保证字段与 sql/init.sql 严格一致。"""
import pathlib

BASE = pathlib.Path("src/main/java/com/grad/theatre")
T = {"L": "Long", "S": "String", "I": "Integer", "B": "BigDecimal", "D": "LocalDateTime", "T": "LocalDate"}

# (类名, 表名, 注释, [(类型缩写, 字段, 中文注释)], 是否含逻辑删除)
SPEC = [
    ("SysOrg", "sys_org", "门店/组织", [("L","id","主键"),("S","orgName","门店名称"),("S","orgCode","门店编码"),("S","city","城市"),("S","address","地址"),("S","phone","联系电话"),("S","openTime","营业开始"),("S","closeTime","营业结束"),("S","traffic","交通指引"),("S","notice","服务须知"),("S","cover","门店图片"),("S","manager","负责人"),("I","status","1 营业 0 停业"),("D","createTime","创建时间")], False),
    ("SysUser", "sys_user", "系统用户", [("L","id","主键"),("S","username","登录名"),("S","password","BCrypt 密码"),("S","realName","姓名"),("S","phone","联系电话"),("S","email","邮箱"),("S","avatar","头像相对路径"),("L","orgId","所属门店"),("S","orgName","门店名称"),("S","gender","性别"),("I","status","1 正常 0 停用"),("I","loginFailCount","连续失败次数"),("D","lockUntil","锁定截止"),("D","lastLogin","最近登录"),("I","deleted","逻辑删除"),("D","createTime","创建时间"),("D","updateTime","更新时间")], True),
    ("SysRole", "sys_role", "角色", [("L","id","主键"),("S","roleCode","角色编码"),("S","roleName","角色名称"),("S","dataScope","all/store/self"),("S","remark","说明")], False),
    ("SysUserRole", "sys_user_role", "用户角色", [("L","id","主键"),("L","userId","用户"),("L","roleId","角色")], False),
    ("SysMenu", "sys_menu", "菜单", [("L","id","主键"),("L","parentId","父菜单"),("S","menuName","菜单名称"),("S","path","前端路由"),("S","icon","图标"),("S","menuType","catalog/menu/button"),("S","perm","权限标识"),("I","sort","排序"),("I","visible","是否显示")], False),
    ("SysRoleMenu", "sys_role_menu", "角色菜单", [("L","id","主键"),("L","roleId","角色"),("L","menuId","菜单")], False),
    ("SysDict", "sys_dict", "数据字典", [("L","id","主键"),("S","dictType","字典类型"),("S","dictLabel","显示值"),("S","dictValue","存储值"),("I","sort","排序")], False),
    ("SysMessage", "sys_message", "站内消息", [("L","id","主键"),("L","receiverId","接收人"),("S","msgType","消息类型"),("S","title","标题"),("S","content","内容"),("S","bizNo","业务编号"),("S","link","跳转地址"),("I","isRead","是否已读"),("D","createTime","创建时间")], False),
    ("SysLog", "sys_log", "操作日志", [("L","id","主键"),("S","username","操作人"),("S","action","操作"),("S","target","操作对象"),("S","detail","明细"),("S","ip","来源 IP"),("D","createTime","时间")], False),

    ("TheatreRoom", "theatre_room", "演出空间（房间/候场区/储物区）", [("L","id","主键"),("S","roomNo","空间编号"),("L","storeId","门店"),("S","storeName","门店名称"),("S","roomName","空间名称"),("S","roomType","演出房间/候场区/储物区"),("I","capacity","容纳人数"),("S","supportThemes","可支持主题"),("S","disabledSlots","停用时间段"),("S","status","可用/停用/维护"),("S","photo","空间图片"),("S","remark","备注"),("D","createTime","创建时间")], False),
    ("TheatreTheme", "theatre_theme", "沉浸式剧场主题", [("L","id","主键"),("S","themeNo","主题编号"),("L","storeId","门店"),("S","storeName","门店名称"),("S","themeName","剧目名称"),("S","themeType","悬疑/古风/科幻/情感/恐怖"),("I","duration","时长分钟"),("I","minPeople","最低成团人数"),("I","maxPeople","人数上限"),("I","ageLimit","年龄限制"),("S","scareLevel","惊吓程度"),("B","price","人均票价"),("I","minInterval","清场间隔分钟"),("S","poster","剧目海报"),("S","notice","注意事项"),("S","description","主题背景"),("S","status","上架/下架"),("D","createTime","创建时间")], False),
    ("TheatreThemeRole", "theatre_theme_role", "主题角色编制", [("L","id","主键"),("L","themeId","主题"),("S","roleName","角色名称"),("S","actorType","演员/主持人/场务"),("I","needCount","所需人数"),("S","description","角色说明")], False),
    ("TheatreSession", "theatre_session", "演出场次（核心主档）", [("L","id","主键"),("S","sessionNo","场次编号"),("L","themeId","主题"),("S","themeName","剧目名称"),("L","storeId","门店"),("S","storeName","门店名称"),("L","roomId","房间"),("S","roomName","房间名称"),("T","showDate","演出日期"),("D","startTime","开始时间"),("D","endTime","结束时间"),("I","duration","时长分钟"),("I","minPeople","最低成团人数"),("I","maxPeople","容纳人数"),("I","bookedCount","已预约人数"),("D","entryDeadline","入场截止时间"),("L","leaderId","场次负责人"),("S","leaderName","负责人姓名"),("S","troupe","演员班组"),("S","status","已发布/待确认/布景准备/检票中/演出中/已结束/已取消/已停演"),("S","poster","剧目海报"),("S","cancelReason","取消原因"),("I","version","乐观锁版本"),("I","deleted","逻辑删除"),("L","createBy","创建人"),("D","createTime","创建时间"),("D","updateTime","更新时间")], True),
    ("TheatreReservation", "theatre_reservation", "预约与门店任务", [("L","id","主键"),("S","reservationNo","预约编号"),("L","sessionId","场次"),("S","sessionNo","场次编号"),("S","themeName","剧目名称"),("S","storeName","门店名称"),("D","showTime","演出开始时间"),("S","contactName","联系人"),("S","contactPhone","联系电话"),("I","peopleCount","同行人数（含本人）"),("I","extraCount","现场加人"),("B","totalAmount","应收金额"),("B","paidAmount","线下实收"),("B","discountAmount","优惠金额"),("B","refundAmount","退款金额"),("S","payMethod","现金/微信/支付宝/挂账"),("S","source","线上/前台"),("S","status","待确认/已确认/已改期/已核验/已完成/已取消"),("S","stage","场次发布/用户预约/门店确认/布景准备/检票体验/复盘归档"),("I","safetyConfirm","安全事项是否确认"),("S","remark","备注"),("L","operatorId","经办人"),("S","operatorName","经办人姓名"),("I","version","乐观锁版本"),("I","deleted","逻辑删除"),("L","createBy","创建人"),("D","createTime","创建时间"),("D","updateTime","更新时间")], True),
    ("TheatreCompanion", "theatre_companion", "同行人", [("L","id","主键"),("L","reservationId","预约"),("S","name","姓名"),("S","phone","电话"),("S","idCard","证件号后四位"),("S","relation","关系"),("I","age","年龄"),("S","healthNote","健康说明"),("D","createTime","创建时间")], False),
    ("TheatreCheckin", "theatre_checkin", "入场核验记录", [("L","id","主键"),("S","checkinNo","核验编号"),("L","reservationId","预约"),("S","reservationNo","预约编号"),("L","sessionId","场次"),("S","sessionNo","场次编号"),("I","arriveCount","实到人数"),("I","extraCount","临时加人"),("S","arriveStatus","已到/迟到/缺席/加人"),("D","checkTime","核验时间"),("S","checker","核验人"),("S","photo","现场图片"),("S","remark","备注"),("D","createTime","创建时间")], False),
    ("TheatreStaff", "theatre_staff", "员工档案", [("L","id","主键"),("S","staffNo","员工编号"),("L","userId","关联账号"),("S","staffName","姓名"),("S","staffType","演员/主持人/场务/前台"),("L","storeId","门店"),("S","storeName","门店名称"),("S","phone","电话"),("S","avatar","头像"),("S","skills","擅长题材"),("S","status","在岗/休假/离职"),("D","entryDate","入职日期"),("D","createTime","创建时间")], False),
    ("TheatreSchedule", "theatre_schedule", "演职人员排班", [("L","id","主键"),("S","scheduleNo","排班编号"),("L","sessionId","场次"),("S","sessionNo","场次编号"),("L","staffId","员工"),("S","staffName","姓名"),("S","duty","演员/主持人/场务"),("S","characterName","角色名称"),("D","startTime","到场时间"),("D","endTime","离场时间"),("S","confirmStatus","待确认/已确认/申请调整"),("S","adjustReason","调整原因"),("S","approveStatus","待审批/已通过/已驳回"),("S","approver","审批人"),("D","approveTime","审批时间"),("D","createTime","创建时间")], False),
    ("TheatreLeave", "theatre_leave", "请假记录", [("L","id","主键"),("L","staffId","员工"),("S","staffName","姓名"),("S","leaveType","病假/事假/调休"),("D","startTime","开始时间"),("D","endTime","结束时间"),("S","reason","原因"),("S","status","待审批/已通过/已驳回"),("S","approver","审批人"),("D","createTime","创建时间")], False),
    ("TheatreChecklist", "theatre_checklist", "场前检查清单", [("L","id","主键"),("L","sessionId","场次"),("S","sessionNo","场次编号"),("S","itemType","房间/灯光/道具/安全通道"),("S","itemName","检查项"),("S","result","正常/异常/不适用"),("S","checker","检查人"),("D","checkTime","检查时间"),("S","remark","备注")], False),
    ("TheatreProp", "theatre_prop", "道具档案", [("L","id","主键"),("S","propNo","道具编号"),("S","propName","道具名称"),("S","category","类别"),("S","location","存放位置"),("L","themeId","适用主题"),("S","themeName","适用主题名称"),("I","stock","库存数量"),("I","warnStock","预警数量"),("S","status","可用/使用中/损坏/送修/停用"),("S","photo","道具图片"),("S","description","说明"),("D","createTime","创建时间")], False),
    ("TheatrePropFlow", "theatre_prop_flow", "道具流转", [("L","id","主键"),("S","flowNo","流转编号"),("L","propId","道具"),("S","propName","道具名称"),("L","sessionId","场次"),("S","sessionNo","场次编号"),("S","flowType","领用/归还/损坏/送修"),("I","qty","数量"),("S","operator","经办人"),("D","flowTime","流转时间"),("S","remark","备注")], False),
    ("TheatreConsumable", "theatre_consumable", "耗材库存", [("L","id","主键"),("S","consumeNo","耗材编号"),("S","consumeName","耗材名称"),("S","category","类别"),("L","storeId","门店"),("S","storeName","门店名称"),("I","stock","库存"),("I","warnStock","预警值"),("S","unit","单位"),("I","status","1 正常 0 停用"),("D","createTime","创建时间")], False),
    ("TheatreIncident", "theatre_incident", "异常事件", [("L","id","主键"),("S","incidentNo","事件编号"),("L","sessionId","场次"),("S","sessionNo","场次编号"),("S","incidentType","身体不适/设备故障/道具损坏/演出中断/安全事故"),("S","level","一般/较大/重大"),("S","location","发生位置"),("D","happenTime","发生时间"),("S","persons","涉及人员"),("S","description","事件描述"),("S","measures","处理过程"),("S","result","处理结果"),("S","photo","现场图片"),("S","handler","责任人"),("S","status","待处置/处置中/待复核/已关闭"),("S","reviewer","确认人"),("S","closeConclusion","复盘结论"),("D","closeTime","关闭时间"),("D","createTime","上报时间")], False),
    ("TheatrePayment", "theatre_payment", "线下收费记录", [("L","id","主键"),("S","payNo","收款单号"),("L","reservationId","预约"),("S","reservationNo","预约编号"),("S","storeName","门店名称"),("B","receivable","应收"),("B","received","实收"),("B","discount","优惠"),("B","refunded","已退"),("S","payMethod","现金/微信/支付宝/挂账"),("S","operator","经办人"),("D","payTime","收款时间"),("S","remark","备注")], False),
    ("TheatreRefund", "theatre_refund", "退款记录", [("L","id","主键"),("S","refundNo","退款单号"),("L","reservationId","预约"),("S","reservationNo","预约编号"),("B","amount","退款金额"),("S","reason","退款原因"),("S","operator","经办人"),("S","status","待审批/已退款/已驳回"),("D","refundTime","退款时间"),("D","createTime","创建时间")], False),
    ("TheatreReview", "theatre_review", "观众评价", [("L","id","主键"),("L","reservationId","预约"),("L","sessionId","场次"),("S","themeName","剧目名称"),("S","audienceName","观众"),("I","themeScore","主题体验"),("I","staffScore","演职服务"),("I","envScore","环境"),("I","safeScore","安全感"),("B","avgScore","平均分"),("S","content","评价内容"),("S","images","评价图片"),("S","reply","门店回复"),("I","status","1 显示 0 隐藏"),("D","createTime","评价时间")], False),
    ("TheatreRule", "theatre_rule", "协同排期规则", [("L","id","主键"),("S","version","规则版本"),("B","wRoom","场景占用权重"),("B","wStaff","演员排班权重"),("B","wProp","道具任务权重"),("B","wInterval","清场间隔权重"),("B","wPeople","预约人数权重"),("B","wBusiness","营业时间权重"),("I","passScore","推荐等级下限"),("I","warnScore","关注等级下限"),("I","minInterval","最小清场间隔分钟"),("D","effectiveTime","生效时间"),("S","creator","创建人"),("I","status","1 启用 0 历史"),("S","remark","变更说明"),("D","createTime","创建时间")], False),
    ("TheatreScore", "theatre_score", "排期协同计算结果快照", [("L","id","主键"),("L","sessionId","场次"),("S","sessionNo","场次编号"),("S","ruleVersion","规则版本"),("I","totalScore","总分"),("S","level","推荐/可执行/不推荐"),("I","roomScore","场景占用得分"),("I","staffScore","演员排班得分"),("I","propScore","道具任务得分"),("I","intervalScore","清场间隔得分"),("I","peopleScore","预约人数得分"),("I","businessScore","营业时间得分"),("S","factorJson","各项明细 JSON"),("S","suggestion","推荐结果"),("S","mainFactor","主要影响因素"),("S","unmet","未满足条件"),("S","improve","可改进项"),("S","manualResult","人工修正结果"),("S","manualReason","修正原因"),("S","manualBy","修正人"),("D","manualTime","修正时间"),("D","createTime","计算时间")], False),
    ("TheatreRecord", "theatre_record", "业务时间轴", [("L","id","主键"),("S","bizType","业务类型"),("L","bizId","业务主键"),("S","bizNo","业务编号"),("S","action","动作"),("S","fromStatus","变更前"),("S","toStatus","变更后"),("S","reason","原因"),("S","operator","操作人"),("D","createTime","时间")], False),
    ("TheatreAttachment", "theatre_attachment", "附件元数据", [("L","id","主键"),("S","bizType","业务类型"),("L","bizId","业务主键"),("S","filePath","相对路径"),("S","fileName","原始文件名"),("L","fileSize","字节数"),("S","uploader","上传人"),("D","createTime","上传时间")], False),
]

IMP = {
    "Long": None, "String": None, "Integer": None,
    "BigDecimal": "java.math.BigDecimal",
    "LocalDateTime": "java.time.LocalDateTime",
    "LocalDate": "java.time.LocalDate",
}


def gen_entity(cls, table, cn, fields, logic):
    imports = set()
    for typ, _, _ in fields:
        t = T[typ]
        if IMP.get(t):
            imports.add(f"import {IMP[t]};")
    head = ["package com.grad.theatre.entity;", "",
            "import com.baomidou.mybatisplus.annotation.TableName;"]
    if logic:
        head.append("import com.baomidou.mybatisplus.annotation.TableLogic;")
    head.append("import lombok.Data;")
    head += sorted(imports)
    head += ["", "/**", f" * {cn}（表 {table}）。字段与 sql/init.sql 保持一致。", " */", "@Data",
             f'@TableName("{table}")', f"public class {cls} {{", ""]
    body = []
    for typ, name, note in fields:
        body.append(f"    /** {note} */")
        if logic and name == "deleted":
            body.append("    @TableLogic")
        body.append(f"    private {T[typ]} {name};")
        body.append("")
    return "\n".join(head + body + ["}"]) + "\n"


def gen_mapper(cls):
    return f"""package com.grad.theatre.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.theatre.entity.{cls};
import org.apache.ibatis.annotations.Mapper;

/** {cls} 数据访问，通用 CRUD 由 MyBatis-Plus 提供。 */
@Mapper
public interface {cls}Mapper extends BaseMapper<{cls}> {{
}}
"""


def main():
    (BASE / "entity").mkdir(parents=True, exist_ok=True)
    (BASE / "mapper").mkdir(parents=True, exist_ok=True)
    for cls, table, cn, fields, logic in SPEC:
        (BASE / "entity" / f"{cls}.java").write_text(gen_entity(cls, table, cn, fields, logic), encoding="utf-8")
        (BASE / "mapper" / f"{cls}Mapper.java").write_text(gen_mapper(cls), encoding="utf-8")
    print("entities+mappers:", len(SPEC))


if __name__ == "__main__":
    main()
