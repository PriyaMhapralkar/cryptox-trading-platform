package com.cryptox.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private String toUserEmail;
    private BigDecimal amount;
    private String purpose;
}