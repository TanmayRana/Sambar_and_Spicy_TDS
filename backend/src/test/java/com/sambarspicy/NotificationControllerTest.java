package com.sambarspicy;

import com.sambarspicy.enums.InventoryUnit;
import com.sambarspicy.enums.NotificationSeverity;
import com.sambarspicy.model.InventoryItem;
import com.sambarspicy.model.Notification;
import com.sambarspicy.repository.InventoryItemRepository;
import com.sambarspicy.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @Autowired InventoryItemRepository inventory;
    @Autowired NotificationRepository notifications;
    private Long notificationId;
    private Long itemId;

    @BeforeEach
    void createAlert() {
        InventoryItem item = inventory.saveAndFlush(new InventoryItem("Rice " + UUID.randomUUID(),
                InventoryUnit.KG, new BigDecimal("5"), new BigDecimal("2")));
        itemId = item.getId();
        notificationId = notifications.saveAndFlush(new Notification(item, NotificationSeverity.WARNING,
                "Rice is low", new BigDecimal("1"))).getId();
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void adminCanReadAndAcknowledgeAlertWithoutResolvingIt() throws Exception {
        mvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inventory_item_id").value(itemId))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
        mvc.perform(post("/api/v1/notifications/{id}/read", notificationId))
                .andExpect(status().isForbidden());
        String readAt = mvc.perform(post("/api/v1/notifications/{id}/read", notificationId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read_by").value(1))
                .andExpect(jsonPath("$.read_at").isNotEmpty())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/notifications/{id}/read", notificationId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json(readAt));
        mvc.perform(post("/api/v1/notifications/999999/read").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username="viewer", roles="READ_ONLY")
    void readOnlyCanViewButCannotAcknowledge() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/notifications/{id}/read", notificationId).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username="cashier", roles="BILLING")
    void billingCannotAccessStockAlerts() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/notifications/{id}/read", notificationId).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
