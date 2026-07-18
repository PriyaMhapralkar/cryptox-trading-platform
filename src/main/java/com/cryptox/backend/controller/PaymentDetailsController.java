package com.cryptox.backend.controller;

import com.cryptox.backend.dto.PaymentDetailsRequest;
import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.PaymentDetailsRepository;
import com.cryptox.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment-details")
public class PaymentDetailsController {

    @Autowired private PaymentDetailsRepository paymentDetailsRepository;
    @Autowired private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public PaymentDetails getDetails(Authentication authentication) {
        User user = currentUser(authentication);
        PaymentDetails details = paymentDetailsRepository.findByUserId(user.getId());
        if (details == null) {
            throw new RuntimeException("No payment details found. Please add your bank details.");
        }
        return details;
    }

    @PostMapping
    public PaymentDetails saveDetails(@RequestBody PaymentDetailsRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        PaymentDetails details = paymentDetailsRepository.findByUserId(user.getId());

        if (details == null) {
            details = PaymentDetails.builder().user(user).build();
        }

        details.setAccountNumber(request.getAccountNumber());
        details.setAccountHolderName(request.getAccountHolderName());
        details.setIfsc(request.getIfsc());
        details.setBankName(request.getBankName());

        return paymentDetailsRepository.save(details);
    }
}