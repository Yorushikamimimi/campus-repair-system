package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.RepairRecord;
import com.campusrepair.domain.RepairType;
import com.campusrepair.dto.TypeCountItem;
import com.campusrepair.repository.MaintenanceRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.repository.RepairTypeRepository;
import com.campusrepair.service.impl.StatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {
    @Mock
    RepairOrderRepository repairOrderRepository;
    @Mock
    RepairTypeRepository repairTypeRepository;
    @Mock
    MaintenanceRepository maintenanceRepository;

    private StatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StatisticsServiceImpl(repairOrderRepository, repairTypeRepository, maintenanceRepository);
    }

    @Test
    void countByStatusReturnsAllStatusesWithDefaults() {
        when(repairOrderRepository.selectList(null)).thenReturn(List.of(
                order(101L, OrderStatus.SUBMITTED.code()),
                order(102L, OrderStatus.PENDING_PROCESS.code()),
                order(103L, OrderStatus.PENDING_PROCESS.code()),
                order(104L, OrderStatus.REWORK.code())
        ));

        Map<String, Long> result = service.countByStatus();

        assertEquals(6, result.size());
        assertEquals(1L, result.get(OrderStatus.SUBMITTED.code()));
        assertEquals(2L, result.get(OrderStatus.PENDING_PROCESS.code()));
        assertEquals(0L, result.get(OrderStatus.COMPLETED.code()));
        assertEquals(0L, result.get(OrderStatus.PROCESSING.code()));
        assertEquals(1L, result.get(OrderStatus.REWORK.code()));
        assertEquals(0L, result.get(OrderStatus.PENDING_ACCEPTANCE.code()));
    }

    @Test
    void countByTypeSortsByTypeIdAndContainsTypeName() {
        when(repairOrderRepository.selectList(null)).thenReturn(List.of(
                orderType(301L, 10L),
                orderType(302L, 10L),
                orderType(303L, 11L)
        ));
        when(repairTypeRepository.selectList(null)).thenReturn(List.of(type(11L, "设施维修"), type(10L, "水电维修")));

        List<TypeCountItem> result = service.countByType();

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).typeId());
        assertEquals("水电维修", result.get(0).typeName());
        assertEquals(2L, result.get(0).count());
        assertEquals(11L, result.get(1).typeId());
        assertEquals("设施维修", result.get(1).typeName());
        assertEquals(1L, result.get(1).count());
    }

    @Test
    void countByPeriodRejectsInvalidDateRange() {
        assertThrows(BusinessException.class, () -> service.countByPeriod(null, null));
        assertThrows(BusinessException.class, () -> service.countByPeriod(java.time.LocalDate.of(2026, 9, 13), java.time.LocalDate.of(2026, 9, 12)));
        verify(repairOrderRepository, never()).selectCount(any());
    }

    @Test
    void countByPeriodReturnsRepositoryCount() {
        when(repairOrderRepository.selectCount(any())).thenReturn(3L);

        long result = service.countByPeriod(java.time.LocalDate.of(2026, 9, 1), java.time.LocalDate.of(2026, 9, 30));

        assertEquals(3L, result);
        verify(repairOrderRepository).selectCount(any());
    }

    @Test
    void averageRepairDurationIncludesValidRecordsOnlyAndRoundsMinutes() {
        when(maintenanceRepository.findCompletedRecords()).thenReturn(List.of(
                completedRecord(LocalDateTime.of(2026, 9, 13, 10, 0), LocalDateTime.of(2026, 9, 13, 10, 30)),
                completedRecord(LocalDateTime.of(2026, 9, 13, 11, 0), LocalDateTime.of(2026, 9, 13, 11, 30)),
                completedRecord(LocalDateTime.of(2026, 9, 13, 12, 0), null),
                completedRecord(LocalDateTime.of(2026, 9, 13, 13, 0), LocalDateTime.of(2026, 9, 13, 12, 45))
        ));

        double result = service.averageRepairDuration();

        assertEquals(30.0, result, 0.001);
    }

    @Test
    void averageRepairDurationReturnsZeroWhenNoValidRecord() {
        when(maintenanceRepository.findCompletedRecords()).thenReturn(List.of(
                completedRecord(LocalDateTime.of(2026, 9, 13, 10, 0), null),
                completedRecord(LocalDateTime.of(2026, 9, 13, 10, 0), LocalDateTime.of(2026, 9, 13, 9, 59))
        ));

        assertEquals(0.0, service.averageRepairDuration(), 0.0);
    }

    private RepairOrder order(Long orderId, String status) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setStatus(status);
        return order;
    }

    private RepairOrder orderType(Long orderId, Long typeId) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setTypeId(typeId);
        return order;
    }

    private RepairType type(Long typeId, String typeName) {
        RepairType item = new RepairType();
        item.setTypeId(typeId);
        item.setTypeName(typeName);
        return item;
    }

    private RepairRecord completedRecord(LocalDateTime start, LocalDateTime end) {
        RepairRecord record = new RepairRecord();
        record.setStartTime(start);
        record.setEndTime(end);
        return record;
    }
}
