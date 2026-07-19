package com.cryptox.backend.dto;

import lombok.Data;

@Data
public class StripeConfirmRequest {
    private String paymentIntentId;
}