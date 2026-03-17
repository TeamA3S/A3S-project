package com.example.a3sproject.domain.product.repository;

import com.example.a3sproject.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
