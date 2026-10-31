package com.brightloop.repository;
import com.brightloop.model.Product;import org.springframework.data.jpa.repository.JpaRepository;
public interface ProductRepository extends JpaRepository<Product,Long>{}
