package com.sambarspicy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sambarspicy.model.Category;
import com.sambarspicy.model.MenuItem;
import com.sambarspicy.model.StoreSetting;
import com.sambarspicy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired StoreSettingRepository stores;
    @Autowired CategoryRepository categories;
    @Autowired MenuItemRepository menu;
    private Long itemId;

    @BeforeEach
    void setup() {
        if (stores.findById(1).isEmpty()) stores.saveAndFlush(new StoreSetting("Test store", "Address", "123"));
        Category category = new Category();
        category.setName("Billing test " + UUID.randomUUID());
        category.setIsActive(true);
        category = categories.saveAndFlush(category);
        MenuItem item = new MenuItem();
        item.setCategory(category); item.setName("Dosa"); item.setSellingPrice(new BigDecimal("100.00"));
        item.setHasHalfPortion(true); item.setHalfSellingPrice(new BigDecimal("60.00"));
        item.setHalfEstimatedUnitCost(new BigDecimal("20.00")); item.setIsAvailable(true);
        itemId = menu.saveAndFlush(item).getId();
    }
    private String lines() {
        return "[{\"menu_item_id\":" + itemId + ",\"portion\":\"FULL\",\"quantity\":1,\"expected_unit_price\":100}," +
                "{\"menu_item_id\":" + itemId + ",\"portion\":\"HALF\",\"quantity\":2,\"expected_unit_price\":60}]";
    }
    private String checkout(String key, Long id, int tendered) {
        return "{\"items\":" + lines() + ",\"bill_id\":" + id + ",\"checkout_key\":\"" + key +
                "\",\"payment_method\":\"CASH\",\"tendered_amount\":" + tendered + "}";
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void heldBillCheckoutAndRetryKeepPortionsAndOnePayment() throws Exception {
        String response = mvc.perform(post("/api/v1/bills/hold").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":" + lines() + ",\"hold_label\":\"Counter 2\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("HELD")))
                .andExpect(jsonPath("$.total_amount", is(220.0))).andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(response).get("id").asLong();
        String key = UUID.randomUUID().toString();
        String body = checkout(key, id, 250);
        String paid = mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key",key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.payment.change_amount", is(30.0)))
                .andExpect(jsonPath("$.items", hasSize(2))).andReturn().getResponse().getContentAsString();
        JsonNode result = mapper.readTree(paid);
        mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key",key)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id", is(id.intValue())))
                .andExpect(jsonPath("$.bill_number", is(result.get("bill_number").asText())));
        mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key",key)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout(key,id,300)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/bills/" + id + "/cancel").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"changed mind\"}")).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void invalidPaymentAndChangedPriceDoNotCreateBills() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key",key)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout(key,null,100)))
                .andExpect(status().isBadRequest());
        MenuItem item = menu.findById(itemId).orElseThrow(); item.setSellingPrice(new BigDecimal("120.00")); menu.saveAndFlush(item);
        mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key",key)
                        .contentType(MediaType.APPLICATION_JSON).content(checkout(key,null,300)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void optionalRoundOffPersistsAndReconcilesPaymentAndRetries() throws Exception {
        for (String[] scenario : new String[][] {
                {"100.49", "true", "CASH", "100.00", "-0.49"},
                {"100.50", "true", "UPI", "101.00", "0.50"},
                {"100.01", "true", "CARD", "100.00", "-0.01"},
                {"100.49", "false", "UPI", "100.49", "0.00"},
                {"100.00", "true", "CASH", "100.00", "0.00"}
        }) {
            MenuItem item = menu.findById(itemId).orElseThrow();
            item.setSellingPrice(new BigDecimal(scenario[0]));
            menu.saveAndFlush(item);
            String key = UUID.randomUUID().toString();
            String body = "{\"items\":[{\"menu_item_id\":" + itemId + ",\"quantity\":1,\"expected_unit_price\":" + scenario[0] +
                    "}],\"checkout_key\":\"" + key + "\",\"payment_method\":\"" + scenario[2] +
                    "\",\"tendered_amount\":" + scenario[3] + ",\"round_off\":" + scenario[1] + "}";
            JsonNode paid = mapper.readTree(mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertEquals(0, paid.get("round_off_amount").decimalValue().compareTo(new BigDecimal(scenario[4])));
            assertEquals(0, paid.get("total_amount").decimalValue().compareTo(new BigDecimal(scenario[3])));
            assertEquals(0, paid.get("payment").get("amount").decimalValue().compareTo(new BigDecimal(scenario[3])));
            assertEquals(0, paid.get("items").get(0).get("line_net_total").decimalValue().compareTo(new BigDecimal(scenario[0])));
            mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key", key)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(paid.get("id").asInt())));
            if (!scenario[4].equals("0.00")) {
                String changed = body.replace("\"round_off\":" + scenario[1], "\"round_off\":" + (scenario[1].equals("true") ? "false" : "true"));
                mvc.perform(post("/api/v1/bills/checkout").with(csrf()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(changed)).andExpect(status().isConflict());
            }
            mvc.perform(get("/api/v1/bills/" + paid.get("id").asLong()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.round_off_amount", is(new BigDecimal(scenario[4]).doubleValue())));
        }
    }

    @Test
    @WithMockUser(username="admin", roles="ADMIN")
    void concurrentIdenticalCheckoutRequestsReturnSameBill() throws Exception {
        String key = UUID.randomUUID().toString(), body = checkout(key, null, 250);
        var executor = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<String> call = () -> mvc.perform(post("/api/v1/bills/checkout").with(csrf())
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var first = executor.submit(call);
            var second = executor.submit(call);
            assertEquals(mapper.readTree(first.get()).get("id"), mapper.readTree(second.get()).get("id"));
        } finally { executor.shutdownNow(); }
    }

    @Test
    @WithMockUser(username="viewer", roles="READ_ONLY")
    void readOnlyCanViewButCannotHold() throws Exception {
        mvc.perform(get("/api/v1/bills/capabilities")).andExpect(status().isOk()).andExpect(jsonPath("$.round_off", is(true)));
        mvc.perform(get("/api/v1/bills/held")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/bills/hold").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":" + lines() + "}")).andExpect(status().isForbidden());
    }
}
