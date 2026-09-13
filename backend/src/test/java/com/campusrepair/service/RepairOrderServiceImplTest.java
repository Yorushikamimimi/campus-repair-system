package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.domain.RepairLocation;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.RepairType;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.repository.RepairImageRepository;
import com.campusrepair.repository.RepairLocationRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.repository.RepairTypeRepository;
import com.campusrepair.service.impl.RepairOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepairOrderServiceImplTest {
    @Mock RepairOrderRepository orderRepository;
    @Mock OrderStateService stateService;
    @Mock FileStorageService fileStorageService;
    @Mock RepairTypeRepository typeRepository;
    @Mock RepairLocationRepository locationRepository;
    @Mock RepairImageRepository imageRepository;
    private RepairOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RepairOrderServiceImpl(orderRepository, stateService, fileStorageService,
                typeRepository, locationRepository, imageRepository);
        lenient().when(typeRepository.findById(1L)).thenReturn(type(true));
        lenient().when(locationRepository.findById(2L)).thenReturn(location(true));
        lenient().when(orderRepository.insertOrder(any(RepairOrder.class))).thenAnswer(invocation -> {
            RepairOrder order = invocation.getArgument(0);
            order.setOrderId(100L);
            return order;
        });
    }

    @Test
    void createOrderValidatesReferencesAndPersistsImages() {
        MockMultipartFile file = new MockMultipartFile("files", "leak.png", "image/png", new byte[]{1, 2});
        when(fileStorageService.save(100L, "leak.png", new byte[]{1, 2}))
                .thenReturn("/uploads/repair/100/file.png");

        RepairOrder result = service.createOrder(9L, 1L, 2L, "水龙头漏水", "请尽快处理", List.of(file));

        assertEquals("SUBMITTED", result.getStatus());
        verify(orderRepository).insertOrder(any(RepairOrder.class));
        verify(imageRepository).save(any());
        verify(stateService).submit(100L, 9L);
    }

    @Test
    void createOrderRejectsMissingType() {
        when(typeRepository.findById(1L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.createOrder(9L, 1L, 2L, "标题", "描述", Collections.emptyList()));
        verify(orderRepository, never()).insertOrder(any());
    }

    @Test
    void createOrderRejectsDisabledLocation() {
        when(locationRepository.findById(2L)).thenReturn(location(false));

        assertThrows(BusinessException.class,
                () -> service.createOrder(9L, 1L, 2L, "标题", "描述", Collections.emptyList()));
        verify(orderRepository, never()).insertOrder(any());
    }

    @Test
    void createOrderWithoutImagesIsSuccessful() {
        RepairOrder result = service.createOrder(9L, 1L, 2L, "标题", "描述", Collections.emptyList());

        assertEquals(100L, result.getOrderId());
        verify(imageRepository, never()).save(any());
        verify(stateService).submit(100L, 9L);
    }

    @Test
    void imagePersistenceFailureCleansUpStoredFile() {
        MockMultipartFile file = new MockMultipartFile("files", "leak.jpg", "image/jpeg", new byte[]{3});
        when(fileStorageService.save(100L, "leak.jpg", new byte[]{3}))
                .thenReturn("/uploads/repair/100/file.jpg");
        doThrow(new IllegalStateException("database unavailable")).when(imageRepository).save(any());

        assertThrows(IllegalStateException.class,
                () -> service.createOrder(9L, 1L, 2L, "标题", "描述", List.of(file)));
        verify(fileStorageService).delete("/uploads/repair/100/file.jpg");
        verify(stateService, never()).submit(any(), any());
    }

    @Test
    void updateSubmittedOrderByOwnerSucceeds() {
        RepairOrder order = new RepairOrder();
        order.setOrderId(100L);
        order.setReporterId(9L);
        order.setTypeId(1L);
        order.setLocationId(2L);
        order.setStatus("SUBMITTED");
        order.setTitle("标题");
        order.setDescription("描述");
        when(orderRepository.findById(100L)).thenReturn(order);

        RepairOrder updated = service.updateOrder(9L, 100L, "新标题", "新描述");

        assertEquals("新标题", updated.getTitle());
        assertEquals("新描述", updated.getDescription());
        verify(orderRepository).updateOrder(order);
    }

    @Test
    void updateOrderByOthersIsForbidden() {
        RepairOrder order = new RepairOrder();
        order.setOrderId(101L);
        order.setReporterId(8L);
        order.setStatus("SUBMITTED");
        when(orderRepository.findById(101L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateOrder(9L, 101L, "新标题", "新描述"));
        assertSame(ErrorCode.FORBIDDEN, ex.errorCode());
        verify(orderRepository, never()).updateOrder(order);
    }

    @Test
    void updateOrderWhenNotSubmittedRejected() {
        RepairOrder order = new RepairOrder();
        order.setOrderId(102L);
        order.setReporterId(9L);
        order.setStatus("COMPLETED");
        when(orderRepository.findById(102L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateOrder(9L, 102L, "新标题", "新描述"));
        assertSame(ErrorCode.CONFLICT, ex.errorCode());
        verify(orderRepository, never()).updateOrder(order);
    }

    private RepairType type(boolean enabled) {
        RepairType type = new RepairType();
        type.setTypeId(1L);
        type.setStatus(enabled ? 1 : 0);
        return type;
    }

    private RepairLocation location(boolean enabled) {
        RepairLocation location = new RepairLocation();
        location.setLocationId(2L);
        location.setStatus(enabled ? 1 : 0);
        return location;
    }
}
