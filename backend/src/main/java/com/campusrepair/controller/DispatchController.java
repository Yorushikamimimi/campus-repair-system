package com.campusrepair.controller;

import com.campusrepair.common.ApiResponse;
import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.RepairRecord;
import com.campusrepair.dto.DispatchAuditRequest;
import com.campusrepair.dto.DispatchOrderRequest;
import com.campusrepair.security.CurrentUser;
import com.campusrepair.service.MaintenanceService;
import com.campusrepair.service.DispatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/repair-orders")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class DispatchController {
    private final DispatchService dispatchService;
    private final MaintenanceService maintenanceService;

    public DispatchController(DispatchService dispatchService, MaintenanceService maintenanceService) {
        this.dispatchService = dispatchService;
        this.maintenanceService = maintenanceService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<RepairOrder>> listPending(Authentication authentication) {
        CurrentUser.requireId(authentication);
        return ApiResponse.success(dispatchService.listPendingOrders());
    }

    @PostMapping("/{orderId}/audit")
    public ApiResponse<Void> auditOrder(@PathVariable Long orderId,
                                        @Valid @RequestBody DispatchAuditRequest request,
                                        Authentication authentication) {
        dispatchService.auditOrder(CurrentUser.requireId(authentication), orderId,
                Boolean.TRUE.equals(request.getApproved()), request.getComment());
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderId}/dispatch")
    public ApiResponse<Void> dispatchOrder(@PathVariable Long orderId,
                                          @Valid @RequestBody DispatchOrderRequest request,
                                          Authentication authentication) {
        dispatchService.dispatchOrder(CurrentUser.requireId(authentication), orderId,
                request.getMaintainerId(), request.getNote());
        return new ApiResponse<>(HttpStatus.OK.value(), "success", null);
    }

    @GetMapping
    public ApiResponse<List<RepairOrder>> listAll(Authentication authentication) {
        CurrentUser.requireId(authentication);
        return ApiResponse.success(dispatchService.listPendingOrders());
    }

    @GetMapping("/{orderId}/history")
    public ApiResponse<List<RepairRecord>> getOrderHistory(@PathVariable Long orderId, Authentication authentication) {
        CurrentUser.requireId(authentication);
        return ApiResponse.success(maintenanceService.getRepairHistory(orderId));
    }
}
