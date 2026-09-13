package com.campusrepair.service;

import com.campusrepair.domain.RepairOrder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RepairOrderService {
    RepairOrder createOrder(Long reporterId, Long typeId, Long locationId, String title,
                            String description, List<MultipartFile> files);
    RepairOrder updateOrder(Long reporterId, Long orderId, String title, String description);
    List<RepairOrder> getMyOrders(Long reporterId);
    RepairOrder getProgress(Long reporterId, Long orderId);
}
