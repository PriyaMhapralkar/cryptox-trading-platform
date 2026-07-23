package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WithdrawalService {

    @Autowired private WithdrawalRepository withdrawalRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private PaymentDetailsRepository paymentDetailsRepository;
    @Autowired private AuditLogService auditLogService; 

    @Transactional
    public Withdrawal requestWithdrawal(User user, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        PaymentDetails details = paymentDetailsRepository.findByUserId(user.getId());
        if (details == null) {
            throw new RuntimeException("Please add your bank details before requesting a withdrawal");
        }

        Wallet wallet = walletRepository.findByUserId(user.getId());
        if (wallet == null) throw new RuntimeException("Wallet not found");

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance for this withdrawal");
        }

        // Debit immediately — funds are held pending admin approval
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.WITHDRAWAL)
                .date(LocalDateTime.now())
                .transferId(UUID.randomUUID().toString())
                .purpose("Withdrawal request (pending approval)")
                .amount(amount)
                .build());

        Withdrawal withdrawal = Withdrawal.builder()
                .status(WithdrawalStatus.PENDING)
                .amount(amount)
                .user(user)
                .date(LocalDateTime.now())
                .build();
        
        auditLogService.log(user, "WITHDRAWAL_REQUEST", "SUCCESS",
                "Requested withdrawal of $" + amount); 

        return withdrawalRepository.save(withdrawal);
    }

    public List<Withdrawal> getUserWithdrawals(Long userId) {
        return withdrawalRepository.findByUserIdOrderByDateDesc(userId);
    }

    // ----- Admin methods -----

    public List<Withdrawal> getAllWithdrawals() {
        return withdrawalRepository.findAll();
    }

    @Transactional
    public Withdrawal processWithdrawal(Long withdrawalId, boolean approve) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Withdrawal not found"));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new RuntimeException("This withdrawal has already been processed");
        }

        if (approve) {
            withdrawal.setStatus(WithdrawalStatus.SUCCESS);
            // Money was already debited at request time — in a real system,
            // this is where you'd trigger the actual bank transfer via a payout API.
        } else {
            withdrawal.setStatus(WithdrawalStatus.DECLINED);

            // Refund the wallet since the withdrawal was declined
            Wallet wallet = walletRepository.findByUserId(withdrawal.getUser().getId());
            wallet.setBalance(wallet.getBalance().add(withdrawal.getAmount()));
            walletRepository.save(wallet);

            walletTransactionRepository.save(WalletTransaction.builder()
                    .wallet(wallet)
                    .type(TransactionType.DEPOSIT)
                    .date(LocalDateTime.now())
                    .transferId(UUID.randomUUID().toString())
                    .purpose("Withdrawal declined — refund")
                    .amount(withdrawal.getAmount())
                    .build());
            
            auditLogService.log(withdrawal.getUser(), "WITHDRAWAL_DECLINED", "SUCCESS",
                    "Withdrawal #" + withdrawal.getId() + " declined by admin, refunded"); 
        }

        return withdrawalRepository.save(withdrawal);
        
    }
}