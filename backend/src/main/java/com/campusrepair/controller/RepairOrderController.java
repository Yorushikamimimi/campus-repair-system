package com.campusrepair.controller;

import com.campusrepair.common.ApiResponse;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.dto.RepairOrderCreateRequest;
import com.campusrepair.dto.RepairOrderUpdateRequest;
import com.campusrepair.security.CurrentUser;
import com.campusrepair.service.RepairOrderService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/repair-orders")
@PreAuthorize("hasAnyRole('REPORTER', 'ADMIN', 'MAINTAINER')")
public class RepairOrderController {
    private final RepairOrderService repairOrderService;
    public RepairOrderController(RepairOrderService repairOrderService) {
        this.repairOrderService = repairOrderService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<RepairOrder> createOrder(@Valid @RequestBody RepairOrderCreateRequest request,
                                                Authentication authentication) {
        return ApiResponse.success(repairOrderService.createOrder(
                CurrentUser.requireId(authentication), request.getTypeId(), request.getLocationId(),
                request.getTitle(), request.getDescription(), Collections.emptyList()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RepairOrder> createOrderWithImages(@Valid @ModelAttribute RepairOrderCreateRequest request,
                                                          @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                                          Authentication authentication) {
        return ApiResponse.success(repairOrderService.createOrder(
                CurrentUser.requireId(authentication), request.getTypeId(), request.getLocationId(),
                request.getTitle(), request.getDescription(), files == null ? Collections.emptyList() : files));
    }

    @GetMapping("/my")
    public ApiResponse<List<RepairOrder>> getMyOrders(Authentication authentication) {
        return ApiResponse.success(repairOrderService.getMyOrders(CurrentUser.requireId(authentication)));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<RepairOrder> getProgress(@PathVariable Long orderId, Authentication authentication) {
        return ApiResponse.success(repairOrderService.getProgress(CurrentUser.requireId(authentication), orderId));
    }

    @PutMapping("/{orderId}")
    public ApiResponse<RepairOrder> updateOrder(@PathVariable Long orderId,
                                                @Valid @RequestBody RepairOrderUpdateRequest request,
                                                Authentication authentication) {
        return ApiResponse.success(repairOrderService.updateOrder(
                CurrentUser.requireId(authentication), orderId, request.getTitle(), request.getDescription()));
    }

}
