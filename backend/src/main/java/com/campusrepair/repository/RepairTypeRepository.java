package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.RepairType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepairTypeRepository extends BaseMapper<RepairType> {
    default RepairType findById(Long typeId) { return selectById(typeId); }

    default List<RepairType> findAllEnabled() {
        return selectList(Wrappers.<RepairType>lambdaQuery()
                .eq(RepairType::getStatus, 1)
                .orderByAsc(RepairType::getTypeId));
    }
}
