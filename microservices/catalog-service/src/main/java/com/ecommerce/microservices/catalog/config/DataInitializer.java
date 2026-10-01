package com.ecommerce.microservices.catalog.config;

import com.ecommerce.microservices.catalog.entity.Product;
import com.ecommerce.microservices.catalog.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> products = List.of(
                    productMap("gsr-18v-55", "BOS-GSR1855", "GSR 18V-55", "Brushless drill driver", "Bosch", "Drills & drivers", 14900, 12, true, "/images/gsr-18v-55.png", "A compact, brushless drill driver for drilling and screwdriving in wood and metal.", specMap("Battery platform", "18 V", "Max. torque", "55 Nm", "No-load speed", "0-460 / 0-1,800 rpm", "Chuck capacity", "1.5-13 mm", "Max. drilling in wood", "35 mm", "Included", "Bare tool; battery and charger sold separately")),
                    productMap("gks-190", "BOS-GKS190", "GKS 190", "190 mm circular saw", "Bosch", "Saws", 17900, 8, true, "/images/gks-190.png", "A corded circular saw for straight cuts in timber and sheet materials.", specMap("Rated power", "1,400 W", "Blade diameter", "190 mm", "No-load speed", "5,500 rpm", "Cutting depth at 90 degrees", "70 mm", "Bevel capacity", "56 degrees", "Included", "Saw blade, parallel guide and hex key")),
                    productMap("gst-90-be", "BOS-GST90BE", "GST 90 BE", "650 W jigsaw", "Bosch", "Saws", 11900, 6, false, "/images/gst-90-be.png", "A top-handle jigsaw for controlled curved and straight cuts.", specMap("Rated power", "650 W", "Stroke rate", "500-3,100 spm", "Stroke length", "26 mm", "Cutting depth in wood", "90 mm", "Cutting depth in steel", "10 mm", "Included", "Jigsaw blade and hex key")),
                    productMap("gws-7-115", "BOS-GWS7115", "GWS 7-115", "115 mm angle grinder", "Bosch", "Grinding & sanding", 6900, 15, true, "/images/gws-7-115.png", "A slim-grip corded angle grinder for metal finishing and light grinding work.", specMap("Rated power", "720 W", "Disc diameter", "115 mm", "No-load speed", "11,000 rpm", "Spindle thread", "M14", "Weight", "1.9 kg", "Included", "Protective guard, auxiliary handle and wrench; disc not included")),
                    productMap("gex-125-1-ae", "BOS-GEX1251", "GEX 125-1 AE", "Random orbit sander", "Bosch", "Grinding & sanding", 9900, 9, true, "/images/gex-125-1-ae.png", "A compact random orbit sander for preparing and finishing wood surfaces.", specMap("Rated power", "250 W", "Sanding pad", "125 mm", "No-load speed", "7,500-12,000 rpm", "Orbit diameter", "2.5 mm", "Weight", "1.3 kg", "Included", "Microfilter dust box and sanding sheet")),
                    productMap("glm-40", "BOS-GLM40", "GLM 40", "40 m laser measure", "Bosch", "Measuring", 7900, 11, false, "/images/glm-40.png", "A pocket-sized laser measure for distance, area and volume calculations.", specMap("Measuring range", "0.15-40 m", "Typical accuracy", "+/- 1.5 mm", "Laser class", "2", "Protection rating", "IP54", "Power supply", "2 x AAA batteries", "Included", "Batteries and protective pouch")),
                    productMap("gbh-2-26", "BOS-GBH226", "GBH 2-26", "SDS plus rotary hammer", "Bosch", "Drills & drivers", 19900, 4, false, "/images/gbh-2-26.png", "A corded rotary hammer for drilling concrete, masonry, wood and metal.", specMap("Rated power", "830 W", "Impact energy", "2.7 J", "Tool holder", "SDS plus", "Max. drilling in concrete", "26 mm", "Weight", "2.7 kg", "Included", "Auxiliary handle, depth stop and carrying case")),
                    productMap("gop-30-28", "BOS-GOP3028", "GOP 30-28", "Oscillating multi-tool", "Bosch", "Multi-tools", 13900, 0, false, "/images/gop-30-28.png", "A versatile corded multi-tool for plunge cuts, trimming and detail sanding.", specMap("Rated power", "300 W", "No-load oscillation", "8,000-20,000 opm", "Oscillation angle", "1.4 degrees each side", "Accessory interface", "Starlock", "Weight", "1.5 kg", "Included", "Plunge-cut blade and hex key"))
                );

                for (Map<String, Object> productMap : products) {
                    Product product = new Product();
                    product.setId((String) productMap.get("id"));
                    product.setSku((String) productMap.get("sku"));
                    product.setName((String) productMap.get("name"));
                    product.setSubtitle((String) productMap.get("subtitle"));
                    product.setBrand((String) productMap.get("brand"));
                    product.setFeatured(Boolean.TRUE.equals(productMap.get("featured")));
                    product.setCategory((String) productMap.get("category"));
                    product.setDescription((String) productMap.get("description"));
                    product.setPrice(BigDecimal.valueOf(((Number) productMap.get("price")).longValue()));
                    product.setStockQuantity(((Number) productMap.get("stock")).intValue());
                    product.setImageUrl((String) productMap.get("image"));
                    product.setSellerId(2L);
                    product.setActive(true);
                    product.setSpecs(mapper.writeValueAsString(productMap.get("specs")));
                    productRepository.save(product);
                }
            }
        };
    }

    private Map<String, Object> productMap(String id, String sku, String name, String subtitle, String brand, String category, int price, int stock, boolean featured, String image, String description, Map<String, String> specs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("sku", sku);
        map.put("name", name);
        map.put("subtitle", subtitle);
        map.put("brand", brand);
        map.put("category", category);
        map.put("price", price);
        map.put("stock", stock);
        map.put("featured", featured);
        map.put("image", image);
        map.put("description", description);
        map.put("specs", specs);
        return map;
    }

    private Map<String, String> specMap(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
