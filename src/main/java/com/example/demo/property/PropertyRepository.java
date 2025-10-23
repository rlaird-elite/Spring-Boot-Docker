package com.example.demo.property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {
    // List<Property> findByAddress(String address);
}