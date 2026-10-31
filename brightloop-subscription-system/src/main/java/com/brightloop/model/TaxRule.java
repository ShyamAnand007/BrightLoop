package com.brightloop.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity public class TaxRule { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false) public String name; @Column(nullable=false) public BigDecimal ratePercent=BigDecimal.ZERO; public boolean active=true; }
