package com.capstone.billing.repository;

import com.capstone.billing.domain.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    List<PaymentHistory> findByPolicyIdOrderByDueDateDesc(Long policyId);

    long countByPolicyIdAndMissedTrue(Long policyId);
}
