package com.cryptox.backend.controller;

import com.cryptox.backend.dto.*;
import com.cryptox.backend.entity.User;
import com.cryptox.backend.repository.UserRepository;
import com.cryptox.backend.service.PaymentService;
import com.stripe.model.PaymentIntent;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired private PaymentService paymentService;
    @Autowired private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/razorpay/create-order")
    public String createRazorpayOrder(@RequestBody CreateOrderRequest request, Authentication authentication) throws Exception {
        User user = currentUser(authentication);
        JSONObject order = paymentService.createRazorpayOrder(user, request.getAmount());
        return order.toString();
    }

    @PostMapping("/razorpay/verify")
    public String verifyRazorpay(@RequestBody RazorpayVerifyRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        return paymentService.verifyRazorpayPayment(user,
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());
    }

    @PostMapping("/stripe/create-order")
    public String createStripeIntent(@RequestBody CreateOrderRequest request, Authentication authentication) throws Exception {
        User user = currentUser(authentication);
        PaymentIntent intent = paymentService.createStripePaymentIntent(user, request.getAmount());
        // Return the client secret — the frontend needs this to confirm payment via Stripe.js
        return "{\"clientSecret\":\"" + intent.getClientSecret() + "\",\"paymentIntentId\":\"" + intent.getId() + "\"}";
    }

    @PostMapping("/stripe/confirm")
    public String confirmStripe(@RequestBody StripeConfirmRequest request, Authentication authentication) throws Exception {
        User user = currentUser(authentication);
        return paymentService.confirmStripePayment(user, request.getPaymentIntentId());
    }
}