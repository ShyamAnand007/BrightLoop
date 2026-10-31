package com.brightloop.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity public class QuotationTemplate { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false) public String name; @ManyToOne(optional=false) public Plan plan; public int quantity=1; public int validDays=30; @ManyToOne public TaxRule taxRule; }
