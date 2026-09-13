package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.EvaluationRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EvaluationRepository extends BaseMapper<EvaluationRecord> {
    default EvaluationRecord saveEvaluation(EvaluationRecord record) { insert(record); return record; }
    default List<EvaluationRecord> findByOrder(Long orderId) { return selectList(Wrappers.<EvaluationRecord>lambdaQuery().eq(EvaluationRecord::getOrderId, orderId).orderByAsc(EvaluationRecord::getEvaluationId)); }
    default boolean existsByOrder(Long orderId) { return selectCount(Wrappers.<EvaluationRecord>lambdaQuery().eq(EvaluationRecord::getOrderId, orderId)) > 0; }
}
