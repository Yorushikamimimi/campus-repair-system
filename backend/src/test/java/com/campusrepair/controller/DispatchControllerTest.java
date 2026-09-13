package com.campusrepair.controller;

import com.campusrepair.domain.RepairOrder;
import com.campusrepair.domain.RepairRecord;
import com.campusrepair.security.ApiSecurityExceptionHandler;
import com.campusrepair.security.JwtAuthenticationFilter;
import com.campusrepair.security.JwtTokenProvider;
import com.campusrepair.security.SecurityConfig;
import com.campusrepair.service.DispatchService;
import com.campusrepair.service.MaintenanceService;
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

@WebMvcTest(DispatchController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiSecurityExceptionHandler.class})
class DispatchControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    DispatchService dispatchService;
    @MockBean
    MaintenanceService maintenanceService;
    @MockBean
    JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanGetPendingOrders() throws Exception {
        when(dispatchService.listPendingOrders()).thenReturn(java.util.List.of(order(10L, "SUBMITTED")));

        mockMvc.perform(get("/api/admin/repair-orders/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderId").value(10));

        verify(dispatchService).listPendingOrders();
    }

    @Test
    @WithMockUser(username = "1", roles = "REPORTER")
    void reporterCannotAccessAdminDispatch() throws Exception {
        mockMvc.perform(get("/api/admin/repair-orders/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousCannotAccessAdminDispatch() throws Exception {
        mockMvc.perform(get("/api/admin/repair-orders/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanAuditOrder() throws Exception {
        mockMvc.perform(post("/api/admin/repair-orders/20/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true,\"comment\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dispatchService).auditOrder(1L, 20L, true, "ok");
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanDispatchOrder() throws Exception {
        mockMvc.perform(post("/api/admin/repair-orders/20/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maintainerId\":8,\"note\":\"先派给张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(dispatchService).dispatchOrder(1L, 20L, 8L, "先派给张三");
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanViewOrderHistoryFromAdminEndpoint() throws Exception {
        RepairRecord record = new RepairRecord();
        record.setOrderId(20L);
        when(maintenanceService.getRepairHistory(20L)).thenReturn(java.util.List.of(record));

        mockMvc.perform(get("/api/admin/repair-orders/20/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderId").value(20));

        verify(maintenanceService).getRepairHistory(20L);
    }

    private RepairOrder order(Long orderId, String status) {
        RepairOrder order = new RepairOrder();
        order.setOrderId(orderId);
        order.setStatus(status);
        return order;
    }
}
