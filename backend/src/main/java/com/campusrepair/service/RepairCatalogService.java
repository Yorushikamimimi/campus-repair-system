package com.campusrepair.service;

import com.campusrepair.domain.RepairLocation;
import com.campusrepair.domain.RepairType;

import java.util.List;

public interface RepairCatalogService {
    List<RepairType> listEnabledTypes();
    List<RepairLocation> listEnabledLocations();
}
