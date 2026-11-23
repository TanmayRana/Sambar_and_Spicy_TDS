package com.sambarspicy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SambarSpicyApplicationTests {

    @Autowired MockMvc mvc;

    @Test
    void contextLoads() {
        // Verifies Spring ApplicationContext and all JPA entity mappings boot cleanly
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unconfiguredApiRoutesAreDeniedEvenForAuthenticatedUsers() throws Exception {
        mvc.perform(get("/api/unconfigured-endpoint")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/unconfigured-endpoint")).andExpect(status().isForbidden());
    }
}
