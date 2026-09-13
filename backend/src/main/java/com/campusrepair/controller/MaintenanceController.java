package com.campusrepair.controller;

import com.campusrepair.common.ApiResponse;
import com.campusrepair.domain.RepairRecord;
import com.campusrepair.dto.MaintenanceProcessRequest;
import com.campusrepair.dto.MaintenanceTaskItem;
import com.campusrepair.security.CurrentUser;
import com.campusrepair.service.MaintenanceService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/maintenance/tasks")
@Validated
@PreAuthorize("hasRole('MAINTAINER')")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ApiResponse<List<MaintenanceTaskItem>> listTasks(Authentication authentication) {
        return ApiResponse.success(maintenanceService.listTasks(CurrentUser.requireId(authentication)));
    }

    @PostMapping("/{orderId}/accept")
    public ApiResponse<Void> acceptTask(@PathVariable Long orderId, Authentication authentication) {
        maintenanceService.acceptTask(CurrentUser.requireId(authentication), orderId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderId}/process")
    public ApiResponse<Void> processTask(@PathVariable Long orderId,
                                         @Valid @RequestBody MaintenanceProcessRequest request,
                                         Authentication authentication) {
        maintenanceService.processRepairOrder(CurrentUser.requireId(authentication), orderId, request);
        return ApiResponse.success(null);
    }

    @PostMapping("/{orderId}/continue")
    public ApiResponse<Void> continueRepair(@PathVariable Long orderId, Authentication authentication) {
        maintenanceService.continueRepair(CurrentUser.requireId(authentication), orderId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{orderId}/history")
    public ApiResponse<List<RepairRecord>> getHistory(@PathVariable Long orderId, Authentication authentication) {
        CurrentUser.requireId(authentication);
        return ApiResponse.success(maintenanceService.getRepairHistory(orderId));
    }
}
