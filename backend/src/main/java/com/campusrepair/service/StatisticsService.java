package com.campusrepair.service;

import com.campusrepair.dto.TypeCountItem;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface StatisticsService {
    Map<String, Long> countByStatus();
    List<TypeCountItem> countByType();
    long countByPeriod(LocalDate start, LocalDate end);
    double averageRepairDuration();
}

