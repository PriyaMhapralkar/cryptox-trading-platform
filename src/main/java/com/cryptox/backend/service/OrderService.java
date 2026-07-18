package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private AssetRepository assetRepository;
    @Autowired private CoinRepository coinRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private TradingHistoryRepository tradingHistoryRepository;
    @Autowired private WalletService walletService;

    @Transactional
    public Order processOrder(Long userId, String coinId, String orderTypeStr, BigDecimal usdAmount) {
        if (usdAmount == null || usdAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        Coin coin = coinRepository.findByCoinId(coinId)
                .orElseThrow(() -> new RuntimeException("Coin not found: " + coinId));

        OrderType orderType = OrderType.valueOf(orderTypeStr.toUpperCase());
        Wallet wallet = walletRepository.findByUserId(userId);
        if (wallet == null) throw new RuntimeException("Wallet not found");

        BigDecimal currentPrice = BigDecimal.valueOf(coin.getCurrentPrice());
        BigDecimal quantity = usdAmount.divide(currentPrice, 8, RoundingMode.HALF_UP);

        User user = new User();
        user.setId(userId); // lightweight reference, avoids an extra fetch

        Order order;

        if (orderType == OrderType.BUY) {
            order = processBuy(user, wallet, coin, usdAmount, quantity, currentPrice);
        } else {
            order = processSell(user, wallet, coin, usdAmount, quantity, currentPrice);
        }

        return order;
    }

    private Order processBuy(User user, Wallet wallet, Coin coin, BigDecimal usdAmount,
                              BigDecimal quantity, BigDecimal currentPrice) {

        // 1. Debit wallet (throws if insufficient balance — this rolls back everything below)
        walletService.debitForBuy(wallet, usdAmount);

        // 2. Update or create the user's Asset holding for this coin
        Asset asset = assetRepository.findByUserIdAndCoinId(user.getId(), coin.getId())
                .orElseGet(() -> Asset.builder()
                        .user(user)
                        .coin(coin)
                        .quantity(BigDecimal.ZERO)
                        .buyPrice(currentPrice)
                        .build());

        // Weighted average buy price across all purchases of this coin
        BigDecimal existingValue = asset.getQuantity().multiply(asset.getBuyPrice());
        BigDecimal newValue = existingValue.add(usdAmount);
        BigDecimal newQuantity = asset.getQuantity().add(quantity);
        BigDecimal newAvgBuyPrice = newQuantity.compareTo(BigDecimal.ZERO) > 0
                ? newValue.divide(newQuantity, 8, RoundingMode.HALF_UP)
                : currentPrice;

        asset.setQuantity(newQuantity);
        asset.setBuyPrice(newAvgBuyPrice);
        assetRepository.save(asset);

        // 3. Create OrderItem + Order
        OrderItem orderItem = OrderItem.builder()
                .quantity(quantity)
                .coin(coin)
                .buyPrice(currentPrice)
                .build();
        orderItemRepository.save(orderItem);

        Order order = Order.builder()
                .user(user)
                .orderType(OrderType.BUY)
                .price(usdAmount)
                .timestamp(LocalDateTime.now())
                .status(OrderStatus.SUCCESS)
                .orderItem(orderItem)
                .build();
        orderRepository.save(order);

        // 4. Log trading history (profit/loss is N/A for a buy, so null/zero)
        tradingHistoryRepository.save(TradingHistory.builder()
                .buyingPrice(currentPrice)
                .sellingPrice(null)
                .quantity(quantity)
                .coin(coin)
                .user(user)
                .timestamp(LocalDateTime.now())
                .orderType(OrderType.BUY)
                .profitLoss(BigDecimal.ZERO)
                .build());

        return order;
    }

    private Order processSell(User user, Wallet wallet, Coin coin, BigDecimal usdAmount,
                               BigDecimal quantity, BigDecimal currentPrice) {

        Asset asset = assetRepository.findByUserIdAndCoinId(user.getId(), coin.getId())
                .orElseThrow(() -> new RuntimeException("You don't own any " + coin.getSymbol().toUpperCase()));

        if (asset.getQuantity().compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient " + coin.getSymbol().toUpperCase() + " holdings to sell this amount");
        }

        // 1. Reduce asset quantity (remove asset row entirely if fully sold)
        BigDecimal remainingQty = asset.getQuantity().subtract(quantity);
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            assetRepository.delete(asset);
        } else {
            asset.setQuantity(remainingQty);
            assetRepository.save(asset);
        }

        // 2. Credit wallet with sale proceeds
        walletService.creditForSell(wallet, usdAmount);

        // 3. Create OrderItem + Order
        OrderItem orderItem = OrderItem.builder()
                .quantity(quantity)
                .coin(coin)
                .sellPrice(currentPrice)
                .build();
        orderItemRepository.save(orderItem);

        Order order = Order.builder()
                .user(user)
                .orderType(OrderType.SELL)
                .price(usdAmount)
                .timestamp(LocalDateTime.now())
                .status(OrderStatus.SUCCESS)
                .orderItem(orderItem)
                .build();
        orderRepository.save(order);

        // 4. Log trading history with profit/loss calculated against original avg buy price
        BigDecimal profitLoss = (currentPrice.subtract(asset.getBuyPrice())).multiply(quantity);

        tradingHistoryRepository.save(TradingHistory.builder()
                .buyingPrice(asset.getBuyPrice())
                .sellingPrice(currentPrice)
                .quantity(quantity)
                .coin(coin)
                .user(user)
                .timestamp(LocalDateTime.now())
                .orderType(OrderType.SELL)
                .profitLoss(profitLoss)
                .build());

        return order;
    }
}