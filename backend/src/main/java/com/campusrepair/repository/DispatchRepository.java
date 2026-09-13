package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.DispatchRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DispatchRepository extends BaseMapper<DispatchRecord> {
    default DispatchRecord save(DispatchRecord record) { insert(record); return record; }
    default DispatchRecord findLatestByOrder(Long orderId) { return selectOne(Wrappers.<DispatchRecord>lambdaQuery().eq(DispatchRecord::getOrderId, orderId).orderByDesc(DispatchRecord::getDispatchTime).last("LIMIT 1")); }
    default List<DispatchRecord> findTasksByMaintainer(Long maintainerId) { return selectList(Wrappers.<DispatchRecord>lambdaQuery().eq(DispatchRecord::getMaintainerId, maintainerId).orderByDesc(DispatchRecord::getDispatchTime)); }
    default void updateStatus(Long dispatchId, String status) { update(null, Wrappers.<DispatchRecord>lambdaUpdate().eq(DispatchRecord::getDispatchId, dispatchId).set(DispatchRecord::getDispatchStatus, status)); }
}
