package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.RepairLocation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepairLocationRepository extends BaseMapper<RepairLocation> {
    default RepairLocation findById(Long locationId) { return selectById(locationId); }

    default List<RepairLocation> findAllEnabled() {
        return selectList(Wrappers.<RepairLocation>lambdaQuery()
                .eq(RepairLocation::getStatus, 1)
                .orderByAsc(RepairLocation::getLocationId));
    }
}
