package com.brightloop.model;
import jakarta.persistence.*;
@Entity public class Customer { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; @Column(nullable=false) public String name; @Column(nullable=false,unique=true) public String email; public String company; public String phone; public String billingAddress; }
