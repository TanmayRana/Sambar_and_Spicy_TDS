package com.sambarspicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sambarspicy.dto.CategoryRequest;
import com.sambarspicy.dto.MenuItemRequest;
import com.sambarspicy.model.*;
import com.sambarspicy.repository.BillItemRepository;
import com.sambarspicy.repository.BillRepository;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillItemRepository billItemRepository;

    @Autowired
    private com.sambarspicy.repository.UserRepository userRepository;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        menuItemRepository.deleteAll();
        categoryRepository.deleteAll();

        Category cat = new Category();
        cat.setName("South Indian Delights");
        cat.setSortOrder(1);
        cat.setIsActive(true);
        testCategory = categoryRepository.save(cat);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can create category successfully")
    void testAdminCreateCategory() throws Exception {
        CategoryRequest req = new CategoryRequest("Beverages", null, 2, true);

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Beverages")))
                .andExpect(jsonPath("$.sort_order", is(2)))
                .andExpect(jsonPath("$.is_active", is(true)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Reject duplicate category name with 409 Conflict")
    void testRejectDuplicateCategory() throws Exception {
        CategoryRequest req = new CategoryRequest("South Indian Delights", null, 3, true);

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can edit and deactivate category")
    void testAdminEditAndDeactivateCategory() throws Exception {
        CategoryRequest updateReq = new CategoryRequest();
        updateReq.setName("South Indian Tiffins");
        updateReq.setIsActive(false);

        mockMvc.perform(patch("/api/v1/categories/" + testCategory.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("South Indian Tiffins")))
                .andExpect(jsonPath("$.is_active", is(false)));

        Category updated = categoryRepository.findById(testCategory.getId()).orElseThrow();
        assertFalse(updated.getIsActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cannot delete category with associated menu items")
    void testCannotDeleteCategoryWithItems() throws Exception {
        MenuItem item = new MenuItem();
        item.setCategory(testCategory);
        item.setName("Ghee Roast");
        item.setSellingPrice(new BigDecimal("120.00"));
        item.setIsAvailable(true);
        menuItemRepository.save(item);

        mockMvc.perform(delete("/api/v1/categories/" + testCategory.getId()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("contains associated menu items")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can create dish with full and half portion options")
    void testCreateDishWithPortions() throws Exception {
        MenuItemRequest req = new MenuItemRequest();
        req.setCategoryId(testCategory.getId());
        req.setName("Paneer Butter Masala");
        req.setSku("PBM-01");
        req.setDescription("Rich tomato gravy dish");
        req.setSellingPrice(new BigDecimal("220.00"));
        req.setHasHalfPortion(true);
        req.setHalfSellingPrice(new BigDecimal("130.00"));
        req.setEstimatedUnitCost(new BigDecimal("90.00"));
        req.setHalfEstimatedUnitCost(new BigDecimal("55.00"));
        req.setSortOrder(1);
        req.setIsAvailable(true);

        mockMvc.perform(post("/api/v1/menu-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Paneer Butter Masala")))
                .andExpect(jsonPath("$.selling_price", is(220.00)))
                .andExpect(jsonPath("$.has_half_portion", is(true)))
                .andExpect(jsonPath("$.half_selling_price", is(130.00)))
                .andExpect(jsonPath("$.half_estimated_unit_cost", is(55.00)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Dietary type survives create, read and partial updates")
    void testDietaryTypePersistence() throws Exception {
        MenuItemRequest create = new MenuItemRequest();
        create.setCategoryId(testCategory.getId());
        create.setName("Vegetable dosa");
        create.setSellingPrice(new BigDecimal("70.00"));
        create.setDietaryType("VEG");

        String response = mockMvc.perform(post("/api/v1/menu-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dietary_type", is("VEG")))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/menu-items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietary_type", is("VEG")));

        mockMvc.perform(get("/api/v1/menu-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dietary_type", is("VEG")));

        mockMvc.perform(patch("/api/v1/menu-items/" + id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"is_available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietary_type", is("VEG")));

        mockMvc.perform(patch("/api/v1/menu-items/" + id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dietary_type\":\"EGG\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietary_type", is("EGG")));

        mockMvc.perform(patch("/api/v1/menu-items/" + id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dietary_type\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/menu-items/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dietary_type", is("EGG")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Patch preserves omitted fields and clears explicit nulls")
    void testClearOptionalFields() throws Exception {
        testCategory.setImagePath("/uploads/category.png");
        categoryRepository.saveAndFlush(testCategory);
        mockMvc.perform(patch("/api/v1/categories/" + testCategory.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sort_order\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.image_path", is("/uploads/category.png")));
        mockMvc.perform(patch("/api/v1/categories/" + testCategory.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"image_path\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.image_path").doesNotExist());

        MenuItem dish = new MenuItem();
        dish.setCategory(testCategory);
        dish.setName("Dosa");
        dish.setSku("DOS-1");
        dish.setDescription("A dish");
        dish.setSellingPrice(new BigDecimal("80.00"));
        dish.setEstimatedUnitCost(new BigDecimal("20.00"));
        dish.setImagePath("/uploads/dish.png");
        dish = menuItemRepository.saveAndFlush(dish);
        mockMvc.perform(patch("/api/v1/menu-items/" + dish.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"is_available\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku", is("DOS-1")))
                .andExpect(jsonPath("$.description", is("A dish")));
        mockMvc.perform(patch("/api/v1/menu-items/" + dish.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":null,\"description\":null,\"estimated_unit_cost\":null,\"image_path\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sku").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.estimated_unit_cost").doesNotExist())
                .andExpect(jsonPath("$.image_path").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Image uploads reject a file whose contents do not match its extension")
    void testRejectFakeImage() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("file", "dish.png", "image/png", "not a png".getBytes());
        mockMvc.perform(multipart("/api/v1/categories/" + testCategory.getId() + "/image")
                        .file(fake).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Reject half portion dish without half selling price")
    void testRejectHalfPortionWithoutPrice() throws Exception {
        MenuItemRequest req = new MenuItemRequest();
        req.setCategoryId(testCategory.getId());
        req.setName("Special Biryani");
        req.setSellingPrice(new BigDecimal("200.00"));
        req.setHasHalfPortion(true);
        req.setHalfSellingPrice(null);

        mockMvc.perform(post("/api/v1/menu-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Half selling price is required")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can change dish availability")
    void testChangeDishAvailability() throws Exception {
        MenuItem item = new MenuItem();
        item.setCategory(testCategory);
        item.setName("Mysore Pak");
        item.setSellingPrice(new BigDecimal("50.00"));
        item.setIsAvailable(true);
        MenuItem saved = menuItemRepository.save(item);

        MenuItemRequest patchReq = new MenuItemRequest();
        patchReq.setIsAvailable(false);

        mockMvc.perform(patch("/api/v1/menu-items/" + saved.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_available", is(false)));

        MenuItem updated = menuItemRepository.findById(saved.getId()).orElseThrow();
        assertFalse(updated.getIsAvailable());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cannot delete menu item that has bill history")
    void testCannotDeleteMenuItemWithBillHistory() throws Exception {
        MenuItem item = new MenuItem();
        item.setCategory(testCategory);
        item.setName("Masala Chai");
        item.setSellingPrice(new BigDecimal("30.00"));
        item.setIsAvailable(true);
        MenuItem saved = menuItemRepository.save(item);

        User adminUser = userRepository.findById(1).orElseGet(() -> {
            User u = new User();
            u.setId(1);
            u.setUsername("admin");
            u.setFullName("Admin");
            u.setRole(com.sambarspicy.enums.UserRole.ADMIN);
            u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuu");
            u.setIsActive(true);
            return userRepository.save(u);
        });

        Bill bill = new Bill();
        bill.setBusinessDate(LocalDate.now());
        bill.setStatus(com.sambarspicy.enums.BillStatus.PAID);
        bill.setBillNumber("SNS-20261121-0001");
        bill.setSubtotalAmount(new BigDecimal("30.00"));
        bill.setTotalAmount(new BigDecimal("30.00"));
        bill.setItemCount(1);
        bill.setCreatedBy(adminUser);
        Bill savedBill = billRepository.save(bill);

        BillItem billItem = new BillItem(savedBill, saved, "Masala Chai", 1, new BigDecimal("30.00"), com.sambarspicy.enums.PortionSize.FULL);
        billItemRepository.save(billItem);

        mockMvc.perform(delete("/api/v1/menu-items/" + saved.getId()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("referenced in past bills")));
    }

    @Test
    @WithMockUser(roles = "BILLING")
    @DisplayName("BILLING role cannot create category (403 Forbidden)")
    void testBillingCannotCreateCategory() throws Exception {
        CategoryRequest req = new CategoryRequest("Juices", null, 4, true);

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY role cannot create dish (403 Forbidden)")
    void testReadOnlyCannotCreateDish() throws Exception {
        MenuItemRequest req = new MenuItemRequest();
        req.setCategoryId(testCategory.getId());
        req.setName("Filter Coffee");
        req.setSellingPrice(new BigDecimal("40.00"));

        mockMvc.perform(post("/api/v1/menu-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY role can view categories and menu items")
    void testReadOnlyCanViewMenu() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/menu-items"))
                .andExpect(status().isOk());
    }
}
