package com.brightloop.model;
import jakarta.persistence.*;import java.math.BigDecimal;
@Entity public class ProductVariant { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @ManyToOne(optional=false) public Product product; @Column(nullable=false) public String attributeName; @Column(nullable=false) public String attributeValue; @Column(nullable=false) public BigDecimal priceAdjustment=BigDecimal.ZERO; }
