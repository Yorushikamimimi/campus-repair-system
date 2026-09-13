package com.campusrepair.service.impl;

import com.campusrepair.domain.RepairLocation;
import com.campusrepair.domain.RepairType;
import com.campusrepair.repository.RepairLocationRepository;
import com.campusrepair.repository.RepairTypeRepository;
import com.campusrepair.service.RepairCatalogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepairCatalogServiceImpl implements RepairCatalogService {
    private final RepairTypeRepository repairTypeRepository;
    private final RepairLocationRepository repairLocationRepository;

    public RepairCatalogServiceImpl(RepairTypeRepository repairTypeRepository,
                                    RepairLocationRepository repairLocationRepository) {
        this.repairTypeRepository = repairTypeRepository;
        this.repairLocationRepository = repairLocationRepository;
    }

    @Override
    public List<RepairType> listEnabledTypes() {
        return repairTypeRepository.findAllEnabled();
    }

    @Override
    public List<RepairLocation> listEnabledLocations() {
        return repairLocationRepository.findAllEnabled();
    }
}
