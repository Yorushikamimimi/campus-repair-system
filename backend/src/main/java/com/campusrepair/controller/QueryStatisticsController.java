package com.campusrepair.controller;

import com.campusrepair.common.ApiResponse;
import com.campusrepair.dto.TypeCountItem;
import com.campusrepair.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class QueryStatisticsController {
    private final StatisticsService statisticsService;

    public QueryStatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Long>> getStatusStats() {
        return ApiResponse.success(statisticsService.countByStatus());
    }

    @GetMapping("/type")
    public ApiResponse<List<TypeCountItem>> getTypeStats() {
        return ApiResponse.success(statisticsService.countByType());
    }

    @GetMapping("/period")
    public ApiResponse<Long> getPeriodStats(@RequestParam("start")
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                           @RequestParam("end")
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.success(statisticsService.countByPeriod(start, end));
    }

    @GetMapping("/repair-duration")
    public ApiResponse<Double> getAverageRepairDuration() {
        return ApiResponse.success(statisticsService.averageRepairDuration());
    }
}
