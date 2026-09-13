package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.impl.QueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceImplTest {
    @Mock RepairOrderRepository repository;

    @Test
    void queryProgressReturnsOnlyOwnedOrder() {
        RepairOrder order = order(10L, 9L, "SUBMITTED");
        when(repository.findById(10L)).thenReturn(order);

        assertEquals(order, new QueryServiceImpl(repository).queryOrderProgress(9L, 10L));
    }

    @Test
    void queryProgressRejectsOtherUsersOrder() {
        when(repository.findById(10L)).thenReturn(order(10L, 99L, "SUBMITTED"));

        assertThrows(BusinessException.class,
                () -> new QueryServiceImpl(repository).queryOrderProgress(9L, 10L));
    }

    private RepairOrder order(Long orderId, Long reporterId, String status) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setReporterId(reporterId);
        order.setStatus(status);
        return order;
    }
}
