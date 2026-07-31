package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("trader@example.com").build();
        wallet = Wallet.builder().id(10L).user(user).balance(BigDecimal.valueOf(1000)).build();
    }

    @Test
    void addBalance_shouldIncreaseWalletBalance() {
        when(walletRepository.findByUserId(1L)).thenReturn(wallet);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.addBalance(1L, BigDecimal.valueOf(500));

        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void addBalance_shouldRejectZeroOrNegativeAmount() {
        assertThatThrownBy(() -> walletService.addBalance(1L, BigDecimal.ZERO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> walletService.addBalance(1L, BigDecimal.valueOf(-50)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("greater than zero");

        verifyNoInteractions(walletRepository);
    }

    @Test
    void transfer_shouldMoveFundsBetweenTwoWallets() {
        User recipient = User.builder().id(2L).email("recipient@example.com").build();
        Wallet recipientWallet = Wallet.builder().id(11L).user(recipient).balance(BigDecimal.valueOf(200)).build();

        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUserId(1L)).thenReturn(wallet);
        when(walletRepository.findByUserId(2L)).thenReturn(recipientWallet);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.transfer(1L, "recipient@example.com", BigDecimal.valueOf(300), "test transfer");

        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
        assertThat(recipientWallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(500));

        // Two transaction rows should be recorded: one TRANSFER_OUT, one TRANSFER_IN
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));
    }

    @Test
    void transfer_shouldRejectInsufficientBalance() {
        User recipient = User.builder().id(2L).email("recipient@example.com").build();
        Wallet recipientWallet = Wallet.builder().id(11L).user(recipient).balance(BigDecimal.valueOf(200)).build();

        when(userRepository.findByEmail("recipient@example.com")).thenReturn(Optional.of(recipient));
        when(walletRepository.findByUserId(1L)).thenReturn(wallet);
        when(walletRepository.findByUserId(2L)).thenReturn(recipientWallet);

        assertThatThrownBy(() ->
                walletService.transfer(1L, "recipient@example.com", BigDecimal.valueOf(5000), "too much")
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Insufficient wallet balance");

        // Balances must remain unchanged since the transfer should have been rejected before mutating anything
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void transfer_shouldRejectSelfTransfer() {
        when(userRepository.findByEmail("trader@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                walletService.transfer(1L, "trader@example.com", BigDecimal.valueOf(100), "self transfer")
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Cannot transfer to yourself");
    }

    @Test
    void debitForBuy_shouldRejectInsufficientBalance() {
        assertThatThrownBy(() ->
                walletService.debitForBuy(wallet, BigDecimal.valueOf(5000))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Insufficient balance");
    }
}