package com.cryptox.backend.controller;

import com.cryptox.backend.dto.OrderRequest;
import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.OrderRepository;
import com.cryptox.backend.repository.UserRepository;
import com.cryptox.backend.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/pay")
    public Order pay(@RequestBody OrderRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        return orderService.processOrder(user.getId(), request.getCoinId(), request.getOrderType(), request.getAmount());
    }

    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    @GetMapping("/user")
    public List<Order> getUserOrders(Authentication authentication) {
        User user = currentUser(authentication);
        return orderRepository.findByUserIdOrderByTimestampDesc(user.getId());
    }
}