package com.campusrepair.controller;

import com.campusrepair.security.ApiSecurityExceptionHandler;
import com.campusrepair.security.JwtAuthenticationFilter;
import com.campusrepair.security.JwtTokenProvider;
import com.campusrepair.security.SecurityConfig;
import com.campusrepair.service.MaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiSecurityExceptionHandler.class})
class MaintenanceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceService maintenanceService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "8", roles = "MAINTAINER")
    void maintainerCanListAssignedTasks() throws Exception {
        mockMvc.perform(get("/api/maintenance/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(maintenanceService).listTasks(8L);
    }

    @Test
    @WithMockUser(username = "8", roles = "MAINTAINER")
    void maintainerCanSubmitProcessAndAccept() throws Exception {
        mockMvc.perform(post("/api/maintenance/tasks/10/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(maintenanceService).acceptTask(8L, 10L);

        mockMvc.perform(post("/api/maintenance/tasks/10/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processDesc\":\"已到场\",\"completed\":true,\"repairResult\":\"已完成\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(maintenanceService).processRepairOrder(eq(8L), eq(10L), any());

        mockMvc.perform(post("/api/maintenance/tasks/10/continue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(maintenanceService).continueRepair(8L, 10L);
    }

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void reporterCannotAccessMaintenanceTasks() throws Exception {
        mockMvc.perform(get("/api/maintenance/tasks"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousCannotAccessMaintenanceTasks() throws Exception {
        mockMvc.perform(get("/api/maintenance/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
