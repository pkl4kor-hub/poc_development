package com.ecommerce.poc.service;

import com.ecommerce.poc.dto.CartResponse;
import com.ecommerce.poc.dto.ProductResponse;
import com.ecommerce.poc.entity.Cart;
import com.ecommerce.poc.entity.CartItem;
import com.ecommerce.poc.entity.Product;
import com.ecommerce.poc.entity.User;
import com.ecommerce.poc.exception.ResourceNotFoundException;
import com.ecommerce.poc.exception.ValidationException;
import com.ecommerce.poc.repository.CartItemRepository;
import com.ecommerce.poc.repository.CartRepository;
import com.ecommerce.poc.repository.ProductRepository;
import com.ecommerce.poc.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CartService {
    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("15000");
    private static final BigDecimal STANDARD_SHIPPING = new BigDecimal("850");
    private static final BigDecimal EXPRESS_SHIPPING = new BigDecimal("1800");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository,
                      UserRepository userRepository, SessionService sessionService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    public CartResponse getCart(String shippingMethod) {
        User buyer = currentUser();
        Cart cart = cartRepository.findByBuyerIdAndStatus(buyer.getId(), "ACTIVE")
                .orElseGet(() -> createCart(buyer));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartResponse.CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : items) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "This tool could not be found."));
            ProductResponse productResponse = toProductResponse(product);
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            CartResponse.CartItemResponse responseItem = new CartResponse.CartItemResponse();
            responseItem.setProduct(productResponse);
            responseItem.setQuantity(item.getQuantity());
            responseItem.setLineTotal(lineTotal);
            responseItem.setAvailable(item.getQuantity() <= product.getStockQuantity());
            itemResponses.add(responseItem);
        }
        BigDecimal shipping = resolveShipping(subtotal, shippingMethod);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(0, java.math.RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shipping).add(tax);
        CartResponse response = new CartResponse();
        response.setItems(itemResponses);
        response.setItemCount(items.stream().mapToInt(CartItem::getQuantity).sum());
        response.setSubtotal(subtotal);
        response.setShipping(shipping);
        response.setTax(tax);
        response.setTotal(total);
        response.setCurrency("USD");
        response.setShippingMethod(shippingMethod);
        response.setCanCheckout(!items.isEmpty() && itemResponses.stream().allMatch(CartResponse.CartItemResponse::isAvailable));
        return response;
    }

    @Transactional
    public CartResponse setCartItem(String productId, int quantity) {
        User buyer = currentUser();
        Cart cart = cartRepository.findByBuyerIdAndStatus(buyer.getId(), "ACTIVE")
                .orElseGet(() -> createCart(buyer));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "This tool could not be found."));
        if (quantity < 0 || quantity > 99) {
            throw new ValidationException(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "Quantity must be a whole number between 0 and 99.");
        }
        if (quantity > product.getStockQuantity()) {
            throw new ValidationException(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", "Only " + product.getStockQuantity() + " of " + product.getName() + " available.");
        }
        if (quantity == 0) {
            cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).ifPresent(cartItemRepository::delete);
            return getCart("standard");
        }
        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseGet(() -> {
                    CartItem item = new CartItem();
                    item.setCart(cart);
                    item.setProductId(productId);
                    item.setUnitPrice(product.getPrice());
                    return item;
                });
        cartItem.setQuantity(quantity);
        cartItem.setUnitPrice(product.getPrice());
        cartItemRepository.save(cartItem);
        return getCart("standard");
    }

    private BigDecimal resolveShipping(BigDecimal subtotal, String shippingMethod) {
        if (shippingMethod == null || shippingMethod.isBlank()) shippingMethod = "standard";
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 && "standard".equalsIgnoreCase(shippingMethod)) {
            return BigDecimal.ZERO;
        }
        if ("express".equalsIgnoreCase(shippingMethod)) {
            return EXPRESS_SHIPPING;
        }
        return STANDARD_SHIPPING;
    }

    private Cart createCart(User buyer) {
        Cart cart = new Cart();
        cart.setBuyerId(buyer.getId());
        cart.setStatus("ACTIVE");
        return cartRepository.save(cart);
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            username = auth.getName();
        }
        if (username == null) {
            String sessionId = null;
            if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes attrs) {
                sessionId = attrs.getRequest().getHeader("X-Session-Id");
            }
            if (sessionId != null && !sessionId.isBlank()) {
                username = sessionService.resolveUsername(sessionId);
            }
        }
        if (username == null) {
            throw new ValidationException(HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED", "Create a local shopping session first.");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException(HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED", "Create a local shopping session first."));
    }

    private ProductResponse toProductResponse(Product product) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(product.getId());
        productResponse.setSku(product.getSku());
        productResponse.setName(product.getName());
        productResponse.setSubtitle(product.getSubtitle() != null ? product.getSubtitle() : "");
        productResponse.setBrand(product.getBrand() != null ? product.getBrand() : "Bosch");
        productResponse.setCategory(product.getCategory());
        productResponse.setPrice(product.getPrice());
        productResponse.setStock(product.getStockQuantity());
        productResponse.setFeatured(product.isFeatured());
        productResponse.setImage(product.getImageUrl());
        productResponse.setDescription(product.getDescription());
        productResponse.setSpecs(Map.of());
        return productResponse;
    }
}
