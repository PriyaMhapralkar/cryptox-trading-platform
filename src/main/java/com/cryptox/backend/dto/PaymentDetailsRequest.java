package com.cryptox.backend.dto;

import lombok.Data;

@Data
public class PaymentDetailsRequest {
    private String accountNumber;
    private String accountHolderName;
    private String ifsc;
    private String bankName;
}