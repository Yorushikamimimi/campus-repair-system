package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.RepairOrder;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface RepairOrderRepository extends BaseMapper<RepairOrder> {
    default RepairOrder insertOrder(RepairOrder order) {
        if (order.getSubmitTime() == null) order.setSubmitTime(LocalDateTime.now());
        insert(order);
        return order;
    }

    default RepairOrder findById(Long orderId) { return selectById(orderId); }

    default List<RepairOrder> findByReporter(Long reporterId) {
        return selectList(Wrappers.<RepairOrder>lambdaQuery()
                .eq(RepairOrder::getReporterId, reporterId)
                .orderByDesc(RepairOrder::getSubmitTime));
    }

    default void updateOrder(RepairOrder order) {
        order.setUpdateTime(LocalDateTime.now());
        updateById(order);
    }

    default List<RepairOrder> findByFilters(String status, Long typeId) {
        QueryWrapper<RepairOrder> query = Wrappers.query();
        query.eq(status != null && !status.isBlank(), "status", status)
                .eq(typeId != null, "type_id", typeId)
                .orderByDesc("submit_time");
        return selectList(query);
    }
}
