package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.OrderStatus;
import com.campusrepair.domain.RepairImage;
import com.campusrepair.domain.RepairLocation;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.RepairType;
import com.campusrepair.repository.RepairImageRepository;
import com.campusrepair.repository.RepairLocationRepository;
import com.campusrepair.repository.RepairOrderRepository;
import com.campusrepair.repository.RepairTypeRepository;
import com.campusrepair.service.FileStorageService;
import com.campusrepair.service.OrderStateService;
import com.campusrepair.service.RepairOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class RepairOrderServiceImpl implements RepairOrderService {
    private static final Logger log = LoggerFactory.getLogger(RepairOrderServiceImpl.class);
    private final RepairOrderRepository repairOrderRepository;
    private final OrderStateService orderStateService;
    private final FileStorageService fileStorageService;
    private final RepairTypeRepository repairTypeRepository;
    private final RepairLocationRepository repairLocationRepository;
    private final RepairImageRepository repairImageRepository;

    public RepairOrderServiceImpl(RepairOrderRepository repairOrderRepository,
                                  OrderStateService orderStateService,
                                  FileStorageService fileStorageService,
                                  RepairTypeRepository repairTypeRepository,
                                  RepairLocationRepository repairLocationRepository,
                                  RepairImageRepository repairImageRepository) {
        this.repairOrderRepository = repairOrderRepository;
        this.orderStateService = orderStateService;
        this.fileStorageService = fileStorageService;
        this.repairTypeRepository = repairTypeRepository;
        this.repairLocationRepository = repairLocationRepository;
        this.repairImageRepository = repairImageRepository;
    }

    @Override
    @Transactional
    public RepairOrder createOrder(Long reporterId, Long typeId, Long locationId, String title,
                                   String description, List<MultipartFile> files) {
        requireUserId(reporterId);
        validateCreate(typeId, locationId);
        String normalizedTitle = normalizeText(title, "工单标题", 128);
        String normalizedDescription = normalizeText(description, "工单描述", 10000);

        RepairOrder order = new RepairOrder();
        order.setReporterId(reporterId);
        order.setTypeId(typeId);
        order.setLocationId(locationId);
        order.setTitle(normalizedTitle);
        order.setDescription(normalizedDescription);
        order.setStatus(OrderStatus.SUBMITTED.code());
        order.setSubmitTime(LocalDateTime.now());

        RepairOrder saved = repairOrderRepository.insertOrder(order);
        if (saved == null || saved.getOrderId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "工单保存失败");
        }

        List<String> storedUrls = new ArrayList<>();
        try {
            saveImages(saved.getOrderId(), files == null ? Collections.emptyList() : files, storedUrls);
            orderStateService.submit(saved.getOrderId(), reporterId);
            return saved;
        } catch (RuntimeException ex) {
            cleanupFiles(storedUrls);
            throw ex;
        }
    }

    @Override
    @Transactional
    public RepairOrder updateOrder(Long reporterId, Long orderId, String title, String description) {
        RepairOrder order = requireOwnedOrder(reporterId, orderId);
        if (!OrderStatus.SUBMITTED.code().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "工单审核后不能修改内容");
        }
        order.updateContent(normalizeText(title, "工单标题", 128),
                normalizeText(description, "工单描述", 10000));
        repairOrderRepository.updateOrder(order);
        return order;
    }

    @Override
    public List<RepairOrder> getMyOrders(Long reporterId) {
        requireUserId(reporterId);
        return repairOrderRepository.findByReporter(reporterId);
    }

    @Override
    public RepairOrder getProgress(Long reporterId, Long orderId) {
        return requireOwnedOrder(reporterId, orderId);
    }

    private void validateCreate(Long typeId, Long locationId) {
        if (typeId == null || locationId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报修类型和地点不能为空");
        }
        RepairType type = repairTypeRepository.findById(typeId);
        if (type == null || !Integer.valueOf(1).equals(type.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报修类型不存在或已停用");
        }
        RepairLocation location = repairLocationRepository.findById(locationId);
        if (location == null || !Integer.valueOf(1).equals(location.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报修地点不存在或已停用");
        }
    }

    private void saveImages(Long orderId, List<MultipartFile> files, List<String> storedUrls) {
        for (MultipartFile file : files) {
            if (file == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件不能为空");
            String fileName = file.getOriginalFilename();
            try {
                String fileUrl = fileStorageService.save(orderId, fileName, file.getBytes());
                if (fileUrl == null || fileUrl.isBlank()) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
                }
                storedUrls.add(fileUrl);
                RepairImage image = new RepairImage();
                image.setOrderId(orderId);
                image.setFileUrl(fileUrl);
                image.setFileName(fileName);
                image.setUploadTime(LocalDateTime.now());
                repairImageRepository.save(image);
            } catch (IOException ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片读取失败");
            }
        }
    }

    private void cleanupFiles(List<String> storedUrls) {
        for (String fileUrl : storedUrls) {
            try {
                fileStorageService.delete(fileUrl);
            } catch (RuntimeException cleanupFailure) {
                log.warn("清理报修图片失败，路径={}", fileUrl, cleanupFailure);
            }
        }
    }

    private RepairOrder requireOwnedOrder(Long reporterId, Long orderId) {
        requireUserId(reporterId);
        if (orderId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "工单ID不能为空");
        RepairOrder order = repairOrderRepository.findById(orderId);
        if (order == null) throw new BusinessException(ErrorCode.NOT_FOUND, "工单不存在");
        if (!order.isOwnedBy(reporterId)) throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作自己的工单");
        return order;
    }

    private void requireUserId(Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private String normalizeText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + "不能为空");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }
}
