package com.ecommerce.microservices.catalog.controller;

import com.ecommerce.microservices.common.dto.ChatbotRequest;
import com.ecommerce.microservices.common.dto.ChatbotResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatbotController {

    private static final Map<String, String> RULES = new LinkedHashMap<>();

    static {
        RULES.put("RECOMMEND", "Based on your workshop needs: For general woodworking and fastening, try the GSR 18V-55 Brushless Drill Driver ($149). For straight cuts, check the GKS 190 Circular Saw ($179). For sanding, check the GEX 125-1 AE ($99). Tell me what work you have planned!");
        RULES.put("DRILL", "For drilling and fastening, check the GSR 18V-55 Brushless drill driver ($149.00, 18V, 55 Nm) or the GBH 2-26 SDS plus rotary hammer ($199.00, 2.7 J) for concrete drilling.");
        RULES.put("SAW", "For timber and sheet cutting, we offer the GKS 190 circular saw ($179.00, 70 mm cut depth) and the GST 90 BE 650W jigsaw ($119.00, tool-free blade change).");
        RULES.put("SANDER", "For surface prep and finishing, check the GEX 125-1 AE random orbit sander ($99.00) with dust collection, and the GWS 7-115 115mm angle grinder ($69.00).");
        RULES.put("MEASURE", "For distance, area, and volume calculations, check the GLM 40 Laser Measure ($79.00) with 40-meter range and IP54 dust/splash protection.");
        RULES.put("SHIPPING", "Standard delivery is $8.50 (3-5 business days) and is FREE on orders of $150 or more! Express delivery is $18.00 (1-2 business days).");
        RULES.put("CHECKOUT", "Go to your Cart and click 'Continue to checkout'. Enter fictional demo delivery details and proceed to review mock payment.");
        RULES.put("CART", "Open any product, select your desired quantity, and click 'Add to cart'. You can view and edit quantities anytime in your Cart.");
        RULES.put("ORDERS", "Open 'Order history' from the top navigation to view all demo orders placed in this local session.");
        RULES.put("SEARCH", "Use the search bar at the top of the 'Browse tools' page or choose a category filter to find any tool by model, brand, or specification.");
        RULES.put("TAX", "Estimated tax is 8% of the merchandise subtotal, calculated automatically at checkout.");
        RULES.put("PAYMENT", "Mock payments only! Choose 'Approve payment' to create a confirmed demo order or 'Decline payment' to test card decline simulation. No real card details are ever taken.");
        RULES.put("ROLES", "Demo roles available: Buyer (buyer / Buyer@123), Seller (seller / Seller@123), and Admin (admin / Admin@123). Click 'Profile' in the header to switch roles instantly.");
        RULES.put("GREETING", "Hello! I'm your Workbench Smart Assistant. How can I assist you with your workshop tools today? You can ask about tool recommendations, search, delivery rates, checkout, or demo user roles.");
    }

    @PostMapping("/chatbot/message")
    public ResponseEntity<ChatbotResponse> message(@Valid @RequestBody ChatbotRequest request) {
        String msg = request.getMessage() != null ? request.getMessage().trim().toLowerCase() : "";
        for (Map.Entry<String, String> entry : RULES.entrySet()) {
            if (matches(msg, entry.getKey())) {
                return ResponseEntity.ok(new ChatbotResponse(entry.getValue(), entry.getKey()));
            }
        }
        return ResponseEntity.ok(new ChatbotResponse(
                "I can help you find tools (drills, saws, sanders), answer questions about shipping and checkout, or assist with switching demo accounts. How can I assist you?", "FALLBACK"));
    }

    private boolean matches(String text, String ruleName) {
        return switch (ruleName) {
            case "RECOMMEND" -> text.contains("recommend") || text.contains("suggest") || text.contains("best tool");
            case "DRILL" -> text.contains("drill") || text.contains("screwdriver") || text.contains("gsr") || text.contains("gbh");
            case "SAW" -> text.contains("saw") || text.contains("cut") || text.contains("jigsaw") || text.contains("gks") || text.contains("gst");
            case "SANDER" -> text.contains("sand") || text.contains("grind") || text.contains("gex") || text.contains("gws");
            case "MEASURE" -> text.contains("measure") || text.contains("laser") || text.contains("glm");
            case "SEARCH" -> text.contains("search") || text.contains("find") || text.contains("browse");
            case "CART" -> text.contains("cart") || text.contains("basket");
            case "CHECKOUT" -> text.contains("checkout") || text.contains("buy");
            case "ORDERS" -> text.contains("order") || text.contains("history");
            case "SHIPPING" -> text.contains("shipping") || text.contains("delivery");
            case "TAX" -> text.contains("tax");
            case "PAYMENT" -> text.contains("payment") || text.contains("card");
            case "ROLES" -> text.contains("role") || text.contains("login") || text.contains("user");
            case "GREETING" -> text.contains("hello") || text.contains("hi") || text.contains("help") || text.contains("assist");
            default -> false;
        };
    }
}
