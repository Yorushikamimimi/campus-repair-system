package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderStatusRepository extends BaseMapper<OrderStatusLog> {
    default void appendLog(OrderStatusLog log) { insert(log); }

    default List<OrderStatusLog> findByOrder(Long orderId) {
        return selectList(Wrappers.<OrderStatusLog>lambdaQuery()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getChangeTime));
    }

    default OrderStatusLog findLatest(Long orderId) {
        return selectOne(Wrappers.<OrderStatusLog>lambdaQuery()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByDesc(OrderStatusLog::getChangeTime)
                .last("LIMIT 1"));
    }
}
