package com.campusrepair.controller;

import com.campusrepair.common.ApiResponse;
import com.campusrepair.domain.RepairLocation;
import com.campusrepair.domain.RepairType;
import com.campusrepair.service.RepairCatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('REPORTER', 'ADMIN', 'MAINTAINER')")
public class RepairCatalogController {
    private final RepairCatalogService repairCatalogService;

    public RepairCatalogController(RepairCatalogService repairCatalogService) {
        this.repairCatalogService = repairCatalogService;
    }

    @GetMapping("/repair-types")
    public ApiResponse<List<RepairType>> getRepairTypes() {
        return ApiResponse.success(repairCatalogService.listEnabledTypes());
    }

    @GetMapping("/repair-locations")
    public ApiResponse<List<RepairLocation>> getRepairLocations() {
        return ApiResponse.success(repairCatalogService.listEnabledLocations());
    }
}
