package com.ecommerce.poc;

import com.ecommerce.poc.dto.ProductResponse;
import com.ecommerce.poc.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CatalogServiceTest {
    @Autowired
    private CatalogService catalogService;

    @Test
    void publicCatalogSearchAndListingWork() {
        var page = catalogService.search("drill", "Drills & drivers", null, null, "featured", 0, 20);
        assertFalse(page.getContent().isEmpty());
        assertTrue(page.getContent().stream().anyMatch(p -> p.getName().contains("GSR")));

        ProductResponse product = catalogService.getById("gsr-18v-55");
        assertNotNull(product);
        assertEquals("gsr-18v-55", product.getId());
    }
}
