package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.PaymentOrderRepository;
import com.stripe.model.PaymentIntent;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Autowired private RazorpayService razorpayService;
    @Autowired private StripeService stripeService;
    @Autowired private PaymentOrderRepository paymentOrderRepository;
    @Autowired private WalletService walletService;

    public JSONObject createRazorpayOrder(User user, BigDecimal amount) throws Exception {
        JSONObject order = razorpayService.createOrder(amount);

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .amount(amount)
                .status(PaymentOrderStatus.CREATED)
                .paymentMethod(PaymentMethod.RAZORPAY)
                .gatewayOrderId(order.getString("id"))
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        paymentOrderRepository.save(paymentOrder);

        return order;
    }

    @Transactional
    public String verifyRazorpayPayment(User user, String orderId, String paymentId, String signature) {
        boolean valid = razorpayService.verifySignature(orderId, paymentId, signature);

        PaymentOrder paymentOrder = paymentOrderRepository.findAll().stream()
                .filter(po -> po.getGatewayOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment order not found"));

        if (!valid) {
            paymentOrder.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(paymentOrder);
            throw new RuntimeException("Payment signature verification failed");
        }

        paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
        paymentOrder.setGatewayPaymentId(paymentId);
        paymentOrderRepository.save(paymentOrder);

        // Credit the wallet only after verified success
        walletService.addBalance(user.getId(), paymentOrder.getAmount());

        return "Payment verified and wallet credited";
    }

    public PaymentIntent createStripePaymentIntent(User user, BigDecimal amount) throws Exception {
        PaymentIntent intent = stripeService.createPaymentIntent(amount);

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .amount(amount)
                .status(PaymentOrderStatus.CREATED)
                .paymentMethod(PaymentMethod.STRIPE)
                .gatewayOrderId(intent.getId())
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();
        paymentOrderRepository.save(paymentOrder);

        return intent;
    }

    @Transactional
    public String confirmStripePayment(User user, String paymentIntentId) throws Exception {
        PaymentIntent intent = stripeService.retrievePaymentIntent(paymentIntentId);

        PaymentOrder paymentOrder = paymentOrderRepository.findAll().stream()
                .filter(po -> po.getGatewayOrderId().equals(paymentIntentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment order not found"));

        if (!"succeeded".equals(intent.getStatus())) {
            paymentOrder.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(paymentOrder);
            throw new RuntimeException("Payment not completed. Status: " + intent.getStatus());
        }

        paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
        paymentOrder.setGatewayPaymentId(intent.getId());
        paymentOrderRepository.save(paymentOrder);

        walletService.addBalance(user.getId(), paymentOrder.getAmount());

        return "Payment confirmed and wallet credited";
    }
}