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

import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private CoinRepository coinRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TradingHistoryRepository tradingHistoryRepository;
    @Mock private WalletService walletService;

    @InjectMocks
    private OrderService orderService;

    private Coin bitcoin;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        bitcoin = Coin.builder()
                .id(1L)
                .coinId("bitcoin")
                .symbol("btc")
                .currentPrice(50000.0)
                .build();

        wallet = Wallet.builder().id(1L).balance(BigDecimal.valueOf(10000)).build();

        lenient().when(coinRepository.findByCoinId("bitcoin")).thenReturn(Optional.of(bitcoin));
        lenient().when(walletRepository.findByUserId(anyLong())).thenReturn(wallet);
        lenient().when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tradingHistoryRepository.save(any(TradingHistory.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void buyOrder_shouldCalculateQuantityCorrectlyAndCreateNewAsset() {
        when(assetRepository.findByUserIdAndCoinId(anyLong(), eq(1L))).thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.processOrder(1L, "bitcoin", "BUY", BigDecimal.valueOf(1000));

        assertThat(result.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        // $1000 / $50000 per BTC = 0.02 BTC
        assertThat(result.getOrderItem().getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(0.02));

        verify(walletService).debitForBuy(wallet, BigDecimal.valueOf(1000));
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void buyOrder_shouldAverageCostBasisWithExistingHolding() {
        Asset existing = Asset.builder()
                .user(User.builder().id(1L).build())
                .coin(bitcoin)
                .quantity(BigDecimal.valueOf(0.01)) // already owns 0.01 BTC
                .buyPrice(BigDecimal.valueOf(40000)) // bought at $40k avg
                .build();

        when(assetRepository.findByUserIdAndCoinId(anyLong(), eq(1L))).thenReturn(Optional.of(existing));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.processOrder(1L, "bitcoin", "BUY", BigDecimal.valueOf(1000)); // buys 0.02 more at $50k

        // Existing value: 0.01 * 40000 = 400
        // New purchase:   1000
        // Total value:    1400, total quantity: 0.03
        // New avg price:  1400 / 0.03 = 46666.67 (rounded)
        assertThat(existing.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(0.03));
        assertThat(existing.getBuyPrice()).isCloseTo(BigDecimal.valueOf(46666.67), within(BigDecimal.valueOf(0.01)));
    }

    @Test
    void sellOrder_shouldRejectSellingMoreThanOwned() {
        Asset existing = Asset.builder()
                .user(User.builder().id(1L).build())
                .coin(bitcoin)
                .quantity(BigDecimal.valueOf(0.01)) // owns only 0.01 BTC
                .buyPrice(BigDecimal.valueOf(40000))
                .build();

        when(assetRepository.findByUserIdAndCoinId(anyLong(), eq(1L))).thenReturn(Optional.of(existing));

        // Trying to sell $1000 worth = 0.02 BTC, but only 0.01 owned
        assertThatThrownBy(() ->
                orderService.processOrder(1L, "bitcoin", "SELL", BigDecimal.valueOf(1000))
        ).isInstanceOf(RuntimeException.class)
         .hasMessageContaining("Insufficient");

        verify(walletService, never()).creditForSell(any(), any());
    }

    @Test
    void sellOrder_shouldCalculateProfitCorrectly() {
        Asset existing = Asset.builder()
                .user(User.builder().id(1L).build())
                .coin(bitcoin)
                .quantity(BigDecimal.valueOf(0.05)) // owns 0.05 BTC, bought at $40k avg
                .buyPrice(BigDecimal.valueOf(40000))
                .build();

        when(assetRepository.findByUserIdAndCoinId(anyLong(), eq(1L))).thenReturn(Optional.of(existing));

        // Selling $1000 worth at current price $50000/BTC = 0.02 BTC sold
        Order result = orderService.processOrder(1L, "bitcoin", "SELL", BigDecimal.valueOf(1000));

        assertThat(result.getOrderType()).isEqualTo(OrderType.SELL);
        verify(walletService).creditForSell(wallet, BigDecimal.valueOf(1000));

        // Remaining holding should be reduced, not deleted, since 0.03 BTC remains
        verify(assetRepository).save(any(Asset.class));
        verify(assetRepository, never()).delete(any(Asset.class));
    }

    @Test
    void sellOrder_shouldDeleteAssetWhenFullyLiquidated() {
        Asset existing = Asset.builder()
                .user(User.builder().id(1L).build())
                .coin(bitcoin)
                .quantity(BigDecimal.valueOf(0.02)) // owns exactly 0.02 BTC
                .buyPrice(BigDecimal.valueOf(40000))
                .build();

        when(assetRepository.findByUserIdAndCoinId(anyLong(), eq(1L))).thenReturn(Optional.of(existing));

        // Selling $1000 worth = 0.02 BTC = exactly the full holding
        orderService.processOrder(1L, "bitcoin", "SELL", BigDecimal.valueOf(1000));

        verify(assetRepository).delete(existing);
        verify(assetRepository, never()).save(any(Asset.class));
    }
}