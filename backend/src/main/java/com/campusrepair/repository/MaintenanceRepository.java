package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.RepairRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MaintenanceRepository extends BaseMapper<RepairRecord> {
    default RepairRecord saveRepairRecord(RepairRecord record) { insert(record); return record; }
    default List<RepairRecord> findByOrder(Long orderId) { return selectList(Wrappers.<RepairRecord>lambdaQuery().eq(RepairRecord::getOrderId, orderId).orderByAsc(RepairRecord::getRepairId)); }
    default RepairRecord findLatestByOrder(Long orderId) { return selectOne(Wrappers.<RepairRecord>lambdaQuery().eq(RepairRecord::getOrderId, orderId).orderByDesc(RepairRecord::getRepairId).last("LIMIT 1")); }
    default void updateRepairRecord(RepairRecord record) { updateById(record); }

    default List<RepairRecord> findCompletedRecords() {
        return selectList(Wrappers.<RepairRecord>lambdaQuery()
                .isNotNull(RepairRecord::getStartTime)
                .isNotNull(RepairRecord::getEndTime));
    }
}
