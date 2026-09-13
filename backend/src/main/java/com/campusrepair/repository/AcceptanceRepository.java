package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.AcceptanceRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AcceptanceRepository extends BaseMapper<AcceptanceRecord> {
    default AcceptanceRecord saveAcceptance(AcceptanceRecord record) { insert(record); return record; }
    default List<AcceptanceRecord> findByOrder(Long orderId) { return selectList(Wrappers.<AcceptanceRecord>lambdaQuery().eq(AcceptanceRecord::getOrderId, orderId).orderByAsc(AcceptanceRecord::getAcceptanceId)); }
    default AcceptanceRecord findLatestByOrder(Long orderId) { return selectOne(Wrappers.<AcceptanceRecord>lambdaQuery().eq(AcceptanceRecord::getOrderId, orderId).orderByDesc(AcceptanceRecord::getAcceptanceId).last("LIMIT 1")); }
}
