package com.cryptox.backend.repository;

import com.cryptox.backend.entity.VerificationCode;
import com.cryptox.backend.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByUserIdAndVerificationTypeAndIsUsedFalseOrderByIdDesc(
            Long userId, VerificationType verificationType);
}