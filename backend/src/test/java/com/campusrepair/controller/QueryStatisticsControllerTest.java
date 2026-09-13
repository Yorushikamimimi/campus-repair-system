package com.campusrepair.controller;

import com.campusrepair.dto.TypeCountItem;
import com.campusrepair.security.ApiSecurityExceptionHandler;
import com.campusrepair.security.JwtAuthenticationFilter;
import com.campusrepair.security.JwtTokenProvider;
import com.campusrepair.security.SecurityConfig;
import com.campusrepair.service.StatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryStatisticsController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiSecurityExceptionHandler.class})
class QueryStatisticsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanGetStatusStatistics() throws Exception {
        when(statisticsService.countByStatus()).thenReturn(Map.of("SUBMITTED", 1L));
        mockMvc.perform(get("/api/admin/statistics/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.SUBMITTED").value(1));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanGetTypeStatistics() throws Exception {
        when(statisticsService.countByType()).thenReturn(List.of(new TypeCountItem(1L, "水电维修", 3L)));
        mockMvc.perform(get("/api/admin/statistics/type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].typeName").value("水电维修"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanGetPeriodStatistics() throws Exception {
        when(statisticsService.countByPeriod(
                java.time.LocalDate.of(2026, 9, 1),
                java.time.LocalDate.of(2026, 9, 2)
        )).thenReturn(5L);

        mockMvc.perform(get("/api/admin/statistics/period")
                        .param("start", "2026-09-01")
                        .param("end", "2026-09-02")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(5));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanGetRepairDurationStatistics() throws Exception {
        when(statisticsService.averageRepairDuration()).thenReturn(35.25);
        mockMvc.perform(get("/api/admin/statistics/repair-duration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(35.25));
    }

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void reporterCannotAccessAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(username = "8", roles = "MAINTAINER")
    void maintainerCannotAccessAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/repair-duration"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousCannotAccessAdminStatistics() throws Exception {
        mockMvc.perform(get("/api/admin/statistics/type"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}

