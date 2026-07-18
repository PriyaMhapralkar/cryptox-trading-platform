package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WalletService {

    @Autowired private WalletRepository walletRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private UserRepository userRepository;

    public Wallet getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId);
        if (wallet == null) throw new RuntimeException("Wallet not found for user");
        return wallet;
    }

    @Transactional
    public Wallet addBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction txn = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEPOSIT)
                .date(LocalDateTime.now())
                .transferId(UUID.randomUUID().toString())
                .purpose("Wallet top-up")
                .amount(amount)
                .build();
        walletTransactionRepository.save(txn);

        return wallet;
    }

    @Transactional
    public void transfer(Long fromUserId, String toUserEmail, BigDecimal amount, String purpose) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        User toUser = userRepository.findByEmail(toUserEmail)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        if (toUser.getId().equals(fromUserId)) {
            throw new RuntimeException("Cannot transfer to yourself");
        }

        Wallet fromWallet = getWalletByUserId(fromUserId);
        Wallet toWallet = getWalletByUserId(toUser.getId());

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        String transferId = UUID.randomUUID().toString();

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(fromWallet)
                .type(TransactionType.TRANSFER_OUT)
                .date(LocalDateTime.now())
                .transferId(transferId)
                .purpose(purpose)
                .amount(amount)
                .build());

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(toWallet)
                .type(TransactionType.TRANSFER_IN)
                .date(LocalDateTime.now())
                .transferId(transferId)
                .purpose(purpose)
                .amount(amount)
                .build());
    }

    // Used internally by OrderService — debits/credits wallet during buy/sell
    @Transactional
    public void debitForBuy(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance to complete this purchase");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.BUY)
                .date(LocalDateTime.now())
                .transferId(UUID.randomUUID().toString())
                .purpose("Crypto purchase")
                .amount(amount)
                .build());
    }

    @Transactional
    public void creditForSell(Wallet wallet, BigDecimal amount) {
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.SELL)
                .date(LocalDateTime.now())
                .transferId(UUID.randomUUID().toString())
                .purpose("Crypto sale")
                .amount(amount)
                .build());
    }

    public java.util.List<WalletTransaction> getTransactionHistory(Long walletId) {
        return walletTransactionRepository.findByWalletIdOrderByDateDesc(walletId);
    }
}