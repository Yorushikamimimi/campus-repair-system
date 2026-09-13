package com.campusrepair.controller;

import com.campusrepair.domain.RepairOrder;
import com.campusrepair.security.JwtAuthenticationFilter;
import com.campusrepair.security.JwtTokenProvider;
import com.campusrepair.security.ApiSecurityExceptionHandler;
import com.campusrepair.security.SecurityConfig;
import com.campusrepair.service.RepairOrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RepairOrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiSecurityExceptionHandler.class})
class RepairOrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean RepairOrderService repairOrderService;
    @MockBean JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "9", roles = "REPORTER")
    void createOrderEndpointUsesAuthenticatedReporter() throws Exception {
        RepairOrder order = new RepairOrder();
        order.setOrderId(100L);
        order.setReporterId(9L);
        order.setStatus("SUBMITTED");
        when(repairOrderService.createOrder(eq(9L), eq(1L), eq(2L), eq("标题"), eq("描述"), any()))
                .thenReturn(order);

        mockMvc.perform(post("/api/repair-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeId\":1,\"locationId\":2,\"title\":\"标题\",\"description\":\"描述\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderId").value(100));

        verify(repairOrderService).createOrder(eq(9L), eq(1L), eq(2L), eq("标题"), eq("描述"), any());
    }

    @Test
    void unauthenticatedOrderRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/repair-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeId\":1,\"locationId\":2,\"title\":\"标题\",\"description\":\"描述\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }
}
