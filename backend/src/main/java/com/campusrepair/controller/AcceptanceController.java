package com.campusrepair.controller;

import com.campusrepair.common.ApiResponse;
import com.campusrepair.domain.AcceptanceRecord;
import com.campusrepair.domain.EvaluationRecord;
import com.campusrepair.dto.AcceptanceRequest;
import com.campusrepair.dto.EvaluationRequest;
import com.campusrepair.dto.ReworkRequest;
import com.campusrepair.security.CurrentUser;
import com.campusrepair.service.AcceptanceService;
import com.campusrepair.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/repair-orders")
@Validated
public class AcceptanceController {
    private final AcceptanceService acceptanceService;
    private final EvaluationService evaluationService;

    public AcceptanceController(AcceptanceService acceptanceService,
                               EvaluationService evaluationService) {
        this.acceptanceService = acceptanceService;
        this.evaluationService = evaluationService;
    }

    @PostMapping("/{orderId}/acceptance")
    @PreAuthorize("hasRole('REPORTER')")
    public ApiResponse<AcceptanceRecord> acceptRepair(@PathVariable Long orderId,
                                                     @Valid @RequestBody AcceptanceRequest request,
                                                     Authentication authentication) {
        AcceptanceRecord record = acceptanceService.acceptRepairResult(
                CurrentUser.requireId(authentication),
                orderId,
                request.getPassed(),
                request.getReturnReason()
        );
        return ApiResponse.success(record);
    }

    @PostMapping("/{orderId}/rework")
    @PreAuthorize("hasRole('REPORTER')")
    public ApiResponse<AcceptanceRecord> requestRework(@PathVariable Long orderId,
                                                      @Valid @RequestBody ReworkRequest request,
                                                      Authentication authentication) {
        AcceptanceRecord record = acceptanceService.requestRework(
                CurrentUser.requireId(authentication),
                orderId,
                request.getReturnReason());
        return ApiResponse.success(record);
    }

    @GetMapping("/{orderId}/acceptance-history")
    @PreAuthorize("hasAnyRole('REPORTER','ADMIN')")
    public ApiResponse<List<AcceptanceRecord>> getAcceptanceHistory(@PathVariable Long orderId,
                                                                   Authentication authentication) {
        var userId = CurrentUser.requireId(authentication);
        boolean allowAdmin = hasAdminAuthority(authentication);
        return ApiResponse.success(acceptanceService.getAcceptanceHistory(userId, orderId, allowAdmin));
    }

    @PostMapping("/{orderId}/evaluation")
    @PreAuthorize("hasRole('REPORTER')")
    public ApiResponse<EvaluationRecord> evaluate(@PathVariable Long orderId,
                                                 @Valid @RequestBody EvaluationRequest request,
                                                 Authentication authentication) {
        EvaluationRecord evaluation = evaluationService.evaluateService(
                CurrentUser.requireId(authentication),
                orderId,
                request.getScore(),
                request.getComment());
        return ApiResponse.success(evaluation);
    }

    @GetMapping("/{orderId}/evaluation")
    @PreAuthorize("hasAnyRole('REPORTER','ADMIN')")
    public ApiResponse<EvaluationRecord> getEvaluation(@PathVariable Long orderId,
                                                      Authentication authentication) {
        var userId = CurrentUser.requireId(authentication);
        boolean allowAdmin = hasAdminAuthority(authentication);
        return ApiResponse.success(evaluationService.getEvaluation(userId, orderId, allowAdmin));
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
