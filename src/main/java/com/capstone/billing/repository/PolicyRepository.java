package com.capstone.billing.repository;

import com.capstone.billing.domain.Policy;
import com.capstone.billing.domain.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(int riskScore);

    long countByStatus(PolicyStatus status);

    long countByRiskScoreGreaterThanEqual(int riskScore);

    @Query("select coalesce(sum(p.premium), 0) from Policy p where p.dueDate = current_date")
    java.math.BigDecimal sumPremiumDueToday();

    @Query("select coalesce(sum(p.premium), 0) from Policy p where p.riskScore >= 70")
    java.math.BigDecimal sumPremiumAtRisk();

    @Query("""
            select distinct p from Policy p
            join fetch p.customer
            where p.riskScore >= :minRisk
            order by p.riskScore desc
            """)
    List<Policy> findAtRiskWithCustomer(@Param("minRisk") int minRisk);

    @Query("""
            select p from Policy p
            join fetch p.customer
            where p.id = :id
            """)
    Optional<Policy> findDetailById(@Param("id") Long id);

    @Query("""
            select c.region, count(p)
            from Policy p join p.customer c
            where p.riskScore >= 70
            group by c.region
            order by count(p) desc
            """)
    List<Object[]> countAtRiskByRegion();
}
