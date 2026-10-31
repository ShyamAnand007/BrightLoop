package com.brightloop.model;
import jakarta.persistence.*;import java.time.LocalDateTime;
@Entity @Table(name="app_users") public class AppUser {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false,unique=true) public String email;
 @Column(nullable=false) public String passwordHash;
 @Column(nullable=false) public String fullName;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role=Role.CUSTOMER;
 public LocalDateTime createdAt=LocalDateTime.now();
 @Column(nullable=false,columnDefinition="boolean default true") public Boolean active=true;
 public enum Role{ADMIN,SALES,CUSTOMER}
}
