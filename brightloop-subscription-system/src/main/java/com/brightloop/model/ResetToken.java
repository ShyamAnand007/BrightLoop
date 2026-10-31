package com.brightloop.model;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.*;
@Entity @Table(name="reset_tokens") public class ResetToken { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false,unique=true) public String token; @Column(nullable=false) public String email; public LocalDateTime expiresAt; }
