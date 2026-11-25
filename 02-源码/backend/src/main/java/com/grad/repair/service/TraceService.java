package com.grad.repair.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grad.repair.entity.SysLog;
import com.grad.repair.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作留痕服务。
 *
 * <p>需求文档要求「工单关键状态变化必须记录日志」，且事件不可由普通用户修改。
 * 这里统一写 sys_log：action 表示动作（接车/报价/确认/派工/领料/质检/交车/返工/人工修正），
 * target 写业务编号（工单号、领用单号等），detail 依次记录「原状态 → 新状态｜原因」。</p>
 *
 * <p>只做插入与查询，不提供更新/删除方法，保证留痕不可篡改。</p>
 */
@Service
@RequiredArgsConstructor
public class TraceService {

    private final SysLogMapper logMapper;

    /** 记一条业务留痕。detail 建议写成「原状态 → 新状态｜原因」。 */
    public void log(String username, String action, String target, String detail) {
        SysLog log = new SysLog();
        log.setUsername(username == null ? "system" : username);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail != null && detail.length() > 290 ? detail.substring(0, 290) : detail);
        log.setIp("127.0.0.1");
        log.setCreateTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    /** 按业务编号查时间轴，倒序返回（最近的在最上面）。 */
    public List<SysLog> timeline(String bizNo) {
        return logMapper.selectList(Wrappers.<SysLog>lambdaQuery()
                .eq(SysLog::getTarget, bizNo)
                .orderByDesc(SysLog::getId));
    }

    /** 关键字检索留痕，供日志页使用。 */
    public List<SysLog> search(String keyword, int limit) {
        var q = Wrappers.<SysLog>lambdaQuery().orderByDesc(SysLog::getId);
        if (keyword != null && !keyword.isBlank()) {
            q.and(w -> w.like(SysLog::getTarget, keyword)
                    .or().like(SysLog::getDetail, keyword)
                    .or().like(SysLog::getUsername, keyword));
        }
        return logMapper.selectList(q.last("limit " + Math.max(1, Math.min(limit, 200))));
    }
}
