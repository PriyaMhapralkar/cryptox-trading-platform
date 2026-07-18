package com.cryptox.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddBalanceRequest {
    private BigDecimal amount;
}