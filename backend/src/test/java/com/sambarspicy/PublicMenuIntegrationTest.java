package com.sambarspicy;

import com.sambarspicy.model.Category;
import com.sambarspicy.model.MenuItem;
import com.sambarspicy.model.StoreSetting;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import com.sambarspicy.repository.StoreSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PublicMenuIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired MenuItemRepository items;
    @Autowired StoreSettingRepository stores;

    @Test
    void guestsSeeOnlyAvailablePublicFieldsAndCannotAccessStaffEndpoints() throws Exception {
        if (!stores.existsById(1)) stores.saveAndFlush(new StoreSetting("Test kitchen", "Test street", "123"));
        Category active = new Category();
        active.setName("Public test category");
        active.setIsActive(true);
        active = categories.saveAndFlush(active);
        Category inactive = new Category();
        inactive.setName("Hidden test category");
        inactive.setIsActive(false);
        inactive = categories.saveAndFlush(inactive);
        MenuItem visible = new MenuItem(active, "Public test dosa", new BigDecimal("80.00"));
        visible.setEstimatedUnitCost(new BigDecimal("20.00"));
        visible.setSku("PRIVATE-SKU");
        items.saveAndFlush(visible);
        MenuItem unavailable = new MenuItem(active, "Unavailable test dosa", BigDecimal.TEN);
        unavailable.setIsAvailable(false);
        items.saveAndFlush(unavailable);
        items.saveAndFlush(new MenuItem(inactive, "Hidden category dish", BigDecimal.TEN));

        mvc.perform(get("/api/v1/public/menu"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.store.name", not(isEmptyOrNullString())))
                .andExpect(jsonPath("$.items[*].name", hasItem("Public test dosa")))
                .andExpect(jsonPath("$.items[*].name", not(hasItem("Unavailable test dosa"))))
                .andExpect(jsonPath("$.items[*].name", not(hasItem("Hidden category dish"))))
                .andExpect(jsonPath("$.categories[*].name", not(hasItem("Hidden test category"))))
                .andExpect(jsonPath("$.items[?(@.name == 'Public test dosa')].sku").doesNotExist())
                .andExpect(jsonPath("$.items[?(@.name == 'Public test dosa')].estimatedUnitCost").doesNotExist());
        mvc.perform(get("/api/v1/menu-items")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/bills")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/public/menu").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
