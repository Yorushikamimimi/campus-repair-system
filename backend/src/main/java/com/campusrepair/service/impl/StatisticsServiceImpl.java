package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.RepairRecord;
import com.campusrepair.domain.RepairType;
import com.campusrepair.dto.TypeCountItem;
import com.campusrepair.repository.MaintenanceRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.repository.RepairTypeRepository;
import com.campusrepair.service.StatisticsService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final RepairOrderRepository repairOrderRepository;
    private final RepairTypeRepository repairTypeRepository;
    private final MaintenanceRepository maintenanceRepository;

    public StatisticsServiceImpl(RepairOrderRepository repairOrderRepository,
                               RepairTypeRepository repairTypeRepository,
                               MaintenanceRepository maintenanceRepository) {
        this.repairOrderRepository = repairOrderRepository;
        this.repairTypeRepository = repairTypeRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Override
    public Map<String, Long> countByStatus() {
        List<RepairOrder> orders = repairOrderRepository.selectList(null);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status.code(), 0L);
        }
        for (RepairOrder order : orders) {
            if (order == null || order.getStatus() == null) continue;
            String status = order.getStatus();
            counts.put(status, counts.getOrDefault(status, 0L) + 1L);
        }
        Map<String, Long> result = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status.code(), counts.get(status.code()));
        }
        return result;
    }

    @Override
    public List<TypeCountItem> countByType() {
        List<RepairOrder> orders = repairOrderRepository.selectList(null);
        Map<Long, Long> grouped = orders.stream()
                .filter(order -> order.getTypeId() != null)
                .collect(Collectors.groupingBy(RepairOrder::getTypeId, Collectors.counting()));

        Map<Long, String> typeNames = repairTypeRepository.selectList(null).stream()
                .collect(Collectors.toMap(RepairType::getTypeId, RepairType::getTypeName, (first, second) -> first));

        List<TypeCountItem> items = new ArrayList<>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> items.add(new TypeCountItem(entry.getKey(), typeNames.get(entry.getKey()), entry.getValue())));
        return items;
    }

    @Override
    public long countByPeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "开始和结束日期不能为空");
        if (start.isAfter(end)) throw new BusinessException(ErrorCode.BAD_REQUEST, "开始日期不能晚于结束日期");
        LocalDateTime startTime = start.atStartOfDay();
        LocalDateTime endTime = end.plusDays(1).atStartOfDay();
        return repairOrderRepository.selectCount(
                Wrappers.<RepairOrder>lambdaQuery()
                        .ge(RepairOrder::getSubmitTime, startTime)
                        .lt(RepairOrder::getSubmitTime, endTime));
    }

    @Override
    public double averageRepairDuration() {
        List<RepairRecord> completed = maintenanceRepository.findCompletedRecords();
        if (completed.isEmpty()) return 0.0;

        List<RepairRecord> validRecords = completed.stream()
                .filter(this::isValidDurationRecord)
                .toList();
        if (validRecords.isEmpty()) return 0.0;

        long totalMinutes = validRecords.stream()
                .mapToLong(this::calculateDurationMinutes)
                .sum();
        return roundedAverage(totalMinutes, validRecords.size());
    }

    private boolean isValidDurationRecord(RepairRecord record) {
        return record.getStartTime() != null
                && record.getEndTime() != null
                && !record.getEndTime().isBefore(record.getStartTime());
    }

    private long calculateDurationMinutes(RepairRecord record) {
        return Duration.between(record.getStartTime(), record.getEndTime()).toMinutes();
    }

    private double roundedAverage(long totalMinutes, int count) {
        return BigDecimal.valueOf(((double) totalMinutes) / count)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
