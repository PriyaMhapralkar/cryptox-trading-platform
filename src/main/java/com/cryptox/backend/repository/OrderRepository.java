package com.cryptox.backend.repository;

import com.cryptox.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByTimestampDesc(Long userId);
}