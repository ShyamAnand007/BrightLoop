package com.brightloop.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.LocalDate;
@Entity public class Payment { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(optional=false) public Invoice invoice; public BigDecimal amount=BigDecimal.ZERO; public LocalDate paymentDate; public String method; public String note; }
