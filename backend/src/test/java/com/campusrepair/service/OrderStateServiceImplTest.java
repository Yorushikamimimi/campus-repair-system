package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.repository.OrderStatusRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.service.impl.OrderStateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStateServiceImplTest {
    @Mock RepairOrderRepository orderRepository;
    @Mock OrderStatusRepository statusRepository;
    private OrderStateServiceImpl service;

    @BeforeEach
    void setUp() { service = new OrderStateServiceImpl(orderRepository, statusRepository); }

    @Test
    void submitSetsPendingAuditAndWritesLog() {
        RepairOrder order = new RepairOrder();
        order.setOrderId(20L);
        when(orderRepository.findById(20L)).thenReturn(order);

        service.submit(20L, 9L);

        assertEquals("SUBMITTED", order.getStatus());
        verify(orderRepository).updateOrder(order);
        verify(statusRepository).appendLog(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void illegalTransitionIsRejected() {
        RepairOrder order = new RepairOrder();
        order.setStatus("PROCESSING");
        when(orderRepository.findById(20L)).thenReturn(order);

        assertThrows(BusinessException.class, () -> service.approve(20L, 8L));
        verify(orderRepository, never()).updateOrder(order);
        verify(statusRepository, never()).appendLog(org.mockito.ArgumentMatchers.any());
    }
}
