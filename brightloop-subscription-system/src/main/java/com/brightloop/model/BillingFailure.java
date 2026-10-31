package com.brightloop.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity public class BillingFailure { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public Long subscriptionId; public LocalDate billingDate; @Column(length=500) public String message; public LocalDateTime createdAt=LocalDateTime.now(); }
