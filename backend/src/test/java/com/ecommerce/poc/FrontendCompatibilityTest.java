package com.ecommerce.poc;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FrontendCompatibilityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void configAndSessionEndpointsAreCompatibleWithTheFrontend() throws Exception {
        mockMvc.perform(get("/api/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.productCount").exists())
            .andExpect(jsonPath("$.categories[0].name").exists());

        MvcResult sessionResult = mockMvc.perform(post("/api/sessions"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").exists())
            .andReturn();

        String sessionId = JsonPath.read(sessionResult.getResponse().getContentAsString(), "$.sessionId");

        mockMvc.perform(get("/api/products?featured=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items[0].brand").value("Bosch"))
            .andExpect(jsonPath("$.items[0].subtitle").isNotEmpty());

        mockMvc.perform(get("/api/cart").header("X-Session-Id", sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemCount").value(0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/cart/items/gsr-18v-55")
                .header("X-Session-Id", sessionId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"quantity\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemCount").value(2));
    }
}
