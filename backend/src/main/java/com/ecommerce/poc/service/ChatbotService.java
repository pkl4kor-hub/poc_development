package com.ecommerce.poc.service;

import com.ecommerce.poc.dto.ChatbotResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ChatbotService {
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
        RULES.put("SELLER_LIST", "Sellers can manage product catalog listings and adjust inventory levels from their seller dashboard.");
        RULES.put("SELLER_STOCK", "Stock quantities update automatically when demo orders are placed. Sellers can monitor inventory levels.");
        RULES.put("ADMIN", "Admins have full platform oversight, user management, and catalog monitoring privileges.");
        RULES.put("GREETING", "Hello! I'm your Workbench Smart Assistant. How can I assist you with your workshop tools today? You can ask about tool recommendations, search, delivery rates, checkout, or demo user roles.");
    }

    public ChatbotResponse answer(String message) {
        if (message == null || message.isBlank()) {
            return new ChatbotResponse("Hello! How can I assist you with tools, shopping, or your demo account today?", "GREETING");
        }

        String normalized = normalize(message);
        if (normalized.isEmpty()) {
            return new ChatbotResponse("Hello! How can I assist you with tools, shopping, or your demo account today?", "GREETING");
        }

        for (Map.Entry<String, String> entry : RULES.entrySet()) {
            String ruleName = entry.getKey();
            String response = entry.getValue();
            if (matches(normalized, ruleName)) {
                return new ChatbotResponse(response, ruleName);
            }
        }

        return new ChatbotResponse("I can help you find tools (drills, saws, sanders), answer questions about shipping and checkout, or assist with switching demo accounts. How can I assist you?", "FALLBACK");
    }

    private String normalize(String input) {
        return input.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private boolean matches(String text, String ruleName) {
        return switch (ruleName) {
            case "RECOMMEND" -> text.contains("recommend") || text.contains("suggest") || text.contains("what should") || text.contains("best tool") || text.contains("what to buy");
            case "DRILL" -> text.contains("drill") || text.contains("screwdriver") || text.contains("fasten") || text.contains("rotary") || text.contains("hammer drill") || text.contains("gsr") || text.contains("gbh");
            case "SAW" -> text.contains("saw") || text.contains("cut") || text.contains("cutting") || text.contains("jigsaw") || text.contains("circular") || text.contains("gks") || text.contains("gst");
            case "SANDER" -> text.contains("sand") || text.contains("sander") || text.contains("grind") || text.contains("grinder") || text.contains("gex") || text.contains("gws");
            case "MEASURE" -> text.contains("measure") || text.contains("laser") || text.contains("distance") || text.contains("glm");
            case "SEARCH" -> text.contains("search") || text.contains("find") || text.contains("browse") || text.contains("catalog");
            case "CART" -> text.contains("cart") || text.contains("add to cart") || text.contains("basket");
            case "CHECKOUT" -> text.contains("checkout") || text.contains("check out") || text.contains("buy");
            case "ORDERS" -> text.contains("order") || text.contains("orders") || text.contains("history") || text.contains("track");
            case "SHIPPING" -> text.contains("shipping") || text.contains("delivery") || text.contains("cost") || text.contains("rate") || text.contains("dispatch");
            case "TAX" -> text.contains("tax") || text.contains("vat");
            case "PAYMENT" -> text.contains("payment") || text.contains("card") || text.contains("pay") || text.contains("mock");
            case "ROLES" -> text.contains("role") || text.contains("login") || text.contains("sign in") || text.contains("account") || text.contains("credential") || text.contains("buyer") || text.contains("seller") || text.contains("admin");
            case "SELLER_LIST" -> text.contains("seller") && (text.contains("list") || text.contains("listing") || text.contains("product"));
            case "SELLER_STOCK" -> text.contains("stock") && text.contains("seller");
            case "ADMIN" -> text.contains("admin") || text.contains("manage users") || text.contains("manage products");
            case "GREETING" -> text.contains("hello") || text.contains("hi") || text.contains("hey") || text.startsWith("how can") || (text.contains("assist") && !text.contains("how much") && !text.contains("how does")) || text.contains("who are you");
            default -> false;
        };
    }
}
