package com.cryptox.backend.controller;

import com.cryptox.backend.dto.AdminStatsResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private WithdrawalRepository withdrawalRepository;
    @Autowired private WalletTransactionRepository walletTransactionRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @GetMapping("/stats")
    public AdminStatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalTransactions = orderRepository.count();
        BigDecimal totalBalance = walletRepository.findAll().stream()
                .map(Wallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pending = withdrawalRepository.findByStatus(WithdrawalStatus.PENDING).size();

        return new AdminStatsResponse(totalUsers, totalTransactions, totalBalance, pending);
    }
    
    @GetMapping("/stats/transactions-timeline")
    public List<Map<String, Object>> getTransactionsTimeline() {
        List<Order> orders = orderRepository.findAll();

        Map<String, Long> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getTimestamp().toLocalDate().toString(),
                        Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("date", e.getKey());
                    point.put("count", e.getValue());
                    return point;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PutMapping("/users/{id}/block")
    public User blockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus("BLOCKED");
        return userRepository.save(user);
    }

    @PutMapping("/users/{id}/unblock")
    public User unblockUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    @GetMapping("/transactions")
    public List<WalletTransaction> getAllTransactions() {
        return walletTransactionRepository.findAll();
    }

    @GetMapping("/wallets")
    public List<Wallet> getAllWallets() {
        return walletRepository.findAll();
    }

    @GetMapping("/logs")
    public Page<AuditLog> getLogs(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }
}