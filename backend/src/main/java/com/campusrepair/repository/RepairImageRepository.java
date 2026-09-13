package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusrepair.domain.RepairImage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepairImageRepository extends BaseMapper<RepairImage> {
    default RepairImage save(RepairImage image) {
        insert(image);
        return image;
    }

    default List<RepairImage> findByOrder(Long orderId) {
        return selectList(Wrappers.<RepairImage>lambdaQuery()
                .eq(RepairImage::getOrderId, orderId)
                .orderByAsc(RepairImage::getImageId));
    }

    default void deleteByOrder(Long orderId) {
        delete(Wrappers.<RepairImage>lambdaQuery().eq(RepairImage::getOrderId, orderId));
    }
}
