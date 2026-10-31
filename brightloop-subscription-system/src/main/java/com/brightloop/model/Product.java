package com.brightloop.model;
import jakarta.persistence.*;import java.math.BigDecimal;
@Entity public class Product { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false) public String name; public String type; @Column(nullable=false) public BigDecimal salesPrice=BigDecimal.ZERO; public BigDecimal costPrice=BigDecimal.ZERO; @Column(length=2000) public String notes; public boolean active=true; }
