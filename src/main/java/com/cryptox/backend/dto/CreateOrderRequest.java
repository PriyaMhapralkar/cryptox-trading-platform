package com.cryptox.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    private BigDecimal amount; // in USD (or INR for Razorpay, see note below)
}