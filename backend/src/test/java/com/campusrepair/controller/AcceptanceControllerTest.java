package com.campusrepair.controller;

import com.campusrepair.domain.AcceptanceRecord;
import com.campusrepair.domain.EvaluationRecord;
import com.campusrepair.security.ApiSecurityExceptionHandler;
import com.campusrepair.security.JwtAuthenticationFilter;
import com.campusrepair.security.JwtTokenProvider;
import com.campusrepair.security.SecurityConfig;
import com.campusrepair.service.AcceptanceService;
import com.campusrepair.service.EvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcceptanceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiSecurityExceptionHandler.class})
class AcceptanceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcceptanceService acceptanceService;
    @MockBean
    private EvaluationService evaluationService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void reporterCanSubmitAcceptancePass() throws Exception {
        when(acceptanceService.acceptRepairResult(9L, 20L, true, null)).thenReturn(acceptance(20L, 9L, "PASSED", null));

        mockMvc.perform(post("/api/repair-orders/20/acceptance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(acceptanceService).acceptRepairResult(9L, 20L, true, null);
    }

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void reporterCanSubmitRework() throws Exception {
        when(acceptanceService.requestRework(9L, 20L, "零件不足")).thenReturn(acceptance(20L, 9L, "REJECTED", "零件不足"));

        mockMvc.perform(post("/api/repair-orders/20/rework")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"returnReason\":\"零件不足\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(acceptanceService).requestRework(9L, 20L, "零件不足");
    }

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void reporterCanGetAcceptanceHistory() throws Exception {
        mockMvc.perform(get("/api/repair-orders/20/acceptance-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(acceptanceService).getAcceptanceHistory(9L, 20L, false);
    }

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void reporterCanEvaluateOrder() throws Exception {
        when(evaluationService.evaluateService(9L, 20L, 5, "满意")).thenReturn(evaluation(30L, 20L, 5));

        mockMvc.perform(post("/api/repair-orders/20/evaluation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":5,\"comment\":\"满意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(evaluationService).evaluateService(9L, 20L, 5, "满意");
    }

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void reporterCanReadOwnEvaluation() throws Exception {
        when(evaluationService.getEvaluation(9L, 20L, false)).thenReturn(evaluation(30L, 20L, 4));

        mockMvc.perform(get("/api/repair-orders/20/evaluation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.score").value(4));

        verify(evaluationService).getEvaluation(9L, 20L, false);
    }

    @Test
    @WithMockUser(username = "8", roles = "MAINTAINER")
    void maintainerCannotSubmitAcceptance() throws Exception {
        mockMvc.perform(post("/api/repair-orders/20/acceptance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passed\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousCannotSubmitAcceptance() throws Exception {
        mockMvc.perform(post("/api/repair-orders/20/acceptance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passed\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    private AcceptanceRecord acceptance(Long orderId, Long reporterId, String result, String reason) {
        AcceptanceRecord record = new AcceptanceRecord();
        record.setOrderId(orderId);
        record.setReporterId(reporterId);
        record.setAcceptResult(result);
        record.setReturnReason(reason);
        return record;
    }

    private EvaluationRecord evaluation(Long evaluationId, Long orderId, Integer score) {
        EvaluationRecord record = new EvaluationRecord();
        record.setEvaluationId(evaluationId);
        record.setOrderId(orderId);
        record.setScore(score);
        return record;
    }
}

