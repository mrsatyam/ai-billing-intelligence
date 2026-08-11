package com.capstone.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    private int age;

    @Column(length = 80)
    private String occupation;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_segment", length = 20)
    private IncomeSegment incomeSegment;

    @Column(length = 80)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_payment_method", length = 20)
    private PaymentMethod preferredPaymentMethod;

    @Column(name = "salary_credit_day")
    private int salaryCreditDay;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public IncomeSegment getIncomeSegment() {
        return incomeSegment;
    }

    public void setIncomeSegment(IncomeSegment incomeSegment) {
        this.incomeSegment = incomeSegment;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public PaymentMethod getPreferredPaymentMethod() {
        return preferredPaymentMethod;
    }

    public void setPreferredPaymentMethod(PaymentMethod preferredPaymentMethod) {
        this.preferredPaymentMethod = preferredPaymentMethod;
    }

    public int getSalaryCreditDay() {
        return salaryCreditDay;
    }

    public void setSalaryCreditDay(int salaryCreditDay) {
        this.salaryCreditDay = salaryCreditDay;
    }
}
