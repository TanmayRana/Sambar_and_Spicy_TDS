package com.sambarspicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.sambarspicy.model.StoreSetting;
import com.sambarspicy.repository.StoreSettingRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StoreControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreSettingRepository stores;

    @BeforeEach
    void ensureStoreExists() {
        if (!stores.existsById(1)) {
            stores.saveAndFlush(new StoreSetting("Test store", "", ""));
        }
    }

    @Test
    @WithMockUser(username = "cashier", roles = "BILLING")
    void billingCanReadButCannotModifyStore() throws Exception {
        mockMvc.perform(get("/api/v1/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency_code").value("INR"));
        mockMvc.perform(patch("/api/v1/admin/store").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"store_name\":\"Changed\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanUpdateStoreWithCsrf() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/store").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"store_name\":\"Updated store\",\"receipt_footer\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store_name").value("Updated store"))
                .andExpect(jsonPath("$.receipt_footer").doesNotExist());
        mockMvc.perform(get("/api/v1/store"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store_name").value("Updated store"));
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password_hash").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entity_type").value("store_settings"));
    }

    @Test
    @WithMockUser(username = "viewer", roles = "READ_ONLY")
    void readOnlyAccountCannotReadAdministrativeUsersOrAudits() throws Exception {
        mockMvc.perform(get("/api/v1/store")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/audit-logs")).andExpect(status().isForbidden());
    }
}
